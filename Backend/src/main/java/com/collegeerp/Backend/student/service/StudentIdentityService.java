package com.collegeerp.Backend.student.service;

import com.collegeerp.Backend.common.exception.ResourceNotFoundException;
import com.collegeerp.Backend.security.UserPrincipal;
import com.collegeerp.Backend.student.entity.Student;
import com.collegeerp.Backend.student.repository.StudentRepository;
import org.springframework.stereotype.Service;

/**
 * Single source of truth for mapping an authenticated account to the domain Student.
 * Student-facing services/controllers must use this instead of assuming UserPrincipal.id
 * is interchangeable with Student.id.
 */
@Service
public class StudentIdentityService {

    private final StudentRepository studentRepository;

    public StudentIdentityService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    public Student requireStudent(UserPrincipal principal) {
        if (principal == null || principal.getEmail() == null || principal.getEmail().isBlank()) {
            throw new ResourceNotFoundException("Student profile not found for authenticated user");
        }
        return studentRepository.findByEmail(principal.getEmail().trim())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Student profile not found for authenticated user"));
    }

    public Long requireStudentId(UserPrincipal principal) {
        return requireStudent(principal).getId();
    }
}
