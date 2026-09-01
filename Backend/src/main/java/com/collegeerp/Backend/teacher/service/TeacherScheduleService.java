package com.collegeerp.Backend.teacher.service;

import com.collegeerp.Backend.teacher.dto.TeacherScheduleEntryResponse;
import com.collegeerp.Backend.timetable.repository.TimetableEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** Teacher schedule backed by real ClassSubject timetable entries. */
@Service
@Transactional(readOnly = true)
public class TeacherScheduleService {

    private final TimetableEntryRepository timetableEntryRepository;

    public TeacherScheduleService(TimetableEntryRepository timetableEntryRepository) {
        this.timetableEntryRepository = timetableEntryRepository;
    }

    public List<TeacherScheduleEntryResponse> getTodaysSchedule(Long teacherId) {
        return timetableEntryRepository.findForTeacherAndDay(teacherId, LocalDate.now().getDayOfWeek())
                .stream()
                .map(entry -> {
                    var cs = entry.getClassSubject();
                    return TeacherScheduleEntryResponse.builder()
                            .subjectId(cs.getSubject() != null ? cs.getSubject().getId() : cs.getId())
                            .subjectName(cs.getSubjectName())
                            .startTime(entry.getStartTime().toString())
                            .endTime(entry.getEndTime().toString())
                            .room(entry.getRoom() != null ? entry.getRoom() : "TBD")
                            .build();
                })
                .toList();
    }
}
