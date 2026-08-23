package com.collegeerp.Backend.examination.service;

import com.collegeerp.Backend.examination.dto.ExamScheduleRequest;
import com.collegeerp.Backend.examination.dto.ExamScheduleResponse;
import com.collegeerp.Backend.examination.entity.Exam;
import com.collegeerp.Backend.examination.entity.ExamSchedule;
import com.collegeerp.Backend.examination.repository.ExamRepository;
import com.collegeerp.Backend.examination.repository.ExamScheduleRepository;
import com.collegeerp.Backend.subject.entity.Subject;
import com.collegeerp.Backend.subject.repository.SubjectRepository;
import com.collegeerp.Backend.common.User;
import com.collegeerp.Backend.common.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class ExamScheduleService {

    private final ExamScheduleRepository examScheduleRepository;
    private final ExamRepository examRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;

    public ExamScheduleService(ExamScheduleRepository examScheduleRepository,
                                ExamRepository examRepository,
                                SubjectRepository subjectRepository,
                                UserRepository userRepository) {
        this.examScheduleRepository = examScheduleRepository;
        this.examRepository = examRepository;
        this.subjectRepository = subjectRepository;
        this.userRepository = userRepository;
    }

    public ExamScheduleResponse createSchedule(ExamScheduleRequest request) {
        Exam exam = examRepository.findById(request.getExamId())
                .orElseThrow(() -> new RuntimeException("Exam not found"));
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new RuntimeException("Subject not found"));

        validateSchedule(request, exam, subject);

        if (examScheduleRepository.existsByExamIdAndSubjectId(exam.getId(), subject.getId())) {
            throw new RuntimeException("This subject is already scheduled for this exam");
        }

        User invigilator = findInvigilator(request.getInvigilatorId());

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

        return map(examScheduleRepository.save(schedule));
    }

    public List<ExamScheduleResponse> getScheduleByExam(Long examId) {
        return examScheduleRepository.findByExamIdWithDetails(examId).stream()
                .map(this::map).toList();
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

        validateSchedule(request, schedule.getExam(), subject);

        boolean subjectChanged = !Objects.equals(schedule.getSubject().getId(), subject.getId());
        if (subjectChanged && examScheduleRepository.existsByExamIdAndSubjectId(
                schedule.getExam().getId(), subject.getId())) {
            throw new RuntimeException("This subject is already scheduled for this exam");
        }

        schedule.setSubject(subject);
        schedule.setInvigilator(findInvigilator(request.getInvigilatorId()));
        schedule.setExamDate(request.getExamDate());
        schedule.setStartTime(request.getStartTime());
        schedule.setEndTime(request.getEndTime());
        schedule.setRoom(request.getRoom());
        schedule.setMaxMarks(request.getMaxMarks());

        return map(examScheduleRepository.save(schedule));
    }

    public void deleteSchedule(Long id) {
        ExamSchedule schedule = examScheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Exam schedule not found"));
        examScheduleRepository.delete(schedule);
    }

    private void validateSchedule(ExamScheduleRequest request, Exam exam, Subject subject) {
        if (request.getExamDate() == null || request.getStartTime() == null || request.getEndTime() == null) {
            throw new IllegalArgumentException("Exam date, start time and end time are required");
        }
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        if (request.getMaxMarks() == null || request.getMaxMarks() <= 0) {
            throw new IllegalArgumentException("Maximum marks must be greater than zero");
        }
        if (subject.getCourse() == null || exam.getCourse() == null
                || !Objects.equals(subject.getCourse().getId(), exam.getCourse().getId())) {
            throw new IllegalArgumentException("The scheduled subject must belong to the exam's course");
        }

        // Exam startDate/endDate are already LocalDate values, so no conversion is needed.
        LocalDate startDate = exam.getStartDate();
        LocalDate endDate = exam.getEndDate();
        LocalDate examDate = request.getExamDate();

        if ((startDate != null && examDate.isBefore(startDate))
                || (endDate != null && examDate.isAfter(endDate))) {
            throw new IllegalArgumentException("Exam schedule date must fall within the exam date range");
        }
    }

    private User findInvigilator(Long id) {
        if (id == null) {
            return null;
        }
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invigilator not found"));
    }

    private ExamScheduleResponse map(ExamSchedule s) {
        return ExamScheduleResponse.builder()
                .id(s.getId())
                .examId(s.getExam().getId())
                .examName(s.getExam().getExamName())
                .subjectId(s.getSubject().getId())
                .subjectName(s.getSubject().getSubjectName())
                .invigilatorId(s.getInvigilator() != null ? s.getInvigilator().getId() : null)
                .invigilatorName(s.getInvigilator() != null
                        ? s.getInvigilator().getFirstName() + " " + s.getInvigilator().getLastName() : null)
                .examDate(s.getExamDate())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .room(s.getRoom())
                .maxMarks(s.getMaxMarks())
                .build();
    }
}
