package com.collegeerp.Backend.marks.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarksRequest {

    private Long examScheduleId;

    private Long studentId;

    private Integer internalMarks;

    private Integer externalMarks;

}
