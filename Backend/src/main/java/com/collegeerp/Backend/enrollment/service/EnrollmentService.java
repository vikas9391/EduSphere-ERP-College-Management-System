package com.collegeerp.Backend.enrollment.service;

import com.collegeerp.Backend.enrollment.dto.EnrollmentRequest;
import com.collegeerp.Backend.enrollment.dto.EnrollmentResponse;
import com.collegeerp.Backend.enrollment.entity.Enrollment;
import com.collegeerp.Backend.enrollment.repository.EnrollmentRepository;
import com.collegeerp.Backend.student.entity.Student;
import com.collegeerp.Backend.student.repository.StudentRepository;
import com.collegeerp.Backend.subject.entity.Subject;
import com.collegeerp.Backend.subject.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;

    public EnrollmentService(
            EnrollmentRepository enrollmentRepository,
            StudentRepository studentRepository,
            SubjectRepository subjectRepository) {

        this.enrollmentRepository = enrollmentRepository;
        this.studentRepository = studentRepository;
        this.subjectRepository = subjectRepository;
    }

    public EnrollmentResponse createEnrollment(EnrollmentRequest request) {

        if (enrollmentRepository.existsByStudentIdAndSubjectId(
                request.getStudentId(),
                request.getSubjectId())) {

            throw new RuntimeException("Student already enrolled.");
        }

        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new RuntimeException("Subject not found"));

        Enrollment enrollment = Enrollment.builder()
                .student(student)
                .subject(subject)
                .academicYear(request.getAcademicYear())
                .semester(request.getSemester())
                .enrollmentDate(request.getEnrollmentDate())
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();

        enrollment = enrollmentRepository.save(enrollment);

        return map(enrollment);
    }

    public List<EnrollmentResponse> getAllEnrollments() {

        return enrollmentRepository.findAll()
                .stream()
                .map(this::map)
                .toList();
    }

    public EnrollmentResponse getEnrollment(Long id) {

        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));

        return map(enrollment);
    }

    public void deleteEnrollment(Long id) {

        if (!enrollmentRepository.existsById(id)) {
            throw new RuntimeException("Enrollment not found");
        }

        enrollmentRepository.deleteById(id);
    }

    private EnrollmentResponse map(Enrollment e) {

        return EnrollmentResponse.builder()
                .id(e.getId())
                .studentId(e.getStudent().getId())
                .studentName(e.getStudent().getFirstName() + " " + e.getStudent().getLastName())
                .admissionNo(e.getStudent().getAdmissionNo())
                .subjectId(e.getSubject().getId())
                .subjectName(e.getSubject().getSubjectName())
                .subjectCode(e.getSubject().getSubjectCode())
                .courseName(e.getSubject().getCourse().getCourseName())
                .teacherName(e.getSubject().getTeacher().getFirstName() + " " + e.getSubject().getTeacher().getLastName())
                .academicYear(e.getAcademicYear())
                .semester(e.getSemester())
                .enrollmentDate(e.getEnrollmentDate())
                .status(e.getStatus())
                .build();
    }
}