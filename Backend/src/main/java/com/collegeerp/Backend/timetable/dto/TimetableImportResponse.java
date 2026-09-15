package com.collegeerp.Backend.timetable.dto;

import java.util.List;

public record TimetableImportResponse(
        List<Candidate> entries,
        List<String> warnings) {

    public record Candidate(
            Long classSubjectId,
            String className,
            String subjectName,
            String teacherName,
            String dayOfWeek,
            String startTime,
            String endTime,
            String room,
            Double confidence) {
    }
}
