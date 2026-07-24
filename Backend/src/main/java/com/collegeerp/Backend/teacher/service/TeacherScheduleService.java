package com.collegeerp.Backend.teacher.service;

import com.collegeerp.Backend.subject.entity.Subject;
import com.collegeerp.Backend.teacher.dto.TeacherScheduleEntryResponse;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <b>PLACEHOLDER / MOCK DATA.</b>
 * <p>
 * Same gap noted for the student side in {@code StudentTimetableService}: there is no
 * {@code Timetable}/{@code Period} entity in this schema, so there is no real record of which
 * subject a teacher has in which room at which time. This deterministically assigns the
 * teacher's own real subjects (up to 4) to a fixed set of time slots for "today" - the
 * subject names are real, the times and rooms are not read from any actual schedule.
 * Replace this class's body with a real repository-backed query once a Timetable module
 * exists. This was previously hard-coded directly in the frontend's TeacherDashboard
 * component (see README_PROGRESS.md); moving it server-side keeps the placeholder in one
 * place and lets every client share it.
 */
@Service
public class TeacherScheduleService {

    private static final List<String[]> SLOTS = List.of(
            new String[]{"09:00", "10:00", "Room 101"},
            new String[]{"10:15", "11:15", "Room 204"},
            new String[]{"11:30", "12:30", "Lab 3"},
            new String[]{"14:00", "15:00", "Room 108"}
    );

    public List<TeacherScheduleEntryResponse> getTodaysSchedule(List<Subject> subjects) {
        List<TeacherScheduleEntryResponse> result = new java.util.ArrayList<>();
        int limit = Math.min(subjects.size(), SLOTS.size());
        for (int i = 0; i < limit; i++) {
            Subject subject = subjects.get(i);
            String[] slot = SLOTS.get(i);
            result.add(TeacherScheduleEntryResponse.builder()
                    .subjectId(subject.getId())
                    .subjectName(subject.getSubjectName())
                    .startTime(slot[0])
                    .endTime(slot[1])
                    .room(slot[2])
                    .build());
        }
        return result;
    }
}
