package com.collegeerp.Backend.timetable.repository;

import com.collegeerp.Backend.timetable.entity.TimetableEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface TimetableEntryRepository extends JpaRepository<TimetableEntry, Long> {

    @Query("""
            SELECT t FROM TimetableEntry t
            JOIN FETCH t.classSubject cs
            JOIN FETCH cs.schoolClass
            JOIN FETCH cs.teacher
            LEFT JOIN FETCH cs.subject
            WHERE t.id = :id
            """)
    Optional<TimetableEntry> findByIdWithDetails(Long id);

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
            SELECT t FROM TimetableEntry t
            JOIN FETCH t.classSubject cs
            JOIN FETCH cs.schoolClass
            JOIN FETCH cs.teacher
            LEFT JOIN FETCH cs.subject
            WHERE cs.id IN (
                SELECT ce.classSubject.id FROM ClassEnrollment ce WHERE ce.student.id = :studentId
            )
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

    @Query("""
            SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END
            FROM TimetableEntry t
            WHERE t.classSubject.schoolClass.id = :schoolClassId
              AND t.dayOfWeek = :dayOfWeek
              AND t.id <> :excludeId
              AND t.startTime < :endTime
              AND t.endTime > :startTime
            """)
    boolean hasClassConflict(
            Long schoolClassId,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            Long excludeId);

    @Query("""
            SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END
            FROM TimetableEntry t
            WHERE t.classSubject.teacher.id = :teacherId
              AND t.dayOfWeek = :dayOfWeek
              AND t.id <> :excludeId
              AND t.startTime < :endTime
              AND t.endTime > :startTime
            """)
    boolean hasTeacherConflict(
            Long teacherId,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            Long excludeId);
}
