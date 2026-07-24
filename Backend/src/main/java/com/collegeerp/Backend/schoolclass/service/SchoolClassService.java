package com.collegeerp.Backend.schoolclass.service;

import com.collegeerp.Backend.common.exception.BadRequestException;
import com.collegeerp.Backend.common.exception.ForbiddenException;
import com.collegeerp.Backend.common.exception.ResourceNotFoundException;
import com.collegeerp.Backend.schoolclass.dto.AddStudentsRequest;
import com.collegeerp.Backend.schoolclass.dto.ClassStudentResponse;
import com.collegeerp.Backend.schoolclass.dto.SchoolClassRequest;
import com.collegeerp.Backend.schoolclass.dto.SchoolClassResponse;
import com.collegeerp.Backend.schoolclass.entity.ClassEnrollment;
import com.collegeerp.Backend.schoolclass.entity.ClassStudent;
import com.collegeerp.Backend.schoolclass.entity.ClassSubject;
import com.collegeerp.Backend.schoolclass.entity.SchoolClass;
import com.collegeerp.Backend.schoolclass.repository.ClassEnrollmentRepository;
import com.collegeerp.Backend.schoolclass.repository.ClassStudentRepository;
import com.collegeerp.Backend.schoolclass.repository.ClassSubjectRepository;
import com.collegeerp.Backend.schoolclass.repository.SchoolClassRepository;
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
 * Class creation/ownership and roster management. Auto-enrollment of new roster members
 * into a class's existing MANDATORY subjects happens here; the reverse direction (a new
 * MANDATORY subject enrolling the existing roster) lives in {@link ClassSubjectService}.
 */
@Service
@Transactional
public class SchoolClassService {

    private static final Logger log = LoggerFactory.getLogger(SchoolClassService.class);
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String TEACHER_ROLE = "TEACHER";
    private static final String STUDENT_ROLE = "STUDENT";

    private final SchoolClassRepository schoolClassRepository;
    private final ClassStudentRepository classStudentRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;

    public SchoolClassService(SchoolClassRepository schoolClassRepository,
                               ClassStudentRepository classStudentRepository,
                               ClassSubjectRepository classSubjectRepository,
                               ClassEnrollmentRepository classEnrollmentRepository,
                               TeacherRepository teacherRepository,
                               StudentRepository studentRepository) {
        this.schoolClassRepository = schoolClassRepository;
        this.classStudentRepository = classStudentRepository;
        this.classSubjectRepository = classSubjectRepository;
        this.classEnrollmentRepository = classEnrollmentRepository;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
    }

    public SchoolClassResponse createClass(Long teacherId, String role, SchoolClassRequest request) {
        requireTeacher(role);

        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> ResourceNotFoundException.of("Teacher", teacherId));

        SchoolClass schoolClass = SchoolClass.builder()
                .name(request.getName())
                .academicYear(request.getAcademicYear())
                .semester(request.getSemester())
                .maxSubjects(request.getMaxSubjects())
                .teacher(teacher)
                .createdAt(LocalDateTime.now())
                .build();

        schoolClass = schoolClassRepository.save(schoolClass);
        log.info("Created class id={} name='{}' teacherId={}", schoolClass.getId(), schoolClass.getName(), teacherId);

        return map(schoolClass, 0, 0);
    }

    @Transactional(readOnly = true)
    public List<SchoolClassResponse> getMyClasses(Long teacherId, String role) {
        requireTeacher(role);
        return schoolClassRepository.findAllByTeacherId(teacherId).stream()
                .map(c -> map(c,
                        classStudentRepository.findAllByClassId(c.getId()).size(),
                        classSubjectRepository.countBySchoolClassId(c.getId())))
                .toList();
    }

    /** Classes a student is a roster member of - the student-side counterpart to {@link #getMyClasses}. */
    @Transactional(readOnly = true)
    public List<SchoolClassResponse> getMyClassesAsStudent(Long studentId, String role) {
        if (!STUDENT_ROLE.equals(role)) {
            throw new ForbiddenException("Only students can view their own classes");
        }
        return classStudentRepository.findAllByStudentId(studentId).stream()
                .map(cs -> cs.getSchoolClass())
                .map(c -> map(c,
                        classStudentRepository.findAllByClassId(c.getId()).size(),
                        classSubjectRepository.countBySchoolClassId(c.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public SchoolClassResponse getClass(Long classId, Long principalId, String role) {
        SchoolClass schoolClass = findClassOrThrow(classId);
        requireOwnerOrAdmin(schoolClass, principalId, role);
        return map(schoolClass,
                classStudentRepository.findAllByClassId(classId).size(),
                classSubjectRepository.countBySchoolClassId(classId));
    }

    public void deleteClass(Long classId, Long principalId, String role) {
        SchoolClass schoolClass = findClassOrThrow(classId);
        requireOwnerOrAdmin(schoolClass, principalId, role);
        schoolClassRepository.delete(schoolClass);
        log.info("Deleted class id={}", classId);
    }

    /**
     * Adds students to the roster, then auto-enrolls each newly-added student into every
     * MANDATORY subject the class already has (a student joining late shouldn't have to be
     * manually enrolled in subjects that were mandatory before they arrived). Students
     * already on the roster are silently skipped rather than erroring, so re-submitting a
     * roster (e.g. a spreadsheet-imported list with some overlap) is safe.
     */
    public List<ClassStudentResponse> addStudents(Long classId, Long principalId, String role, AddStudentsRequest request) {
        SchoolClass schoolClass = findClassOrThrow(classId);
        requireOwnerOrAdmin(schoolClass, principalId, role);

        List<ClassSubject> mandatorySubjects = classSubjectRepository.findAllByClassId(classId).stream()
                .filter(s -> s.getEnrollmentMode() == ClassSubject.EnrollmentMode.MANDATORY)
                .toList();

        for (Long studentId : request.getStudentIds()) {
            if (classStudentRepository.existsBySchoolClassIdAndStudentId(classId, studentId)) {
                continue;
            }
            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> ResourceNotFoundException.of("Student", studentId));

            classStudentRepository.save(ClassStudent.builder()
                    .schoolClass(schoolClass)
                    .student(student)
                    .addedAt(LocalDateTime.now())
                    .build());

            for (ClassSubject subject : mandatorySubjects) {
                autoEnrollIfAbsent(subject, student);
            }
        }

        log.info("Added {} student(s) to class id={}", request.getStudentIds().size(), classId);
        return getRoster(classId, principalId, role);
    }

    public void removeStudent(Long classId, Long studentId, Long principalId, String role) {
        SchoolClass schoolClass = findClassOrThrow(classId);
        requireOwnerOrAdmin(schoolClass, principalId, role);

        ClassStudent entry = classStudentRepository.findBySchoolClassIdAndStudentId(classId, studentId)
                .orElseThrow(() -> new BadRequestException("This student is not on the class roster"));

        // Cascades to that student's class_enrollments rows for this class's subjects via
        // the FK's ON DELETE CASCADE - removing someone from the roster also drops the
        // enrollments that only existed because they were on it.
        classStudentRepository.delete(entry);
        log.info("Removed student id={} from class id={}", studentId, classId);
    }

    @Transactional(readOnly = true)
    public List<ClassStudentResponse> getRoster(Long classId, Long principalId, String role) {
        SchoolClass schoolClass = findClassOrThrow(classId);
        requireOwnerOrAdmin(schoolClass, principalId, role);

        return classStudentRepository.findAllByClassId(classId).stream()
                .map(cs -> ClassStudentResponse.builder()
                        .studentId(cs.getStudent().getId())
                        .admissionNo(cs.getStudent().getAdmissionNo())
                        .studentName(cs.getStudent().getFirstName() + " " +
                                (cs.getStudent().getLastName() != null ? cs.getStudent().getLastName() : ""))
                        .addedAt(cs.getAddedAt())
                        .build())
                .toList();
    }

    /** Package-private: reused by {@link ClassSubjectService} for the reverse auto-enroll direction. */
    void autoEnrollIfAbsent(ClassSubject subject, Student student) {
        if (classEnrollmentRepository.existsByClassSubjectIdAndStudentId(subject.getId(), student.getId())) {
            return;
        }
        classEnrollmentRepository.save(ClassEnrollment.builder()
                .classSubject(subject)
                .student(student)
                .source(ClassEnrollment.Source.AUTO)
                .enrolledAt(LocalDateTime.now())
                .build());
    }

    SchoolClass findClassOrThrow(Long classId) {
        return schoolClassRepository.findByIdWithTeacher(classId)
                .orElseThrow(() -> ResourceNotFoundException.of("Class", classId));
    }

    List<ClassStudent> rosterOf(Long classId) {
        return classStudentRepository.findAllByClassId(classId);
    }

    static void requireTeacher(String role) {
        if (!TEACHER_ROLE.equals(role)) {
            throw new ForbiddenException("Only teachers can manage classes");
        }
    }

    static void requireOwnerOrAdmin(SchoolClass schoolClass, Long principalId, String role) {
        if (ADMIN_ROLE.equals(role)) {
            return;
        }
        if (!TEACHER_ROLE.equals(role) || !schoolClass.getTeacher().getId().equals(principalId)) {
            throw new ForbiddenException("You do not have access to this class");
        }
    }

    private SchoolClassResponse map(SchoolClass c, int studentCount, int subjectCount) {
        return SchoolClassResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .academicYear(c.getAcademicYear())
                .semester(c.getSemester())
                .maxSubjects(c.getMaxSubjects())
                .teacherId(c.getTeacher().getId())
                .teacherName(c.getTeacher().getFirstName() + " " + c.getTeacher().getLastName())
                .studentCount(studentCount)
                .subjectCount(subjectCount)
                .build();
    }
}
