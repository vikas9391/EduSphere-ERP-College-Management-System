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
import com.collegeerp.Backend.subject.entity.Subject;
import com.collegeerp.Backend.subject.repository.SubjectRepository;
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
    private final SubjectRepository subjectRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final UserRepository userRepository;

    public ExamScheduleService(ExamScheduleRepository examScheduleRepository,
                               ExamRepository examRepository,
                               SubjectRepository subjectRepository,
                               ClassSubjectRepository classSubjectRepository,
                               UserRepository userRepository) {
        this.examScheduleRepository = examScheduleRepository;
        this.examRepository = examRepository;
        this.subjectRepository = subjectRepository;
        this.classSubjectRepository = classSubjectRepository;
        this.userRepository = userRepository;
    }

    public ExamScheduleResponse createSchedule(ExamScheduleRequest request) {
        Exam exam = examRepository.findById(request.getExamId())
                .orElseThrow(() -> new RuntimeException("Exam not found"));
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new RuntimeException("Subject not found"));
        ClassSubject classSubject = resolveClassSubject(request, subject);

        validateSchedule(request, exam, subject);
        validateUniqueSchedule(exam.getId(), subject.getId(), classSubject, null);

        User invigilator = findInvigilator(request.getInvigilatorId());

        ExamSchedule schedule = ExamSchedule.builder()
                .exam(exam)
                .subject(subject)
                .classSubject(classSubject)
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

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new RuntimeException("Subject not found"));
        ClassSubject classSubject = resolveClassSubject(request, subject);

        validateSchedule(request, schedule.getExam(), subject);
        validateUniqueSchedule(schedule.getExam().getId(), subject.getId(), classSubject, schedule);

        schedule.setSubject(subject);
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
        if (principal == null) {
            return false;
        }
        if (isAdmin(principal)) {
            return true;
        }
        if (!"TEACHER".equalsIgnoreCase(principal.getRole())) {
            return false;
        }
        ClassSubject classSubject = schedule.getClassSubject();
        return classSubject != null
                && classSubject.getTeacher() != null
                && Objects.equals(classSubject.getTeacher().getId(), principal.getId());
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

    private ClassSubject resolveClassSubject(ExamScheduleRequest request, Subject subject) {
        if (request.getClassSubjectId() != null) {
            ClassSubject classSubject = classSubjectRepository.findByIdWithRelations(request.getClassSubjectId())
                    .orElseThrow(() -> new RuntimeException("Class subject not found"));
            if (classSubject.getSubject() == null
                    || !Objects.equals(classSubject.getSubject().getId(), subject.getId())) {
                throw new IllegalArgumentException("Class subject does not belong to the selected formal subject");
            }
            return classSubject;
        }

        throw new IllegalArgumentException(
                "Class subject is required; select the exact class subject for this exam schedule");
    }

    private void validateUniqueSchedule(Long examId, Long subjectId,
                                        ClassSubject classSubject, ExamSchedule current) {
        if (classSubject != null) {
            boolean sameCurrent = current != null
                    && current.getClassSubject() != null
                    && Objects.equals(current.getClassSubject().getId(), classSubject.getId());
            if (!sameCurrent && examScheduleRepository.existsByExamIdAndClassSubjectId(examId, classSubject.getId())) {
                throw new RuntimeException("This class subject is already scheduled for this exam");
            }
            return;
        }

        boolean sameLegacyCurrent = current != null
                && current.getClassSubject() == null
                && Objects.equals(current.getSubject().getId(), subjectId);
        if (!sameLegacyCurrent
                && examScheduleRepository.existsByExamIdAndSubjectIdAndClassSubjectIsNull(examId, subjectId)) {
            throw new RuntimeException("This subject is already scheduled for this exam");
        }
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
        ClassSubject classSubject = s.getClassSubject();
        return ExamScheduleResponse.builder()
                .id(s.getId())
                .examId(s.getExam().getId())
                .examName(s.getExam().getExamName())
                .subjectId(s.getSubject().getId())
                .subjectName(s.getSubject().getSubjectName())
                .classSubjectId(classSubject != null ? classSubject.getId() : null)
                .classId(classSubject != null && classSubject.getSchoolClass() != null
                        ? classSubject.getSchoolClass().getId() : null)
                .className(classSubject != null && classSubject.getSchoolClass() != null
                        ? classSubject.getSchoolClass().getName() : null)
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
