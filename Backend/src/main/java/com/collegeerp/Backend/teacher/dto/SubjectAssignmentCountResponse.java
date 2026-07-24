package com.collegeerp.Backend.teacher.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectAssignmentCountResponse {
    private String subjectName;
    private long count;
}
