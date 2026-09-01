package com.collegeerp.Backend.student.service;

import com.collegeerp.Backend.schoolclass.entity.ClassEnrollment;
import com.collegeerp.Backend.student.dto.StudentTimetableResponse;
import com.collegeerp.Backend.student.dto.TimetableEntryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Placeholder timetable service. The schema still has no Timetable/Period model, so the
 * day/time/room values remain non-authoritative. Subject and teacher data now come from the
 * student's authoritative ClassEnrollment -> ClassSubject relationship.
 */
@Service
@Transactional(readOnly = true)
public class StudentTimetableService {

    private static final List<String> DAYS = List.of(
            "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY");
    private static final List<String[]> SLOTS = List.of(
            new String[]{"09:00", "10:00"},
            new String[]{"10:15", "11:15"},
            new String[]{"11:30", "12:30"},
            new String[]{"13:30", "14:30"}
    );

    private final com.collegeerp.Backend.schoolclass.repository.ClassEnrollmentRepository classEnrollmentRepository;

    public StudentTimetableService(
            com.collegeerp.Backend.schoolclass.repository.ClassEnrollmentRepository classEnrollmentRepository) {
        this.classEnrollmentRepository = classEnrollmentRepository;
    }

    public StudentTimetableResponse getTimetable(Long studentId) {
        List<ClassEnrollment> enrollments = classEnrollmentRepository.findAllByStudentId(studentId);

        Map<String, List<TimetableEntryResponse>> schedule = new LinkedHashMap<>();
        for (String day : DAYS) {
            schedule.put(day, new ArrayList<>());
        }

        int slotCursor = 0;
        for (ClassEnrollment enrollment : enrollments) {
            var classSubject = enrollment.getClassSubject();
            if (classSubject == null) {
                continue;
            }

            int dayIndex = slotCursor % DAYS.size();
            int slotIndex = (slotCursor / DAYS.size()) % SLOTS.size();
            String[] slot = SLOTS.get(slotIndex);

            String subjectName = classSubject.getSubjectName();
            String teacherName = classSubject.getTeacher() == null
                    ? null
                    : classSubject.getTeacher().getFirstName() + " " +
                      (classSubject.getTeacher().getLastName() != null
                              ? classSubject.getTeacher().getLastName() : "");

            schedule.get(DAYS.get(dayIndex)).add(TimetableEntryResponse.builder()
                    .startTime(slot[0])
                    .endTime(slot[1])
                    .subjectId(classSubject.getSubject() != null
                            ? classSubject.getSubject().getId() : classSubject.getId())
                    .subjectName(subjectName)
                    .teacherName(teacherName)
                    .room("TBD")
                    .build());

            slotCursor++;
        }

        return StudentTimetableResponse.builder()
                .placeholder(true)
                .note("Placeholder data: no timetable/period module exists in the system yet. "
                        + "Subjects and teachers are real; day/time/room assignments are mock.")
                .schedule(schedule)
                .build();
    }
}
