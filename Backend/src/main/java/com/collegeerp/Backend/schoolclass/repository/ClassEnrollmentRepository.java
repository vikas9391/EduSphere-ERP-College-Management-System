package com.collegeerp.Backend.schoolclass.repository;

import com.collegeerp.Backend.schoolclass.entity.ClassEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ClassEnrollmentRepository extends JpaRepository<ClassEnrollment, Long> {

    boolean existsByClassSubjectIdAndStudentId(Long classSubjectId, Long studentId);

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
            JOIN FETCH e.classSubject cs
            JOIN FETCH cs.teacher
            WHERE e.student.id = :studentId AND cs.schoolClass.id = :schoolClassId
            """)
    List<ClassEnrollment> findAllByStudentIdAndClassId(Long studentId, Long schoolClassId);
}
