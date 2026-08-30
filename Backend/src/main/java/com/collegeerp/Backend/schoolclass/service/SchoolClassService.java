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
import com.collegeerp.Backend.common.User;
import com.collegeerp.Backend.common.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Class creation and roster management.
 *
 * Classes are visible to all teachers in the tenant. Any teacher may add students to a
 * class roster, while destructive class operations remain owner/admin controlled.
 * Auto-enrollment of new roster members into existing MANDATORY subjects happens here;
 * the reverse direction (a new MANDATORY subject enrolling the existing roster) lives in
 * {@link ClassSubjectService}.
 */
@Service
@Transactional
public class SchoolClassService {

    private static final Logger log = LoggerFactory.getLogger(SchoolClassService.class);
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";
    private static final String TEACHER_ROLE = "TEACHER";
    private static final String STUDENT_ROLE = "STUDENT";

    private final SchoolClassRepository schoolClassRepository;
    private final ClassStudentRepository classStudentRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;

    public SchoolClassService(SchoolClassRepository schoolClassRepository,
                               ClassStudentRepository classStudentRepository,
                               ClassSubjectRepository classSubjectRepository,
                               ClassEnrollmentRepository classEnrollmentRepository,
                               UserRepository userRepository,
                               StudentRepository studentRepository) {
        this.schoolClassRepository = schoolClassRepository;
        this.classStudentRepository = classStudentRepository;
        this.classSubjectRepository = classSubjectRepository;
        this.classEnrollmentRepository = classEnrollmentRepository;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
    }

    public SchoolClassResponse createClass(Long teacherId, String role, SchoolClassRequest request) {
        requireTeacher(role);

        User teacher = userRepository.findById(teacherId)
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

    /**
     * All teachers can see all classes in the current tenant. This intentionally does not
     * use the creator/owner as a filter; classes are shared teaching resources.
     */
    @Transactional(readOnly = true)
    public List<SchoolClassResponse> getMyClasses(Long teacherId, String role) {
        requireTeacher(role);
        return schoolClassRepository.findAllWithTeacher().stream()
                .map(c -> map(c,
                        classStudentRepository.findAllByClassId(c.getId()).size(),
                        classSubjectRepository.countBySchoolClassId(c.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SchoolClassResponse> getMyClassesAsStudent(Long studentId, String role) {
        if (!STUDENT_ROLE.equalsIgnoreCase(role)) {
            throw new ForbiddenException("Only students can view their own classes");
        }
        return classStudentRepository.findAllByStudentId(studentId).stream()
                .map(cs -> cs.getSchoolClass())
                .map(c -> map(c,
                        classStudentRepository.findAllByClassId(c.getId()).size(),
                        classSubjectRepository.countBySchoolClassId(c.getId())))
                .toList();
    }

    /** Any teacher may inspect a shared class. */
    @Transactional(readOnly = true)
    public SchoolClassResponse getClass(Long classId, Long principalId, String role) {
        SchoolClass schoolClass = findClassOrThrow(classId);
        requireTeacherOrAdmin(role);
        return map(schoolClass,
                classStudentRepository.findAllByClassId(classId).size(),
                classSubjectRepository.countBySchoolClassId(classId));
    }

    /** Class deletion remains restricted to the creator/admin to avoid destructive conflicts. */
    public void deleteClass(Long classId, Long principalId, String role) {
        SchoolClass schoolClass = findClassOrThrow(classId);
        requireOwnerOrAdmin(schoolClass, principalId, role);
        schoolClassRepository.delete(schoolClass);
        log.info("Deleted class id={}", classId);
    }

    /**
     * Any teacher may add students to any shared class. Newly-added students are also
     * auto-enrolled into the class's existing MANDATORY subjects.
     */
    public List<ClassStudentResponse> addStudents(Long classId, Long principalId, String role, AddStudentsRequest request) {
        SchoolClass schoolClass = findClassOrThrow(classId);
        requireTeacherOrAdmin(role);

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

        log.info("Teacher/admin id={} added {} student(s) to shared class id={}",
                principalId, request.getStudentIds().size(), classId);
        return getRoster(classId, principalId, role);
    }

    /** Removing a student remains owner/admin controlled because it changes the shared roster. */
    public void removeStudent(Long classId, Long studentId, Long principalId, String role) {
        SchoolClass schoolClass = findClassOrThrow(classId);
        requireOwnerOrAdmin(schoolClass, principalId, role);

        ClassStudent entry = classStudentRepository.findBySchoolClassIdAndStudentId(classId, studentId)
                .orElseThrow(() -> new BadRequestException("This student is not on the class roster"));

        classStudentRepository.delete(entry);
        log.info("Removed student id={} from class id={}", studentId, classId);
    }

    @Transactional(readOnly = true)
    public List<ClassStudentResponse> getRoster(Long classId, Long principalId, String role) {
        findClassOrThrow(classId);
        requireTeacherOrAdmin(role);

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

    static void requireTeacherOrAdmin(String role) {
        if (!TEACHER_ROLE.equalsIgnoreCase(role) && !ADMIN_ROLE.equalsIgnoreCase(role) && !SUPER_ADMIN_ROLE.equalsIgnoreCase(role)) {
            throw new ForbiddenException("Only teachers or admins can access shared classes");
        }
    }

    static void requireOwnerOrAdmin(SchoolClass schoolClass, Long principalId, String role) {
        if (ADMIN_ROLE.equalsIgnoreCase(role) || SUPER_ADMIN_ROLE.equalsIgnoreCase(role)) {
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
