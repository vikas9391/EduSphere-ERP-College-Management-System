package com.collegeerp.Backend.schoolclass.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassStudentResponse {

    private Long studentId;
    private String admissionNo;
    private String studentName;
    private LocalDateTime addedAt;
}
