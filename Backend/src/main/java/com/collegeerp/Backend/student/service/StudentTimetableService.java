package com.collegeerp.Backend.student.service;

import com.collegeerp.Backend.enrollment.entity.Enrollment;
import com.collegeerp.Backend.enrollment.repository.EnrollmentRepository;
import com.collegeerp.Backend.student.dto.StudentTimetableResponse;
import com.collegeerp.Backend.student.dto.TimetableEntryResponse;
import com.collegeerp.Backend.subject.entity.Subject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>PLACEHOLDER / MOCK DATA.</b>
 * <p>
 * There is no {@code Timetable} or {@code Period} entity anywhere in this schema - the ERP has
 * no concept of scheduled weekly class periods (only {@code ExamSchedule} for exams). Building
 * a real timetable requires a new data model (day of week, period number, room, recurring
 * schedule) that is out of scope for "implement the Student module using the existing
 * architecture" - it would be a new module/entity, which this session was explicitly told not
 * to introduce for other areas (Teacher/Admin) and which doesn't exist for this area either.
 * <p>
 * To still return a structurally useful, stable response, this service deterministically
 * distributes the student's real enrolled subjects across a fixed Mon-Fri / 4-slots-per-day
 * grid. The subject/teacher names are real; the day, time, and room assignment are NOT read
 * from any actual schedule and must not be treated as authoritative. The response is flagged
 * with {@code placeholder=true} and a human-readable {@code note} for exactly this reason.
 * Replace this class's body with a real repository-backed query once a Timetable module exists.
 */
@Service
@Transactional(readOnly = true)
public class StudentTimetableService {

    private static final List<String> DAYS = List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY");
    private static final List<String[]> SLOTS = List.of(
            new String[]{"09:00", "10:00"},
            new String[]{"10:15", "11:15"},
            new String[]{"11:30", "12:30"},
            new String[]{"13:30", "14:30"}
    );

    private final EnrollmentRepository enrollmentRepository;

    public StudentTimetableService(EnrollmentRepository enrollmentRepository) {
        this.enrollmentRepository = enrollmentRepository;
    }

    public StudentTimetableResponse getTimetable(Long studentId) {

        List<Subject> subjects = enrollmentRepository.findByStudentIdWithDetails(studentId)
                .stream()
                .map(Enrollment::getSubject)
                .distinct()
                .toList();

        Map<String, List<TimetableEntryResponse>> schedule = new LinkedHashMap<>();
        for (String day : DAYS) {
            schedule.put(day, new java.util.ArrayList<>());
        }

        int slotCursor = 0;
        for (Subject subject : subjects) {
            int dayIndex = slotCursor % DAYS.size();
            int slotIndex = (slotCursor / DAYS.size()) % SLOTS.size();
            String[] slot = SLOTS.get(slotIndex);

            schedule.get(DAYS.get(dayIndex)).add(TimetableEntryResponse.builder()
                    .startTime(slot[0])
                    .endTime(slot[1])
                    .subjectId(subject.getId())
                    .subjectName(subject.getSubjectName())
                    .teacherName(subject.getTeacher().getFirstName() + " " + subject.getTeacher().getLastName())
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
