package com.collegeerp.Backend.marks.service;

import com.collegeerp.Backend.enrollment.entity.Enrollment;
import com.collegeerp.Backend.enrollment.repository.EnrollmentRepository;
import com.collegeerp.Backend.examination.entity.ExamSchedule;
import com.collegeerp.Backend.examination.repository.ExamScheduleRepository;
import com.collegeerp.Backend.marks.dto.EligibleStudentResponse;
import com.collegeerp.Backend.marks.dto.MarksRequest;
import com.collegeerp.Backend.marks.dto.MarksResponse;
import com.collegeerp.Backend.marks.entity.Marks;
import com.collegeerp.Backend.marks.repository.MarksRepository;
import com.collegeerp.Backend.schoolclass.entity.ClassSubject;
import com.collegeerp.Backend.schoolclass.repository.ClassEnrollmentRepository;
import com.collegeerp.Backend.schoolclass.repository.ClassSubjectRepository;
import com.collegeerp.Backend.student.entity.Student;
import com.collegeerp.Backend.student.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MarksService {

    private static final String SOURCE_CLASS_ROSTER = "CLASS_ROSTER";
    private static final String SOURCE_FORMAL_ENROLLMENT = "FORMAL_ENROLLMENT";

    private final MarksRepository marksRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final StudentRepository studentRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final EnrollmentRepository enrollmentRepository;

    public MarksService(MarksRepository marksRepository,
                         ExamScheduleRepository examScheduleRepository,
                         StudentRepository studentRepository,
                         ClassSubjectRepository classSubjectRepository,
                         ClassEnrollmentRepository classEnrollmentRepository,
                         EnrollmentRepository enrollmentRepository) {
        this.marksRepository = marksRepository;
        this.examScheduleRepository = examScheduleRepository;
        this.studentRepository = studentRepository;
        this.classSubjectRepository = classSubjectRepository;
        this.classEnrollmentRepository = classEnrollmentRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    public MarksResponse enterMarks(MarksRequest request) {

        ExamSchedule examSchedule = examScheduleRepository.findById(request.getExamScheduleId())
                .orElseThrow(() -> new RuntimeException("Exam schedule not found"));

        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        if (marksRepository.existsByExamScheduleIdAndStudentId(examSchedule.getId(), student.getId())) {
            throw new RuntimeException("Marks already entered for this student in this exam schedule");
        }

        validateEligibility(examSchedule.getSubject().getId(), student.getId());
        validateMarks(request, examSchedule);

        int total = request.getInternalMarks() + request.getExternalMarks();
        double percentage = (total * 100.0) / examSchedule.getMaxMarks();
        String grade = GradeUtil.gradeFor(percentage);

        Marks marks = Marks.builder()
                .examSchedule(examSchedule)
                .student(student)
                .internalMarks(request.getInternalMarks())
                .externalMarks(request.getExternalMarks())
                .totalMarks(total)
                .grade(grade)
                .gradePoint(GradeUtil.gradePointFor(grade))
                .status("DRAFT")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        marks = marksRepository.save(marks);

        return map(marks);
    }

    public MarksResponse updateMarks(Long id, MarksRequest request) {

        Marks marks = marksRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Marks record not found"));

        if ("PUBLISHED".equals(marks.getStatus())) {
            throw new RuntimeException("Published marks cannot be edited");
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

        marks = marksRepository.save(marks);

        return map(marks);
    }

    public List<MarksResponse> getMarksByExamSchedule(Long examScheduleId) {

        return marksRepository.findByExamScheduleIdWithDetails(examScheduleId)
                .stream()
                .map(this::map)
                .toList();
    }

    public MarksResponse getMarks(Long id) {

        return map(marksRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Marks record not found")));
    }

    public MarksResponse publishMarks(Long id) {

        Marks marks = marksRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Marks record not found"));

        marks.setStatus("PUBLISHED");
        marks.setUpdatedAt(LocalDateTime.now());

        marks = marksRepository.save(marks);

        return map(marks);
    }

    public List<MarksResponse> publishMarksForExamSchedule(Long examScheduleId) {

        List<Marks> marksList = marksRepository.findByExamScheduleIdWithDetails(examScheduleId);

        if (marksList.isEmpty()) {
            throw new RuntimeException("No marks found for this exam schedule");
        }

        marksList.forEach(m -> {
            m.setStatus("PUBLISHED");
            m.setUpdatedAt(LocalDateTime.now());
        });

        return marksRepository.saveAll(marksList)
                .stream()
                .map(this::map)
                .toList();
    }

    public void deleteMarks(Long id) {

        Marks marks = marksRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Marks record not found"));

        if ("PUBLISHED".equals(marks.getStatus())) {
            throw new RuntimeException("Published marks cannot be deleted");
        }

        marksRepository.deleteById(id);
    }

    /**
     * Who can have marks entered against a given exam schedule, and where that
     * eligibility comes from.
     * <p>
     * If the exam schedule's Subject is linked to one or more {@link ClassSubject}s (see
     * {@code ClassSubject#subject}), eligibility is scoped to the union of those
     * classes' real rosters (CLASS_ROSTER) - a student must actually be enrolled in the
     * teacher's class, not just hold a loose formal Enrollment row. Otherwise it falls
     * back to the plain Enrollment table (FORMAL_ENROLLMENT), unchanged from before this
     * bridge existed.
     */
    public List<EligibleStudentResponse> getEligibleStudents(Long examScheduleId) {

        ExamSchedule examSchedule = examScheduleRepository.findById(examScheduleId)
                .orElseThrow(() -> new RuntimeException("Exam schedule not found"));

        Long subjectId = examSchedule.getSubject().getId();
        List<ClassSubject> linkedClassSubjects = classSubjectRepository.findBySubjectId(subjectId);

        List<Student> eligibleStudents;
        String source;

        if (!linkedClassSubjects.isEmpty()) {
            eligibleStudents = linkedClassSubjects.stream()
                    .flatMap(cs -> classEnrollmentRepository.findAllByClassSubjectId(cs.getId()).stream())
                    .map(ce -> ce.getStudent())
                    .distinct()
                    .toList();
            source = SOURCE_CLASS_ROSTER;
        } else {
            eligibleStudents = enrollmentRepository.findBySubjectIdWithStudent(subjectId).stream()
                    .map(Enrollment::getStudent)
                    .distinct()
                    .toList();
            source = SOURCE_FORMAL_ENROLLMENT;
        }

        return eligibleStudents.stream()
                .map(s -> EligibleStudentResponse.builder()
                        .studentId(s.getId())
                        .studentName(s.getFirstName() + " " + (s.getLastName() != null ? s.getLastName() : ""))
                        .source(source)
                        .alreadyGraded(marksRepository.existsByExamScheduleIdAndStudentId(examScheduleId, s.getId()))
                        .build())
                .toList();
    }

    /**
     * If the subject being examined is linked to one or more class-subjects, the
     * student must be on at least one of those classes' rosters - marks entry can no
     * longer bypass the class roster just because a stray Enrollment row exists.
     * Subjects with no class-subject link are unaffected (unchanged, pre-existing
     * behavior - any formally enrolled student is eligible).
     */
    private void validateEligibility(Long subjectId, Long studentId) {

        List<ClassSubject> linkedClassSubjects = classSubjectRepository.findBySubjectId(subjectId);
        if (linkedClassSubjects.isEmpty()) {
            return;
        }

        boolean onAnyRoster = linkedClassSubjects.stream()
                .anyMatch(cs -> classEnrollmentRepository.existsByClassSubjectIdAndStudentId(cs.getId(), studentId));

        if (!onAnyRoster) {
            throw new RuntimeException(
                    "This subject is linked to a class roster and the student is not enrolled in that class");
        }
    }

    private void validateMarks(MarksRequest request, ExamSchedule examSchedule) {

        if (request.getInternalMarks() < 0 || request.getExternalMarks() < 0) {
            throw new RuntimeException("Marks cannot be negative");
        }

        int total = request.getInternalMarks() + request.getExternalMarks();

        if (total > examSchedule.getMaxMarks()) {
            throw new RuntimeException("Total marks cannot exceed the maximum marks for this exam");
        }
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
