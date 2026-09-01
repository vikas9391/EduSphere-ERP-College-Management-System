package com.collegeerp.Backend.attendance.repository;

import com.collegeerp.Backend.attendance.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    boolean existsByClassEnrollmentIdAndAttendanceDate(Long classEnrollmentId, java.time.LocalDate attendanceDate);

    boolean existsByClassEnrollmentIdAndAttendanceDateAndIdNot(Long classEnrollmentId, java.time.LocalDate attendanceDate, Long id);

    boolean existsByEnrollmentIdAndAttendanceDate(Long enrollmentId, java.time.LocalDate attendanceDate);

    boolean existsByEnrollmentIdAndAttendanceDateAndIdNot(Long enrollmentId, java.time.LocalDate attendanceDate, Long id);

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
    List<Attendance> findClassAttendanceByStudentId(Long studentId);

    @Query("""
           SELECT a
           FROM Attendance a
           JOIN FETCH a.enrollment e
           JOIN FETCH e.student st
           JOIN FETCH e.subject s
           WHERE st.id = :studentId
           ORDER BY a.attendanceDate DESC
           """)
    List<Attendance> findLegacyAttendanceByStudentId(Long studentId);

    /** Class-based attendance for the exact class subjects taught by a teacher. */
    @Query("""
           SELECT a
           FROM Attendance a
           JOIN FETCH a.classEnrollment ce
           JOIN FETCH ce.student st
           JOIN FETCH ce.classSubject cs
           LEFT JOIN FETCH cs.subject
           JOIN FETCH cs.schoolClass
           JOIN FETCH cs.teacher t
           WHERE t.id = :teacherId
           ORDER BY a.attendanceDate DESC
           """)
    List<Attendance> findClassAttendanceByTeacherId(Long teacherId);

    /** Legacy teacher query retained for compatibility with unmigrated rows. */
    @Query("""
           SELECT a
           FROM Attendance a
           JOIN FETCH a.enrollment e
           JOIN FETCH e.student st
           JOIN FETCH e.subject s
           WHERE s.teacher.id = :teacherId
           ORDER BY a.attendanceDate DESC
           """)
    List<Attendance> findBySubjectTeacherId(Long teacherId);
}
