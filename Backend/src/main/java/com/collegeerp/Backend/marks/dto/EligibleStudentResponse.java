package com.collegeerp.Backend.marks.dto;

import lombok.*;

/**
 * One student eligible to have marks entered for a given exam schedule, plus where that
 * eligibility came from - see {@code MarksService#getEligibleStudents}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EligibleStudentResponse {

    private Long studentId;
    private String studentName;

    /**
     * Eligibility is always derived from the class-scoped ClassSubject/ClassEnrollment
     * attached to the exam schedule. Legacy formal-enrollment fallback is no longer used.
     */
    private String source;

    /** Whether marks already exist for this student on this exam schedule. */
    private boolean alreadyGraded;
}
