package com.collegeerp.Backend.timetable.repository;

import com.collegeerp.Backend.timetable.entity.TimetableEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

public interface TimetableEntryRepository extends JpaRepository<TimetableEntry, Long> {

    boolean existsByClassSubjectIdAndDayOfWeekAndStartTime(
            Long classSubjectId, DayOfWeek dayOfWeek, LocalTime startTime);

    boolean existsByClassSubjectIdAndDayOfWeekAndStartTimeAndIdNot(
            Long classSubjectId, DayOfWeek dayOfWeek, LocalTime startTime, Long id);

    @Query("""
            SELECT t FROM TimetableEntry t
            JOIN FETCH t.classSubject cs
            JOIN FETCH cs.schoolClass
            JOIN FETCH cs.teacher
            LEFT JOIN FETCH cs.subject
            WHERE cs.id = :classSubjectId
            ORDER BY t.dayOfWeek, t.startTime
            """)
    List<TimetableEntry> findAllByClassSubjectIdWithDetails(Long classSubjectId);

    @Query("""
            SELECT DISTINCT t FROM TimetableEntry t
            JOIN FETCH t.classSubject cs
            JOIN FETCH cs.schoolClass
            JOIN FETCH cs.teacher
            LEFT JOIN FETCH cs.subject
            JOIN ClassEnrollment ce ON ce.classSubject.id = cs.id
            WHERE ce.student.id = :studentId
            ORDER BY t.dayOfWeek, t.startTime
            """)
    List<TimetableEntry> findAllForStudent(Long studentId);

    @Query("""
            SELECT t FROM TimetableEntry t
            JOIN FETCH t.classSubject cs
            JOIN FETCH cs.schoolClass
            JOIN FETCH cs.teacher
            LEFT JOIN FETCH cs.subject
            WHERE cs.teacher.id = :teacherId
            ORDER BY t.dayOfWeek, t.startTime
            """)
    List<TimetableEntry> findAllForTeacher(Long teacherId);

    @Query("""
            SELECT t FROM TimetableEntry t
            JOIN FETCH t.classSubject cs
            JOIN FETCH cs.schoolClass
            JOIN FETCH cs.teacher
            LEFT JOIN FETCH cs.subject
            WHERE cs.teacher.id = :teacherId AND t.dayOfWeek = :dayOfWeek
            ORDER BY t.startTime
            """)
    List<TimetableEntry> findForTeacherAndDay(Long teacherId, DayOfWeek dayOfWeek);
}
