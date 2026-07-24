package com.collegeerp.Backend.teacher.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceTrendPointResponse {
    /** e.g. "Mon", "Tue" - short weekday label for chart axes. */
    private String label;
    /** 0-100, rounded. 0 if no attendance was recorded that day. */
    private int ratePercentage;
}
