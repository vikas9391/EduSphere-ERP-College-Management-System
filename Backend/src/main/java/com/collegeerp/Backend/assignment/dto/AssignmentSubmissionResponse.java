package com.collegeerp.Backend.assignment.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentSubmissionResponse {

    private Long id;

    private Long assignmentId;

    private String assignmentTitle;

    private Long studentId;

    private String studentName;

    private String submissionUrl;

    private LocalDateTime submittedAt;

    private Integer marks;

    private String feedback;

    private String status;
}