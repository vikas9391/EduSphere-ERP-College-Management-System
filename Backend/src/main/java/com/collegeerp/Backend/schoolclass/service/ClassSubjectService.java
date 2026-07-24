package com.collegeerp.Backend.schoolclass.service;

import com.collegeerp.Backend.common.exception.BadRequestException;
import com.collegeerp.Backend.common.exception.DuplicateResourceException;
import com.collegeerp.Backend.common.exception.ForbiddenException;
import com.collegeerp.Backend.common.exception.ResourceNotFoundException;
import com.collegeerp.Backend.schoolclass.dto.ClassEnrollmentResponse;
import com.collegeerp.Backend.schoolclass.dto.ClassSubjectRequest;
import com.collegeerp.Backend.schoolclass.dto.ClassSubjectResponse;
import com.collegeerp.Backend.schoolclass.entity.ClassEnrollment;
import com.collegeerp.Backend.schoolclass.entity.ClassStudent;
import com.collegeerp.Backend.schoolclass.entity.ClassSubject;
import com.collegeerp.Backend.schoolclass.entity.SchoolClass;
import com.collegeerp.Backend.schoolclass.repository.ClassEnrollmentRepository;
import com.collegeerp.Backend.schoolclass.repository.ClassSubjectRepository;
import com.collegeerp.Backend.student.entity.Student;
import com.collegeerp.Backend.student.repository.StudentRepository;
import com.collegeerp.Backend.teacher.entity.Teacher;
import com.collegeerp.Backend.teacher.repository.TeacherRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Subjects within a {@link SchoolClass}, and the enrollment connections they create.
 * Creating (or flipping to) a MANDATORY subject auto-enrolls the class's current roster;
 * the reverse direction (a new roster member picking up existing MANDATORY subjects)
 * lives in {@link SchoolClassService#addStudents}. ELECTIVE subjects are never
 * auto-enrolled - students opt in themselves via {@link #selfEnroll}.
 */
@Service
@Transactional
public class ClassSubjectService {

    private static final Logger log = LoggerFactory.getLogger(ClassSubjectService.class);
    private static final String STUDENT_ROLE = "STUDENT";

    private final ClassSubjectRepository classSubjectRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final SchoolClassService schoolClassService;

    public ClassSubjectService(ClassSubjectRepository classSubjectRepository,
                                ClassEnrollmentRepository classEnrollmentRepository,
                                TeacherRepository teacherRepository,
                                StudentRepository studentRepository,
                                SchoolClassService schoolClassService) {
        this.classSubjectRepository = classSubjectRepository;
        this.classEnrollmentRepository = classEnrollmentRepository;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.schoolClassService = schoolClassService;
    }

    /**
     * Creates one subject. For bulk "set up all of this semester's subjects at once",
     * the controller exposes a plural endpoint that just calls this per item, so a
     * partial failure (e.g. a duplicate code third in the list) fails clearly on that
     * one item rather than silently skipping it.
     */
    public ClassSubjectResponse createSubject(Long classId, Long principalId, String role, ClassSubjectRequest request) {
        SchoolClass schoolClass = schoolClassService.findClassOrThrow(classId);
        SchoolClassService.requireOwnerOrAdmin(schoolClass, principalId, role);

        if (classSubjectRepository.existsBySchoolClassIdAndSubjectCode(classId, request.getSubjectCode())) {
            throw new DuplicateResourceException(
                    "Subject code '" + request.getSubjectCode() + "' already exists in this class");
        }

        int currentCount = classSubjectRepository.countBySchoolClassId(classId);
        if (schoolClass.getMaxSubjects() != null && currentCount >= schoolClass.getMaxSubjects()) {
            throw new BadRequestException(
                    "This class is capped at " + schoolClass.getMaxSubjects() + " subject(s); remove one before adding another");
        }

        Teacher teacher = teacherRepository.findById(request.getTeacherId())
                .orElseThrow(() -> ResourceNotFoundException.of("Teacher", request.getTeacherId()));

        ClassSubject subject = ClassSubject.builder()
                .schoolClass(schoolClass)
                .subjectCode(request.getSubjectCode())
                .subjectName(request.getSubjectName())
                .credits(request.getCredits())
                .teacher(teacher)
                .enrollmentMode(request.getEnrollmentMode())
                .createdAt(LocalDateTime.now())
                .build();

        subject = classSubjectRepository.save(subject);
        log.info("Created class subject id={} code={} classId={} mode={}",
                subject.getId(), subject.getSubjectCode(), classId, subject.getEnrollmentMode());

        if (subject.getEnrollmentMode() == ClassSubject.EnrollmentMode.MANDATORY) {
            for (ClassStudent rosterEntry : schoolClassService.rosterOf(classId)) {
                schoolClassService.autoEnrollIfAbsent(subject, rosterEntry.getStudent());
            }
        }

        return map(subject);
    }

    @Transactional(readOnly = true)
    public List<ClassSubjectResponse> getSubjects(Long classId, Long principalId, String role) {
        SchoolClass schoolClass = schoolClassService.findClassOrThrow(classId);
        SchoolClassService.requireOwnerOrAdmin(schoolClass, principalId, role);
        return classSubjectRepository.findAllByClassId(classId).stream().map(this::map).toList();
    }

    /**
     * The student-facing counterpart to {@link #getSubjects}: every subject in a class
     * the student belongs to, each tagged with whether they're personally enrolled -
     * MANDATORY subjects will always show true (auto-enrolled on join), ELECTIVE ones
     * reflect whether the student has opted in via {@link #selfEnroll}.
     */
    @Transactional(readOnly = true)
    public List<ClassSubjectResponse> getSubjectsForStudent(Long classId, Long studentId, String role) {
        requireStudent(role);

        boolean onRoster = schoolClassService.rosterOf(classId).stream()
                .anyMatch(cs -> cs.getStudent().getId().equals(studentId));
        if (!onRoster) {
            throw new ForbiddenException("You are not a member of this class");
        }

        return classSubjectRepository.findAllByClassId(classId).stream()
                .map(s -> {
                    ClassSubjectResponse response = map(s);
                    response.setEnrolledByMe(
                            classEnrollmentRepository.existsByClassSubjectIdAndStudentId(s.getId(), studentId));
                    return response;
                })
                .toList();
    }

    public void deleteSubject(Long subjectId, Long principalId, String role) {
        ClassSubject subject = findSubjectOrThrow(subjectId);
        SchoolClassService.requireOwnerOrAdmin(subject.getSchoolClass(), principalId, role);
        // class_enrollments for this subject cascade-delete via the FK.
        classSubjectRepository.delete(subject);
        log.info("Deleted class subject id={}", subjectId);
    }

    /**
     * A student opts themself into an ELECTIVE subject. Requires the student to already
     * be on the class's roster - a class subject isn't open enrollment to the whole
     * college, only to the students the owning teacher has added.
     */
    public ClassEnrollmentResponse selfEnroll(Long subjectId, Long studentId, String role) {
        requireStudent(role);
        ClassSubject subject = findSubjectOrThrow(subjectId);

        if (subject.getEnrollmentMode() != ClassSubject.EnrollmentMode.ELECTIVE) {
            throw new BadRequestException("This subject is mandatory and already includes every class member");
        }

        boolean onRoster = schoolClassService.rosterOf(subject.getSchoolClass().getId()).stream()
                .anyMatch(cs -> cs.getStudent().getId().equals(studentId));
        if (!onRoster) {
            throw new ForbiddenException("You must be a member of this class to enroll in its subjects");
        }

        if (classEnrollmentRepository.existsByClassSubjectIdAndStudentId(subjectId, studentId)) {
            throw new DuplicateResourceException("You are already enrolled in this subject");
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Student", studentId));

        ClassEnrollment enrollment = classEnrollmentRepository.save(ClassEnrollment.builder()
                .classSubject(subject)
                .student(student)
                .source(ClassEnrollment.Source.SELF)
                .enrolledAt(LocalDateTime.now())
                .build());

        log.info("Student id={} self-enrolled in class subject id={}", studentId, subjectId);
        return mapEnrollment(enrollment);
    }

    /** A student drops an ELECTIVE subject they self-enrolled in. Cannot drop a MANDATORY one. */
    public void selfDrop(Long subjectId, Long studentId, String role) {
        requireStudent(role);
        ClassSubject subject = findSubjectOrThrow(subjectId);

        if (subject.getEnrollmentMode() != ClassSubject.EnrollmentMode.ELECTIVE) {
            throw new BadRequestException("This subject is mandatory and cannot be dropped");
        }

        ClassEnrollment enrollment = classEnrollmentRepository.findByClassSubjectIdAndStudentId(subjectId, studentId)
                .orElseThrow(() -> new BadRequestException("You are not enrolled in this subject"));

        classEnrollmentRepository.delete(enrollment);
        log.info("Student id={} dropped class subject id={}", studentId, subjectId);
    }

    @Transactional(readOnly = true)
    public List<ClassEnrollmentResponse> getEnrollments(Long subjectId, Long principalId, String role) {
        ClassSubject subject = findSubjectOrThrow(subjectId);
        SchoolClassService.requireOwnerOrAdmin(subject.getSchoolClass(), principalId, role);
        return classEnrollmentRepository.findAllByClassSubjectId(subjectId).stream()
                .map(this::mapEnrollment)
                .toList();
    }

    private ClassSubject findSubjectOrThrow(Long id) {
        return classSubjectRepository.findByIdWithRelations(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Class subject", id));
    }

    private static void requireStudent(String role) {
        if (!STUDENT_ROLE.equals(role)) {
            throw new ForbiddenException("Only students can self-enroll");
        }
    }

    private ClassSubjectResponse map(ClassSubject s) {
        return ClassSubjectResponse.builder()
                .id(s.getId())
                .schoolClassId(s.getSchoolClass() != null ? s.getSchoolClass().getId() : null)
                .subjectCode(s.getSubjectCode())
                .subjectName(s.getSubjectName())
                .credits(s.getCredits())
                .teacherId(s.getTeacher().getId())
                .teacherName(s.getTeacher().getFirstName() + " " + s.getTeacher().getLastName())
                .enrollmentMode(s.getEnrollmentMode())
                .enrolledCount(classEnrollmentRepository.findAllByClassSubjectId(s.getId()).size())
                .build();
    }

    private ClassEnrollmentResponse mapEnrollment(ClassEnrollment e) {
        return ClassEnrollmentResponse.builder()
                .id(e.getId())
                .classSubjectId(e.getClassSubject().getId())
                .subjectCode(e.getClassSubject().getSubjectCode())
                .subjectName(e.getClassSubject().getSubjectName())
                .studentId(e.getStudent().getId())
                .studentName(e.getStudent().getFirstName() + " " +
                        (e.getStudent().getLastName() != null ? e.getStudent().getLastName() : ""))
                .source(e.getSource())
                .enrolledAt(e.getEnrolledAt())
                .build();
    }
}
