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

    /** One of NOT_SUBMITTED, SUBMITTED, or the submission's own status (e.g. GRADED) once evaluated. */
    private String submissionStatus;

    private LocalDateTime submittedAt;

    /** Null until the teacher has graded the submission. */
    private Integer marksObtained;

    private String feedback;
}
