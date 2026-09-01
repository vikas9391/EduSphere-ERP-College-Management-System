package com.collegeerp.Backend.schoolclass.controller;

import com.collegeerp.Backend.schoolclass.dto.ClassSubjectResponse;
import com.collegeerp.Backend.schoolclass.entity.ClassSubject;
import com.collegeerp.Backend.schoolclass.repository.ClassEnrollmentRepository;
import com.collegeerp.Backend.schoolclass.repository.ClassSubjectRepository;
import com.collegeerp.Backend.security.UserPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Staff-scoped read endpoint for exact ClassSubject offerings used by operational forms. */
@RestController
@RequestMapping("/api/classes/subjects")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','TEACHER')")
public class TeacherClassSubjectController {

    private final ClassSubjectRepository classSubjectRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;

    public TeacherClassSubjectController(
            ClassSubjectRepository classSubjectRepository,
            ClassEnrollmentRepository classEnrollmentRepository) {
        this.classSubjectRepository = classSubjectRepository;
        this.classEnrollmentRepository = classEnrollmentRepository;
    }

    @GetMapping("/mine-teaching")
    public List<ClassSubjectResponse> mineTeaching(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        List<ClassSubject> subjects = "TEACHER".equalsIgnoreCase(principal.getRole())
                ? classSubjectRepository.findAllByTeacherId(principal.getId())
                : classSubjectRepository.findAllWithRelations();
        return subjects.stream().map(this::map).toList();
    }

    private ClassSubjectResponse map(ClassSubject s) {
        return ClassSubjectResponse.builder()
                .id(s.getId())
                .schoolClassId(s.getSchoolClass().getId())
                .schoolClassName(s.getSchoolClass().getName())
                .academicYear(s.getSchoolClass().getAcademicYear())
                .semester(s.getSchoolClass().getSemester())
                .subjectCode(s.getSubjectCode())
                .subjectName(s.getSubjectName())
                .credits(s.getCredits())
                .teacherId(s.getTeacher().getId())
                .teacherName(s.getTeacher().getFirstName() + " " +
                        (s.getTeacher().getLastName() != null ? s.getTeacher().getLastName() : ""))
                .enrollmentMode(s.getEnrollmentMode())
                .enrolledCount(classEnrollmentRepository.findAllByClassSubjectId(s.getId()).size())
                .linkedSubjectId(s.getSubject() != null ? s.getSubject().getId() : null)
                .linkedSubjectName(s.getSubject() != null ? s.getSubject().getSubjectName() : null)
                .build();
    }
}
