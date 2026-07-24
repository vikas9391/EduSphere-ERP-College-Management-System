package com.collegeerp.Backend.teacher.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherAssignmentResponse {

    private Long assignmentId;
    private String title;
    private Long subjectId;
    private String subjectName;
    private LocalDate dueDate;
    private Integer maxMarks;

    private Integer totalSubmissions;

    /** Submissions with {@code status == "EVALUATED"} (case-insensitive). */
    private Integer evaluatedCount;

    /** {@code totalSubmissions - evaluatedCount} - submitted but not yet graded. */
    private Integer pendingReviewCount;
}
