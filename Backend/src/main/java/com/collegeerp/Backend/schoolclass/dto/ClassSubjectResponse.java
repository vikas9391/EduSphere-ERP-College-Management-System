package com.collegeerp.Backend.schoolclass.dto;

import com.collegeerp.Backend.schoolclass.entity.ClassSubject;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassSubjectResponse {

    private Long id;
    private Long schoolClassId;
    private String subjectCode;
    private String subjectName;
    private Integer credits;
    private Long teacherId;
    private String teacherName;
    private ClassSubject.EnrollmentMode enrollmentMode;
    private int enrolledCount;

    /**
     * Whether the calling student is enrolled in this subject. Only populated by
     * student-facing endpoints (e.g. {@code GET /classes/{id}/subjects/mine}); left
     * null for the teacher/admin listing, where it isn't meaningful.
     */
    private Boolean enrolledByMe;
}
