package com.collegeerp.Backend.attendance.repository;

import com.collegeerp.Backend.attendance.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    boolean existsByClassEnrollmentIdAndAttendanceDate(Long classEnrollmentId, LocalDate attendanceDate);

    boolean existsByClassEnrollmentIdAndAttendanceDateAndIdNot(Long classEnrollmentId, LocalDate attendanceDate, Long id);

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
}
