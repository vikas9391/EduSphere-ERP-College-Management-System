package com.collegeerp.Backend.teacher.service;

import com.collegeerp.Backend.enrollment.dto.EnrollmentResponse;
import com.collegeerp.Backend.schoolclass.entity.ClassEnrollment;
import com.collegeerp.Backend.schoolclass.repository.ClassEnrollmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Teacher-scoped roster derived from exact ClassEnrollment relationships. The legacy
 * EnrollmentResponse shape is retained for frontend compatibility while the source is now
 * ClassSubject -> ClassEnrollment, preventing students from unrelated classes leaking in.
 */
@Service
@Transactional(readOnly = true)
public class TeacherStudentQueryService {

    private final ClassEnrollmentRepository classEnrollmentRepository;

    public TeacherStudentQueryService(ClassEnrollmentRepository classEnrollmentRepository) {
        this.classEnrollmentRepository = classEnrollmentRepository;
    }

    public List<EnrollmentResponse> getStudents(Long teacherId) {
        return classEnrollmentRepository.findAllByTeacherId(teacherId)
                .stream()
                .map(this::map)
                .toList();
    }

    private EnrollmentResponse map(ClassEnrollment e) {
        var cs = e.getClassSubject();
        var formalSubject = cs.getSubject();
        var schoolClass = cs.getSchoolClass();
        var teacher = cs.getTeacher();

        String courseName = formalSubject != null && formalSubject.getCourse() != null
                ? formalSubject.getCourse().getCourseName()
                : schoolClass.getName();

        return EnrollmentResponse.builder()
                .id(e.getId())
                .studentId(e.getStudent().getId())
                .studentName((e.getStudent().getFirstName() + " "
                        + (e.getStudent().getLastName() != null ? e.getStudent().getLastName() : "")).trim())
                .admissionNo(e.getStudent().getAdmissionNo())
                .subjectId(formalSubject != null ? formalSubject.getId() : cs.getId())
                .subjectName(cs.getSubjectName())
                .subjectCode(cs.getSubjectCode())
                .courseName(courseName)
                .teacherName(teacher != null
                        ? (teacher.getFirstName() + " " + teacher.getLastName()).trim() : null)
                .academicYear(schoolClass.getAcademicYear())
                .semester(schoolClass.getSemester())
                .enrollmentDate(e.getEnrolledAt() != null ? e.getEnrolledAt().toLocalDate() : null)
                .status("ACTIVE")
                .build();
    }
}
