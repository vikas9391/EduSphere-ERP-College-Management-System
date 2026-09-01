package com.collegeerp.Backend.student.service;

import com.collegeerp.Backend.schoolclass.entity.ClassEnrollment;
import com.collegeerp.Backend.schoolclass.entity.ClassSubject;
import com.collegeerp.Backend.schoolclass.repository.ClassEnrollmentRepository;
import com.collegeerp.Backend.student.dto.StudentSubjectResponse;
import com.collegeerp.Backend.subject.entity.Subject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Student's active subjects are derived from ClassEnrollment -> ClassSubject.
 * The class-based relationship is authoritative for operational student participation.
 */
@Service
@Transactional(readOnly = true)
public class StudentSubjectService {

    private final ClassEnrollmentRepository classEnrollmentRepository;

    public StudentSubjectService(ClassEnrollmentRepository classEnrollmentRepository) {
        this.classEnrollmentRepository = classEnrollmentRepository;
    }

    public List<StudentSubjectResponse> getSubjects(Long studentId) {
        return classEnrollmentRepository.findAllByStudentId(studentId)
                .stream()
                .map(ClassEnrollment::getClassSubject)
                .filter(java.util.Objects::nonNull)
                .map(ClassSubject::getSubject)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .sorted(Comparator.comparing(
                        Subject::getSubjectCode,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
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
                .courseId(s.getCourse() != null ? s.getCourse().getId() : null)
                .courseName(s.getCourse() != null ? s.getCourse().getCourseName() : null)
                .teacherId(s.getTeacher() != null ? s.getTeacher().getId() : null)
                .teacherName(s.getTeacher() == null
                        ? null
                        : s.getTeacher().getFirstName() + " " + s.getTeacher().getLastName())
                .build();
    }
}
