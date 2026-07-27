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
     * "CLASS_ROSTER" when eligibility came from a linked ClassSubject's roster
     * (class_enrollments), or "FORMAL_ENROLLMENT" when it fell back to the plain
     * Enrollment table because no class-subject is linked to this Subject.
     */
    private String source;

    /** Whether marks already exist for this student on this exam schedule. */
    private boolean alreadyGraded;
}
