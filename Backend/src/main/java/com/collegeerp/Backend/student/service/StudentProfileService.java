package com.collegeerp.Backend.student.service;

import com.collegeerp.Backend.common.exception.BadRequestException;
import com.collegeerp.Backend.common.exception.ResourceNotFoundException;
import com.collegeerp.Backend.student.auth.StudentPasswordRequest;
import com.collegeerp.Backend.student.auth.StudentProfileResponse;
import com.collegeerp.Backend.student.auth.StudentProfileUpdateRequest;
import com.collegeerp.Backend.student.entity.Student;
import com.collegeerp.Backend.student.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Self-service operations a logged-in student performs on their own record. Deliberately
 * separate from {@link StudentService} (admin CRUD): every method here is scoped to the
 * calling student's own id (enforced by the controller passing {@code principal.getId()}),
 * and the editable field set is intentionally narrower - see {@link StudentProfileUpdateRequest}.
 */
@Service
@Transactional
public class StudentProfileService {

    private static final Logger log = LoggerFactory.getLogger(StudentProfileService.class);

    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    public StudentProfileService(StudentRepository studentRepository, PasswordEncoder passwordEncoder) {
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public StudentProfileResponse getProfile(Long studentId) {
        return map(findStudentOrThrow(studentId));
    }

    public StudentProfileResponse updateProfile(Long studentId, StudentProfileUpdateRequest request) {

        Student student = findStudentOrThrow(studentId);

        student.setPhone(request.getPhone());
        student.setGender(request.getGender());
        student.setDateOfBirth(request.getDateOfBirth());

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
        student.setAadhaarNumber(request.getAadhaarNumber());
        student.setPhotoUrl(request.getPhotoUrl());

        student = studentRepository.save(student);
        log.info("Student id={} updated their own profile", studentId);

        return map(student);
    }

    public void changePassword(Long studentId, StudentPasswordRequest request) {

        Student student = findStudentOrThrow(studentId);

        if (!passwordEncoder.matches(request.getOldPassword(), student.getPasswordHash())) {
            throw new BadRequestException("Old password is incorrect");
        }

        if (passwordEncoder.matches(request.getNewPassword(), student.getPasswordHash())) {
            throw new BadRequestException("New password must be different from the current password");
        }

        student.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        studentRepository.save(student);

        log.info("Student id={} changed their password", studentId);
    }

    private Student findStudentOrThrow(Long studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Student", studentId));
    }

    private StudentProfileResponse map(Student student) {
        return StudentProfileResponse.builder()
                .id(student.getId())
                .admissionNo(student.getAdmissionNo())
                .rollNumber(student.getRollNumber())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .email(student.getEmail())
                .phone(student.getPhone())
                .gender(student.getGender())
                .dateOfBirth(student.getDateOfBirth())
                .admissionDate(student.getAdmissionDate())
                .address(student.getAddress())
                .city(student.getCity())
                .state(student.getState())
                .country(student.getCountry())
                .pincode(student.getPincode())
                .fatherName(student.getFatherName())
                .motherName(student.getMotherName())
                .parentPhone(student.getParentPhone())
                .parentEmail(student.getParentEmail())
                .bloodGroup(student.getBloodGroup())
                .category(student.getCategory())
                .nationality(student.getNationality())
                .photoUrl(student.getPhotoUrl())
                .status(student.getStatus())
                .build();
    }
}
