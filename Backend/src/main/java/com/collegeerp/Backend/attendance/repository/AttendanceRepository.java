package com.collegeerp.Backend.attendance.repository;

import com.collegeerp.Backend.attendance.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AttendanceRepository
        extends JpaRepository<Attendance, Long> {

    boolean existsByClassEnrollmentIdAndAttendanceDate(Long classEnrollmentId, java.time.LocalDate attendanceDate);

    boolean existsByClassEnrollmentIdAndAttendanceDateAndIdNot(Long classEnrollmentId, java.time.LocalDate attendanceDate, Long id);

    boolean existsByEnrollmentIdAndAttendanceDate(
            Long enrollmentId,
            java.time.LocalDate attendanceDate);

    boolean existsByEnrollmentIdAndAttendanceDateAndIdNot(
            Long enrollmentId,
            java.time.LocalDate attendanceDate,
            Long id);

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

    /** Fetches class-based attendance with the complete class-subject relationship. */
    @Query("""
           SELECT a
           FROM Attendance a
           JOIN FETCH a.classEnrollment ce
           JOIN FETCH ce.student st
           JOIN FETCH ce.classSubject cs
           LEFT JOIN FETCH cs.subject
           JOIN FETCH cs.schoolClass
           JOIN FETCH cs.teacher
           WHERE st.id = :studentId
           ORDER BY a.attendanceDate DESC
           """)
    java.util.List<Attendance> findClassAttendanceByStudentId(Long studentId);

    /** Legacy attendance retained only for records that have not yet been migrated. */
    @Query("""
           SELECT a
           FROM Attendance a
           JOIN FETCH a.enrollment e
           JOIN FETCH e.student st
           JOIN FETCH e.subject s
           WHERE st.id = :studentId
           ORDER BY a.attendanceDate DESC
           """)
    java.util.List<Attendance> findLegacyAttendanceByStudentId(Long studentId);


}