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
    private String schoolClassName;
    private String academicYear;
    private Integer semester;
    private String subjectCode;
    private String subjectName;
    private Integer credits;
    private Long teacherId;
    private String teacherName;
    private ClassSubject.EnrollmentMode enrollmentMode;
    private int enrolledCount;
    private Boolean enrolledByMe;
    private Long linkedSubjectId;
    private String linkedSubjectName;
}
