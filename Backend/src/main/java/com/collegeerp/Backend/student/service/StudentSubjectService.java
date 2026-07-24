package com.collegeerp.Backend.student.service;

import com.collegeerp.Backend.enrollment.entity.Enrollment;
import com.collegeerp.Backend.enrollment.repository.EnrollmentRepository;
import com.collegeerp.Backend.student.dto.StudentSubjectResponse;
import com.collegeerp.Backend.subject.entity.Subject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class StudentSubjectService {

    private final EnrollmentRepository enrollmentRepository;

    public StudentSubjectService(EnrollmentRepository enrollmentRepository) {
        this.enrollmentRepository = enrollmentRepository;
    }

    public List<StudentSubjectResponse> getSubjects(Long studentId) {
        return enrollmentRepository.findByStudentIdWithDetails(studentId)
                .stream()
                .map(Enrollment::getSubject)
                .sorted(Comparator.comparing(Subject::getSubjectCode))
                .map(this::map)
                .toList();
    }

    private StudentSubjectResponse map(Subject s) {
        return StudentSubjectResponse.builder()
                .subjectId(s.getId())
                .subjectCode(s.getSubjectCode())
                .subjectName(s.getSubjectName())
                .credits(s.getCredits())
                .semester(s.getSemester())
                .courseId(s.getCourse().getId())
                .courseName(s.getCourse().getCourseName())
                .teacherId(s.getTeacher().getId())
                .teacherName(s.getTeacher().getFirstName() + " " + s.getTeacher().getLastName())
                .build();
    }
}
