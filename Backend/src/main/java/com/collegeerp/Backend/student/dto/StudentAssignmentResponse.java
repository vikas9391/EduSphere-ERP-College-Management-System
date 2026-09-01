package com.collegeerp.Backend.student.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentAssignmentResponse {

    private Long assignmentId;
    private String title;
    private String description;

    private Long subjectId;
    private String subjectName;
    private String teacherName;

    private LocalDate dueDate;
    private Integer maxMarks;

    /** One of NOT_SUBMITTED, SUBMITTED, EVALUATED, etc. */
    private String submissionStatus;

    private LocalDateTime submittedAt;
    private String submissionUrl;

    /** Null until the teacher has graded the submission. */
    private Integer marksObtained;

    private String feedback;
}
