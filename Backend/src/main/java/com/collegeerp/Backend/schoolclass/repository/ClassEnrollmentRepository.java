package com.collegeerp.Backend.schoolclass.repository;

import com.collegeerp.Backend.schoolclass.entity.ClassEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ClassEnrollmentRepository extends JpaRepository<ClassEnrollment, Long> {

    boolean existsByClassSubjectIdAndStudentId(Long classSubjectId, Long studentId);

    boolean existsByStudentIdAndClassSubjectTeacherId(Long studentId, Long teacherId);

    Optional<ClassEnrollment> findByClassSubjectIdAndStudentId(Long classSubjectId, Long studentId);

    @Query("""
            SELECT e FROM ClassEnrollment e
            JOIN FETCH e.student
            WHERE e.classSubject.id = :classSubjectId
            ORDER BY e.enrolledAt
            """)
    List<ClassEnrollment> findAllByClassSubjectId(Long classSubjectId);

    @Query("""
            SELECT e FROM ClassEnrollment e
            JOIN FETCH e.student
            JOIN FETCH e.classSubject cs
            LEFT JOIN FETCH cs.subject
            JOIN FETCH cs.schoolClass
            JOIN FETCH cs.teacher t
            WHERE t.id = :teacherId
            ORDER BY cs.schoolClass.academicYear DESC, cs.schoolClass.semester, cs.id, e.student.id
            """)
    List<ClassEnrollment> findAllByTeacherId(Long teacherId);

    @Query("""
            SELECT e FROM ClassEnrollment e
            JOIN FETCH e.classSubject cs
            JOIN FETCH cs.teacher
            WHERE e.student.id = :studentId AND cs.schoolClass.id = :schoolClassId
            """)
    List<ClassEnrollment> findAllByStudentIdAndClassId(Long studentId, Long schoolClassId);

    @Query("""
            SELECT e FROM ClassEnrollment e
            JOIN FETCH e.classSubject cs
            LEFT JOIN FETCH cs.subject
            JOIN FETCH cs.schoolClass
            JOIN FETCH cs.teacher
            WHERE e.student.id = :studentId
            ORDER BY cs.schoolClass.academicYear DESC, cs.schoolClass.semester, cs.id
            """)
    List<ClassEnrollment> findAllByStudentId(Long studentId);
}
