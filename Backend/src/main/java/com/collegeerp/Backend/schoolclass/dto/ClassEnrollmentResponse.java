package com.collegeerp.Backend.schoolclass.dto;

import com.collegeerp.Backend.schoolclass.entity.ClassEnrollment;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassEnrollmentResponse {

    private Long id;
    private Long classSubjectId;
    private String subjectCode;
    private String subjectName;
    private Long studentId;
    private String studentName;
    private ClassEnrollment.Source source;
    private LocalDateTime enrolledAt;
}
