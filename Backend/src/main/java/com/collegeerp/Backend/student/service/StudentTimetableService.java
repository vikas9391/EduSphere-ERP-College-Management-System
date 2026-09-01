package com.collegeerp.Backend.student.service;

import com.collegeerp.Backend.student.dto.StudentTimetableResponse;
import com.collegeerp.Backend.student.dto.TimetableEntryResponse;
import com.collegeerp.Backend.timetable.entity.TimetableEntry;
import com.collegeerp.Backend.timetable.repository.TimetableEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Student timetable derived from real ClassEnrollment -> ClassSubject schedule entries. */
@Service
@Transactional(readOnly = true)
public class StudentTimetableService {

    private final TimetableEntryRepository timetableEntryRepository;

    public StudentTimetableService(TimetableEntryRepository timetableEntryRepository) {
        this.timetableEntryRepository = timetableEntryRepository;
    }

    public StudentTimetableResponse getTimetable(Long studentId) {
        List<TimetableEntry> entries = timetableEntryRepository.findAllForStudent(studentId);

        Map<String, List<TimetableEntryResponse>> schedule = new LinkedHashMap<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            schedule.put(day.name(), new ArrayList<>());
        }

        for (TimetableEntry entry : entries) {
            var classSubject = entry.getClassSubject();
            String teacherName = classSubject.getTeacher() == null
                    ? null
                    : (classSubject.getTeacher().getFirstName() + " "
                    + classSubject.getTeacher().getLastName()).trim();

            schedule.get(entry.getDayOfWeek().name()).add(TimetableEntryResponse.builder()
                    .startTime(entry.getStartTime().toString())
                    .endTime(entry.getEndTime().toString())
                    .subjectId(classSubject.getSubject() != null
                            ? classSubject.getSubject().getId() : classSubject.getId())
                    .subjectName(classSubject.getSubjectName())
                    .teacherName(teacherName)
                    .room(entry.getRoom() != null ? entry.getRoom() : "TBD")
                    .build());
        }

        return StudentTimetableResponse.builder()
                .placeholder(false)
                .note(entries.isEmpty()
                        ? "No timetable slots have been scheduled for your enrolled class subjects yet."
                        : "Schedule is based on your current class-subject enrollments.")
                .schedule(schedule)
                .build();
    }
}
