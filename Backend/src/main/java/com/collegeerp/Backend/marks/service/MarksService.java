package com.collegeerp.Backend.marks.service;

import com.collegeerp.Backend.common.exception.BadRequestException;
import com.collegeerp.Backend.common.exception.DuplicateResourceException;
import com.collegeerp.Backend.common.exception.ResourceNotFoundException;
import com.collegeerp.Backend.examination.entity.ExamSchedule;
import com.collegeerp.Backend.examination.repository.ExamScheduleRepository;
import com.collegeerp.Backend.marks.dto.EligibleStudentResponse;
import com.collegeerp.Backend.marks.dto.MarksRequest;
import com.collegeerp.Backend.marks.dto.MarksResponse;
import com.collegeerp.Backend.marks.entity.Marks;
import com.collegeerp.Backend.marks.repository.MarksRepository;
import com.collegeerp.Backend.schoolclass.entity.ClassEnrollment;
import com.collegeerp.Backend.schoolclass.entity.ClassSubject;
import com.collegeerp.Backend.schoolclass.repository.ClassEnrollmentRepository;
import com.collegeerp.Backend.security.UserPrincipal;
import com.collegeerp.Backend.student.entity.Student;
import com.collegeerp.Backend.student.repository.StudentRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class MarksService {

    private static final String SOURCE_CLASS_ROSTER = "CLASS_ROSTER";

    private final MarksRepository marksRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final StudentRepository studentRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;

    public MarksService(MarksRepository marksRepository,
                        ExamScheduleRepository examScheduleRepository,
                        StudentRepository studentRepository,
                        ClassEnrollmentRepository classEnrollmentRepository) {
        this.marksRepository = marksRepository;
        this.examScheduleRepository = examScheduleRepository;
        this.studentRepository = studentRepository;
        this.classEnrollmentRepository = classEnrollmentRepository;
    }

    public MarksResponse enterMarks(MarksRequest request) {
        ExamSchedule examSchedule = findExamSchedule(request.getExamScheduleId());
        requireCanManageSubject(examSchedule);

        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> ResourceNotFoundException.of("Student", request.getStudentId()));

        if (marksRepository.existsByExamScheduleIdAndStudentId(examSchedule.getId(), student.getId())) {
            throw new DuplicateResourceException("Marks already entered for this student in this exam schedule");
        }

        ClassEnrollment classEnrollment = requireEligibleParticipation(examSchedule, student.getId());
        validateMarks(request, examSchedule);

        int total = request.getInternalMarks() + request.getExternalMarks();
        double percentage = (total * 100.0) / examSchedule.getMaxMarks();
        String grade = GradeUtil.gradeFor(percentage);

        Marks marks = Marks.builder()
                .examSchedule(examSchedule)
                .student(student)
                .classEnrollment(classEnrollment)
                .internalMarks(request.getInternalMarks())
                .externalMarks(request.getExternalMarks())
                .totalMarks(total)
                .grade(grade)
                .gradePoint(GradeUtil.gradePointFor(grade))
                .status("DRAFT")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return map(marksRepository.save(marks));
    }

    public MarksResponse updateMarks(Long id, MarksRequest request) {
        Marks marks = findMarks(id);
        requireCanManageSubject(marks.getExamSchedule());

        if ("PUBLISHED".equals(marks.getStatus())) {
            throw new BadRequestException("Published marks cannot be edited");
        }

        validateMarks(request, marks.getExamSchedule());

        int total = request.getInternalMarks() + request.getExternalMarks();
        double percentage = (total * 100.0) / marks.getExamSchedule().getMaxMarks();
        String grade = GradeUtil.gradeFor(percentage);

        marks.setInternalMarks(request.getInternalMarks());
        marks.setExternalMarks(request.getExternalMarks());
        marks.setTotalMarks(total);
        marks.setGrade(grade);
        marks.setGradePoint(GradeUtil.gradePointFor(grade));
        marks.setUpdatedAt(LocalDateTime.now());

        return map(marksRepository.save(marks));
    }

    @Transactional(readOnly = true)
    public List<MarksResponse> getMarksByExamSchedule(Long examScheduleId) {
        ExamSchedule schedule = findExamSchedule(examScheduleId);
        requireCanManageSubject(schedule);
        return marksRepository.findByExamScheduleIdWithDetails(examScheduleId)
                .stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public MarksResponse getMarks(Long id) {
        Marks marks = findMarks(id);
        requireCanManageSubject(marks.getExamSchedule());
        return map(marks);
    }

    public MarksResponse publishMarks(Long id) {
        Marks marks = findMarks(id);
        requireCanManageSubject(marks.getExamSchedule());
        marks.setStatus("PUBLISHED");
        marks.setUpdatedAt(LocalDateTime.now());
        return map(marksRepository.save(marks));
    }

    public List<MarksResponse> publishMarksForExamSchedule(Long examScheduleId) {
        ExamSchedule schedule = findExamSchedule(examScheduleId);
        requireCanManageSubject(schedule);
        List<Marks> marksList = marksRepository.findByExamScheduleIdWithDetails(examScheduleId);
        if (marksList.isEmpty()) {
            throw new ResourceNotFoundException("No marks found for this exam schedule");
        }

        marksList.forEach(m -> {
            m.setStatus("PUBLISHED");
            m.setUpdatedAt(LocalDateTime.now());
        });

        return marksRepository.saveAll(marksList).stream().map(this::map).toList();
    }

    public void deleteMarks(Long id) {
        Marks marks = findMarks(id);
        requireCanManageSubject(marks.getExamSchedule());
        if ("PUBLISHED".equals(marks.getStatus())) {
            throw new BadRequestException("Published marks cannot be deleted");
        }
        marksRepository.delete(marks);
    }

    @Transactional(readOnly = true)
    public List<EligibleStudentResponse> getEligibleStudents(Long examScheduleId) {
        ExamSchedule examSchedule = findExamSchedule(examScheduleId);
        requireCanManageSubject(examSchedule);

        ClassSubject classSubject = requireClassSubject(examSchedule);
        return classEnrollmentRepository.findAllByClassSubjectId(classSubject.getId()).stream()
                .map(ClassEnrollment::getStudent)
                .distinct()
                .map(s -> eligibleStudent(examScheduleId, s, SOURCE_CLASS_ROSTER))
                .toList();
    }

    private EligibleStudentResponse eligibleStudent(Long examScheduleId, Student student, String source) {
        return EligibleStudentResponse.builder()
                .studentId(student.getId())
                .studentName(student.getFirstName() + " " +
                        (student.getLastName() != null ? student.getLastName() : ""))
                .source(source)
                .alreadyGraded(marksRepository.existsByExamScheduleIdAndStudentId(examScheduleId, student.getId()))
                .build();
    }

    private void requireCanManageSubject(ExamSchedule examSchedule) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication is required");
        }

        Object principalObject = authentication.getPrincipal();
        if (!(principalObject instanceof UserPrincipal principal)) {
            throw new AccessDeniedException("Unable to determine the authenticated user");
        }

        String role = principal.getRole();
        if ("ADMIN".equalsIgnoreCase(role) || "SUPER_ADMIN".equalsIgnoreCase(role)) {
            return;
        }

        if (!"TEACHER".equalsIgnoreCase(role)) {
            throw new AccessDeniedException("Only the assigned teacher or a college admin can manage marks");
        }

        ClassSubject classSubject = requireClassSubject(examSchedule);
        Long assignedTeacherId = classSubject.getTeacher() != null
                ? classSubject.getTeacher().getId()
                : null;

        if (assignedTeacherId == null || !principal.getId().equals(assignedTeacherId)) {
            throw new AccessDeniedException("Teachers can only manage marks for their assigned class subject");
        }
    }

    private ClassEnrollment requireEligibleParticipation(ExamSchedule examSchedule, Long studentId) {
        ClassSubject classSubject = requireClassSubject(examSchedule);
        return classEnrollmentRepository
                .findByClassSubjectIdAndStudentId(classSubject.getId(), studentId)
                .orElseThrow(() -> new BadRequestException(
                        "Student is not enrolled in the class subject for this exam schedule"));
    }

    private ClassSubject requireClassSubject(ExamSchedule examSchedule) {
        if (examSchedule.getClassSubject() == null) {
            throw new BadRequestException(
                    "This exam schedule is not class-scoped; migrate the schedule before managing marks");
        }
        return examSchedule.getClassSubject();
    }

    private void validateMarks(MarksRequest request, ExamSchedule examSchedule) {
        if (request.getInternalMarks() == null || request.getExternalMarks() == null) {
            throw new BadRequestException("Internal and external marks are required");
        }
        if (request.getInternalMarks() < 0 || request.getExternalMarks() < 0) {
            throw new BadRequestException("Marks cannot be negative");
        }
        if (examSchedule.getMaxMarks() == null || examSchedule.getMaxMarks() <= 0) {
            throw new BadRequestException("Exam schedule maximum marks must be greater than zero");
        }

        int total = request.getInternalMarks() + request.getExternalMarks();
        if (total > examSchedule.getMaxMarks()) {
            throw new BadRequestException("Total marks cannot exceed the maximum marks for this exam");
        }
    }

    private ExamSchedule findExamSchedule(Long id) {
        return examScheduleRepository.findByIdWithDetails(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Exam schedule", id));
    }

    private Marks findMarks(Long id) {
        return marksRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Marks", id));
    }

    private MarksResponse map(Marks m) {
        return MarksResponse.builder()
                .id(m.getId())
                .examScheduleId(m.getExamSchedule().getId())
                .examId(m.getExamSchedule().getExam().getId())
                .examName(m.getExamSchedule().getExam().getExamName())
                .subjectId(m.getExamSchedule().getSubject().getId())
                .subjectName(m.getExamSchedule().getSubject().getSubjectName())
                .studentId(m.getStudent().getId())
                .studentName(m.getStudent().getFirstName() + " " + m.getStudent().getLastName())
                .internalMarks(m.getInternalMarks())
                .externalMarks(m.getExternalMarks())
                .totalMarks(m.getTotalMarks())
                .maxMarks(m.getExamSchedule().getMaxMarks())
                .grade(m.getGrade())
                .gradePoint(m.getGradePoint())
                .status(m.getStatus())
                .build();
    }
}
