package com.collegeerp.Backend.student.service;

import com.collegeerp.Backend.common.exception.BadRequestException;
import com.collegeerp.Backend.common.exception.DuplicateResourceException;
import com.collegeerp.Backend.common.exception.ResourceNotFoundException;
import com.collegeerp.Backend.course.entity.Course;
import com.collegeerp.Backend.course.repository.CourseRepository;
import com.collegeerp.Backend.student.dto.StudentRequest;
import com.collegeerp.Backend.student.dto.StudentResponse;
import com.collegeerp.Backend.student.entity.Student;
import com.collegeerp.Backend.student.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * Administrative CRUD for students. Self-service profile/password operations live in
 * {@link StudentProfileService} instead, since they have different authorization rules
 * (a student may only ever touch their own record) and a narrower editable field set.
 */
@Service
@Transactional
public class StudentService {

    private static final Logger log = LoggerFactory.getLogger(StudentService.class);
    private static final String DEFAULT_STATUS = "ACTIVE";

    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final PasswordEncoder passwordEncoder;

    public StudentService(StudentRepository studentRepository, CourseRepository courseRepository,
                           PasswordEncoder passwordEncoder) {
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public StudentResponse createStudent(StudentRequest request) {

        if (!StringUtils.hasText(request.getPassword())) {
            throw new BadRequestException("Password is required to create a student");
        }

        if (studentRepository.existsByAdmissionNo(request.getAdmissionNo())) {
            throw new DuplicateResourceException("Admission number '" + request.getAdmissionNo() + "' is already in use");
        }

        if (studentRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email '" + request.getEmail() + "' is already in use");
        }

        if (StringUtils.hasText(request.getRollNumber())
                && studentRepository.existsByRollNumber(request.getRollNumber())) {
            throw new DuplicateResourceException("Roll number '" + request.getRollNumber() + "' is already in use");
        }

        Student student = Student.builder()
                .admissionNo(request.getAdmissionNo())
                .rollNumber(request.getRollNumber())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .gender(request.getGender())
                .dateOfBirth(request.getDateOfBirth())
                .admissionDate(request.getAdmissionDate())
                .course(resolveCourse(request.getCourseId()))
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .country(request.getCountry())
                .pincode(request.getPincode())
                .fatherName(request.getFatherName())
                .motherName(request.getMotherName())
                .parentPhone(request.getParentPhone())
                .parentEmail(request.getParentEmail())
                .bloodGroup(request.getBloodGroup())
                .category(request.getCategory())
                .nationality(request.getNationality())
                .aadhaarNumber(request.getAadhaarNumber())
                .photoUrl(request.getPhotoUrl())
                .status(DEFAULT_STATUS)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        student = studentRepository.save(student);
        log.info("Created student id={} admissionNo={}", student.getId(), student.getAdmissionNo());

        return map(student);
    }

    @Transactional(readOnly = true)
    public Page<StudentResponse> getAllStudents(Pageable pageable) {
        return studentRepository.findAllWithCourse(pageable).map(this::map);
    }

    @Transactional(readOnly = true)
    public StudentResponse getStudent(Long id) {
        return map(studentRepository.findByIdWithCourse(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Student", id)));
    }

    public StudentResponse updateStudent(Long id, StudentRequest request) {

        Student student = findStudentOrThrow(id);

        if (!student.getAdmissionNo().equals(request.getAdmissionNo())
                && studentRepository.existsByAdmissionNo(request.getAdmissionNo())) {
            throw new DuplicateResourceException("Admission number '" + request.getAdmissionNo() + "' is already in use");
        }

        if (!student.getEmail().equalsIgnoreCase(request.getEmail())
                && studentRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email '" + request.getEmail() + "' is already in use");
        }

        if (StringUtils.hasText(request.getRollNumber())
                && !request.getRollNumber().equals(student.getRollNumber())
                && studentRepository.existsByRollNumber(request.getRollNumber())) {
            throw new DuplicateResourceException("Roll number '" + request.getRollNumber() + "' is already in use");
        }

        student.setAdmissionNo(request.getAdmissionNo());
        student.setRollNumber(request.getRollNumber());
        student.setFirstName(request.getFirstName());
        student.setLastName(request.getLastName());
        student.setEmail(request.getEmail());

        if (StringUtils.hasText(request.getPassword())) {
            student.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        student.setPhone(request.getPhone());
        student.setGender(request.getGender());
        student.setDateOfBirth(request.getDateOfBirth());
        student.setAdmissionDate(request.getAdmissionDate());
        student.setCourse(resolveCourse(request.getCourseId()));
        student.setAddress(request.getAddress());
        student.setCity(request.getCity());
        student.setState(request.getState());
        student.setCountry(request.getCountry());
        student.setPincode(request.getPincode());
        student.setFatherName(request.getFatherName());
        student.setMotherName(request.getMotherName());
        student.setParentPhone(request.getParentPhone());
        student.setParentEmail(request.getParentEmail());
        student.setBloodGroup(request.getBloodGroup());
        student.setCategory(request.getCategory());
        student.setNationality(request.getNationality());

        if (StringUtils.hasText(request.getAadhaarNumber())) {
            student.setAadhaarNumber(request.getAadhaarNumber());
        }

        student.setPhotoUrl(request.getPhotoUrl());
        student.setUpdatedAt(LocalDateTime.now());

        student = studentRepository.save(student);
        log.info("Updated student id={}", student.getId());

        return map(student);
    }

    public void deleteStudent(Long id) {
        Student student = findStudentOrThrow(id);
        studentRepository.delete(student);
        log.info("Deleted student id={}", id);
    }

    private Student findStudentOrThrow(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Student", id));
    }

    /**
     * Resolves an optional courseId into a managed Course reference, or null if none was
     * given (meaning "no course" / "clear the course" depending on create vs update).
     * Throws if a non-null id doesn't correspond to a real course, so a student can never
     * be silently linked to a course that doesn't exist.
     */
    private Course resolveCourse(Long courseId) {
        if (courseId == null) {
            return null;
        }
        return courseRepository.findById(courseId)
                .orElseThrow(() -> ResourceNotFoundException.of("Course", courseId));
    }

    private StudentResponse map(Student s) {
        Course course = s.getCourse();
        return StudentResponse.builder()
                .id(s.getId())
                .admissionNo(s.getAdmissionNo())
                .rollNumber(s.getRollNumber())
                .firstName(s.getFirstName())
                .lastName(s.getLastName())
                .email(s.getEmail())
                .phone(s.getPhone())
                .gender(s.getGender())
                .dateOfBirth(s.getDateOfBirth())
                .admissionDate(s.getAdmissionDate())
                .courseId(course != null ? course.getId() : null)
                .courseName(course != null ? course.getCourseName() : null)
                .address(s.getAddress())
                .city(s.getCity())
                .state(s.getState())
                .country(s.getCountry())
                .pincode(s.getPincode())
                .fatherName(s.getFatherName())
                .motherName(s.getMotherName())
                .parentPhone(s.getParentPhone())
                .parentEmail(s.getParentEmail())
                .bloodGroup(s.getBloodGroup())
                .category(s.getCategory())
                .nationality(s.getNationality())
                .photoUrl(s.getPhotoUrl())
                .status(s.getStatus())
                .build();
    }
}
