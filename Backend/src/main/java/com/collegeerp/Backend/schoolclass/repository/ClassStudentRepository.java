package com.collegeerp.Backend.schoolclass.repository;

import com.collegeerp.Backend.schoolclass.entity.ClassStudent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ClassStudentRepository extends JpaRepository<ClassStudent, Long> {

    boolean existsBySchoolClassIdAndStudentId(Long schoolClassId, Long studentId);

    Optional<ClassStudent> findBySchoolClassIdAndStudentId(Long schoolClassId, Long studentId);

    @Query("""
            SELECT cs FROM ClassStudent cs
            JOIN FETCH cs.student
            WHERE cs.schoolClass.id = :schoolClassId
            ORDER BY cs.addedAt
            """)
    List<ClassStudent> findAllByClassId(Long schoolClassId);

    @Query("""
            SELECT cs FROM ClassStudent cs
            JOIN FETCH cs.schoolClass sc
            JOIN FETCH sc.teacher
            WHERE cs.student.id = :studentId
            ORDER BY cs.addedAt DESC
            """)
    List<ClassStudent> findAllByStudentId(Long studentId);
}
