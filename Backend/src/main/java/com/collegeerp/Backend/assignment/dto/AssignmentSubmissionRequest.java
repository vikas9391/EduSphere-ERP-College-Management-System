package com.collegeerp.Backend.assignment.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentSubmissionRequest {

    private Long assignmentId;

    private Long studentId;

    private String submissionUrl;

}