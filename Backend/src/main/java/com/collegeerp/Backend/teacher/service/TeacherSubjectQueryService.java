package com.collegeerp.Backend.teacher.service;

import com.collegeerp.Backend.subject.dto.SubjectResponse;
import com.collegeerp.Backend.subject.entity.Subject;
import com.collegeerp.Backend.subject.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Read-only, teacher-scoped view over {@link Subject}. Reuses {@link SubjectResponse} (the
 * same DTO the admin {@code SubjectController} returns) rather than inventing a parallel DTO,
 * mirroring {@code StudentEnrollmentQueryService}'s reuse of {@code EnrollmentResponse}.
 */
@Service
@Transactional(readOnly = true)
public class TeacherSubjectQueryService {

    private final SubjectRepository subjectRepository;

    public TeacherSubjectQueryService(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    public List<SubjectResponse> getSubjects(Long teacherId) {
        return subjectRepository.findByTeacherIdWithRelations(teacherId)
                .stream()
                .map(this::map)
                .toList();
    }

    private SubjectResponse map(Subject s) {
        return SubjectResponse.builder()
                .id(s.getId())
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
