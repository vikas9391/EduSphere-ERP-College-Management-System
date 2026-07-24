package com.collegeerp.Backend.examination.service;

import com.collegeerp.Backend.examination.dto.ExamScheduleRequest;
import com.collegeerp.Backend.examination.dto.ExamScheduleResponse;
import com.collegeerp.Backend.examination.entity.Exam;
import com.collegeerp.Backend.examination.entity.ExamSchedule;
import com.collegeerp.Backend.examination.repository.ExamRepository;
import com.collegeerp.Backend.examination.repository.ExamScheduleRepository;
import com.collegeerp.Backend.subject.entity.Subject;
import com.collegeerp.Backend.subject.repository.SubjectRepository;
import com.collegeerp.Backend.teacher.entity.Teacher;
import com.collegeerp.Backend.teacher.repository.TeacherRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ExamScheduleService {

    private final ExamScheduleRepository examScheduleRepository;
    private final ExamRepository examRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;

    public ExamScheduleService(ExamScheduleRepository examScheduleRepository,
                                ExamRepository examRepository,
                                SubjectRepository subjectRepository,
                                TeacherRepository teacherRepository) {
        this.examScheduleRepository = examScheduleRepository;
        this.examRepository = examRepository;
        this.subjectRepository = subjectRepository;
        this.teacherRepository = teacherRepository;
    }

    public ExamScheduleResponse createSchedule(ExamScheduleRequest request) {

        Exam exam = examRepository.findById(request.getExamId())
                .orElseThrow(() -> new RuntimeException("Exam not found"));

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new RuntimeException("Subject not found"));

        if (examScheduleRepository.existsByExamIdAndSubjectId(exam.getId(), subject.getId())) {
            throw new RuntimeException("This subject is already scheduled for this exam");
        }

        Teacher invigilator = null;
        if (request.getInvigilatorId() != null) {
            invigilator = teacherRepository.findById(request.getInvigilatorId())
                    .orElseThrow(() -> new RuntimeException("Invigilator not found"));
        }

        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new RuntimeException("End time must be after start time");
        }

        ExamSchedule schedule = ExamSchedule.builder()
                .exam(exam)
                .subject(subject)
                .invigilator(invigilator)
                .examDate(request.getExamDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .room(request.getRoom())
                .maxMarks(request.getMaxMarks())
                .createdAt(LocalDateTime.now())
                .build();

        schedule = examScheduleRepository.save(schedule);

        return map(schedule);
    }

    public List<ExamScheduleResponse> getScheduleByExam(Long examId) {

        return examScheduleRepository.findByExamIdWithDetails(examId)
                .stream()
                .map(this::map)
                .toList();
    }

    public ExamScheduleResponse getSchedule(Long id) {

        return map(examScheduleRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Exam schedule not found")));
    }

    public ExamScheduleResponse updateSchedule(Long id, ExamScheduleRequest request) {

        ExamSchedule schedule = examScheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Exam schedule not found"));

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new RuntimeException("Subject not found"));

        Teacher invigilator = null;
        if (request.getInvigilatorId() != null) {
            invigilator = teacherRepository.findById(request.getInvigilatorId())
                    .orElseThrow(() -> new RuntimeException("Invigilator not found"));
        }

        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new RuntimeException("End time must be after start time");
        }

        schedule.setSubject(subject);
        schedule.setInvigilator(invigilator);
        schedule.setExamDate(request.getExamDate());
        schedule.setStartTime(request.getStartTime());
        schedule.setEndTime(request.getEndTime());
        schedule.setRoom(request.getRoom());
        schedule.setMaxMarks(request.getMaxMarks());

        schedule = examScheduleRepository.save(schedule);

        return map(schedule);
    }

    public void deleteSchedule(Long id) {

        if (!examScheduleRepository.existsById(id)) {
            throw new RuntimeException("Exam schedule not found");
        }

        examScheduleRepository.deleteById(id);
    }

    private ExamScheduleResponse map(ExamSchedule s) {

        return ExamScheduleResponse.builder()
                .id(s.getId())
                .examId(s.getExam().getId())
                .examName(s.getExam().getExamName())
                .subjectId(s.getSubject().getId())
                .subjectName(s.getSubject().getSubjectName())
                .invigilatorId(s.getInvigilator() != null ? s.getInvigilator().getId() : null)
                .invigilatorName(s.getInvigilator() != null ? s.getInvigilator().getFirstName() + " " + s.getInvigilator().getLastName() : null)
                .examDate(s.getExamDate())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .room(s.getRoom())
                .maxMarks(s.getMaxMarks())
                .build();
    }
}
