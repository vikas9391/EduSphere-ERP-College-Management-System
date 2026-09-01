package com.collegeerp.Backend.schoolclass.repository;

import com.collegeerp.Backend.schoolclass.entity.ClassSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ClassSubjectRepository extends JpaRepository<ClassSubject, Long> {

    boolean existsBySchoolClassIdAndSubjectCode(Long schoolClassId, String subjectCode);

    int countBySchoolClassId(Long schoolClassId);

    @Query("""
            SELECT s FROM ClassSubject s
            JOIN FETCH s.teacher
            LEFT JOIN FETCH s.subject
            WHERE s.schoolClass.id = :schoolClassId
            ORDER BY s.id
            """)
    List<ClassSubject> findAllByClassId(Long schoolClassId);

    @Query("""
            SELECT s FROM ClassSubject s
            JOIN FETCH s.teacher
            JOIN FETCH s.schoolClass
            LEFT JOIN FETCH s.subject
            WHERE s.id = :id
            """)
    Optional<ClassSubject> findByIdWithRelations(Long id);

    /** Every class-subject linked to a given formal curriculum Subject. */
    @Query("""
            SELECT s FROM ClassSubject s
            JOIN FETCH s.teacher
            JOIN FETCH s.schoolClass
            LEFT JOIN FETCH s.subject
            WHERE s.subject.id = :subjectId
            """)
    List<ClassSubject> findBySubjectId(Long subjectId);
}
