package com.collegeerp.Backend.enrollment.repository;

import com.collegeerp.Backend.enrollment.entity.Enrollment;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    boolean existsByStudentIdAndSubjectIdAndAcademicYearAndSemester(
            Long studentId, Long subjectId, String academicYear, Integer semester);

    boolean existsByStudentIdAndSubjectIdAndAcademicYearAndSemesterAndIdNot(
            Long studentId, Long subjectId, String academicYear, Integer semester, Long id);

    boolean existsByStudentIdAndSubjectId(Long studentId, Long subjectId);

    List<Enrollment> findByStudentId(Long studentId);

    @Query("""
           SELECT e
           FROM Enrollment e
           JOIN FETCH e.subject s
           JOIN FETCH s.course c
           LEFT JOIN FETCH c.department
           JOIN FETCH s.teacher
           WHERE e.student.id = :studentId
           ORDER BY e.enrollmentDate DESC
           """)
    List<Enrollment> findByStudentIdWithDetails(Long studentId);

    @Query("""
           SELECT e
           FROM Enrollment e
           JOIN FETCH e.student
           JOIN FETCH e.subject s
           JOIN FETCH s.course c
           LEFT JOIN FETCH c.department
           JOIN FETCH s.teacher
           WHERE s.teacher.id = :teacherId
           ORDER BY s.subjectCode ASC, e.student.rollNumber ASC
           """)
    List<Enrollment> findBySubjectTeacherIdWithDetails(Long teacherId);

    @Query("""
           SELECT e
           FROM Enrollment e
           JOIN FETCH e.student
           WHERE e.subject.id = :subjectId
           ORDER BY e.student.rollNumber ASC
           """)
    List<Enrollment> findBySubjectIdWithStudent(Long subjectId);
}
