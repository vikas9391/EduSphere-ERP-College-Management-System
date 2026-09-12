package com.collegeerp.Backend.examination.service;

import com.collegeerp.Backend.common.User;
import com.collegeerp.Backend.common.UserRepository;
import com.collegeerp.Backend.examination.dto.ExamScheduleRequest;
import com.collegeerp.Backend.examination.dto.ExamScheduleResponse;
import com.collegeerp.Backend.examination.entity.Exam;
import com.collegeerp.Backend.examination.entity.ExamSchedule;
import com.collegeerp.Backend.examination.repository.ExamRepository;
import com.collegeerp.Backend.examination.repository.ExamScheduleRepository;
import com.collegeerp.Backend.schoolclass.entity.ClassSubject;
import com.collegeerp.Backend.schoolclass.repository.ClassSubjectRepository;
import com.collegeerp.Backend.security.UserPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class ExamScheduleService {

    private final ExamScheduleRepository examScheduleRepository;
    private final ExamRepository examRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final UserRepository userRepository;

    public ExamScheduleService(ExamScheduleRepository examScheduleRepository,
                               ExamRepository examRepository,
                               ClassSubjectRepository classSubjectRepository,
                               UserRepository userRepository) {
        this.examScheduleRepository = examScheduleRepository;
        this.examRepository = examRepository;
        this.classSubjectRepository = classSubjectRepository;
        this.userRepository = userRepository;
    }

    public ExamScheduleResponse createSchedule(ExamScheduleRequest request) {
        Exam exam = findExam(request.getExamId());
        ClassSubject classSubject = findClassSubject(request.getClassSubjectId());
        validateSchedule(request, exam, classSubject);
        validateUniqueSchedule(exam.getId(), classSubject.getId(), null);

        ExamSchedule schedule = ExamSchedule.builder()
                .exam(exam)
                .classSubject(classSubject)
                .invigilator(findInvigilator(request.getInvigilatorId()))
                .examDate(request.getExamDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .room(request.getRoom())
                .maxMarks(request.getMaxMarks())
                .createdAt(LocalDateTime.now())
                .build();

        return map(examScheduleRepository.save(schedule));
    }

    public List<ExamScheduleResponse> getScheduleByExam(Long examId, UserPrincipal principal) {
        return examScheduleRepository.findByExamIdWithDetails(examId).stream()
                .filter(schedule -> canView(schedule, principal))
                .map(this::map)
                .toList();
    }

    public ExamScheduleResponse getSchedule(Long id, UserPrincipal principal) {
        ExamSchedule schedule = examScheduleRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Exam schedule not found"));
        requireCanView(schedule, principal);
        return map(schedule);
    }

    public ExamScheduleResponse updateSchedule(Long id, ExamScheduleRequest request) {
        ExamSchedule schedule = examScheduleRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Exam schedule not found"));
        ClassSubject classSubject = findClassSubject(request.getClassSubjectId());
        validateSchedule(request, schedule.getExam(), classSubject);
        validateUniqueSchedule(schedule.getExam().getId(), classSubject.getId(), schedule);

        schedule.setClassSubject(classSubject);
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

    private boolean canView(ExamSchedule schedule, UserPrincipal principal) {
        if (principal == null) return false;
        if (isAdmin(principal)) return true;
        if (!"TEACHER".equalsIgnoreCase(principal.getRole())) return false;
        ClassSubject cs = schedule.getClassSubject();
        return cs != null && cs.getTeacher() != null
                && Objects.equals(cs.getTeacher().getId(), principal.getId());
    }

    private void requireCanView(ExamSchedule schedule, UserPrincipal principal) {
        if (!canView(schedule, principal)) {
            throw new AccessDeniedException("You can only view exam schedules for your assigned class subjects");
        }
    }

    private boolean isAdmin(UserPrincipal principal) {
        return "ADMIN".equalsIgnoreCase(principal.getRole())
                || "SUPER_ADMIN".equalsIgnoreCase(principal.getRole());
    }

    private void validateUniqueSchedule(Long examId, Long classSubjectId, ExamSchedule current) {
        boolean sameCurrent = current != null && current.getClassSubject() != null
                && Objects.equals(current.getClassSubject().getId(), classSubjectId);
        if (!sameCurrent && examScheduleRepository.existsByExamIdAndClassSubjectId(examId, classSubjectId)) {
            throw new RuntimeException("This class subject is already scheduled for this exam");
        }
    }

    private void validateSchedule(ExamScheduleRequest request, Exam exam, ClassSubject classSubject) {
        if (request.getExamDate() == null || request.getStartTime() == null || request.getEndTime() == null) {
            throw new IllegalArgumentException("Exam date, start time and end time are required");
        }
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        if (request.getMaxMarks() == null || request.getMaxMarks() <= 0) {
            throw new IllegalArgumentException("Maximum marks must be greater than zero");
        }
        if (classSubject.getSubject() != null && exam.getCourse() != null
                && classSubject.getSubject().getCourse() != null
                && !Objects.equals(classSubject.getSubject().getCourse().getId(), exam.getCourse().getId())) {
            throw new IllegalArgumentException("The class subject's formal subject must belong to the exam's course");
        }
        LocalDate startDate = exam.getStartDate();
        LocalDate endDate = exam.getEndDate();
        if ((startDate != null && request.getExamDate().isBefore(startDate))
                || (endDate != null && request.getExamDate().isAfter(endDate))) {
            throw new IllegalArgumentException("Exam schedule date must fall within the exam date range");
        }
    }

    private Exam findExam(Long id) {
        return examRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Exam not found"));
    }

    private ClassSubject findClassSubject(Long id) {
        return classSubjectRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new RuntimeException("Class subject not found"));
    }

    private User findInvigilator(Long id) {
        if (id == null) return null;
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invigilator not found"));
    }

    private ExamScheduleResponse map(ExamSchedule s) {
        ClassSubject cs = s.getClassSubject();
        return ExamScheduleResponse.builder()
                .id(s.getId())
                .examId(s.getExam().getId())
                .examName(s.getExam().getExamName())
                .subjectId(cs.getSubject() != null ? cs.getSubject().getId() : null)
                .subjectName(cs.getSubjectName())
                .classSubjectId(cs.getId())
                .classId(cs.getSchoolClass().getId())
                .className(cs.getSchoolClass().getName())
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
