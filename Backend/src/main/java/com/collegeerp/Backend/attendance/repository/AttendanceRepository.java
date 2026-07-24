package com.collegeerp.Backend.attendance.repository;

import com.collegeerp.Backend.attendance.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AttendanceRepository
        extends JpaRepository<Attendance, Long> {

    boolean existsByEnrollmentIdAndAttendanceDate(
            Long enrollmentId,
            java.time.LocalDate attendanceDate);

    /**
     * Fetch-joins enrollment -> subject so the student self-service attendance endpoint can
     * compute overall + per-subject percentages without N+1 lazy-loading.
     */
    @Query("""
           SELECT a
           FROM Attendance a
           JOIN FETCH a.enrollment e
           JOIN FETCH e.subject
           WHERE e.student.id = :studentId
           ORDER BY a.attendanceDate DESC
           """)
    java.util.List<Attendance> findByStudentId(Long studentId);

    /**
     * Fetch-joins enrollment -> subject so the teacher self-service dashboard can compute
     * "which of my subjects still need attendance marked today" and a recent attendance
     * trend without N+1 lazy-loading. Mirrors {@link #findByStudentId}.
     */
    @Query("""
           SELECT a
           FROM Attendance a
           JOIN FETCH a.enrollment e
           JOIN FETCH e.subject s
           WHERE s.teacher.id = :teacherId
           ORDER BY a.attendanceDate DESC
           """)
    java.util.List<Attendance> findBySubjectTeacherId(Long teacherId);

}