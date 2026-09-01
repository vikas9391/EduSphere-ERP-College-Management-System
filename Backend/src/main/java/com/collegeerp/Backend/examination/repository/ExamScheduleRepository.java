package com.collegeerp.Backend.examination.repository;

import com.collegeerp.Backend.examination.entity.ExamSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ExamScheduleRepository extends JpaRepository<ExamSchedule, Long> {

    boolean existsByExamIdAndSubjectIdAndClassSubjectIsNull(Long examId, Long subjectId);

    boolean existsByExamIdAndClassSubjectId(Long examId, Long classSubjectId);

    @Query("""
           SELECT es
           FROM ExamSchedule es
           JOIN FETCH es.exam
           JOIN FETCH es.subject
           LEFT JOIN FETCH es.classSubject cs
           LEFT JOIN FETCH cs.schoolClass
           LEFT JOIN FETCH es.invigilator
           WHERE es.exam.id = :examId
           """)
    List<ExamSchedule> findByExamIdWithDetails(Long examId);

    @Query("""
           SELECT es
           FROM ExamSchedule es
           JOIN FETCH es.exam
           JOIN FETCH es.subject
           LEFT JOIN FETCH es.classSubject cs
           LEFT JOIN FETCH cs.schoolClass
           LEFT JOIN FETCH es.invigilator
           WHERE es.id = :id
           """)
    Optional<ExamSchedule> findByIdWithDetails(Long id);

    /**
     * Current class-scoped schedules win. Subject-only schedules are legacy compatibility rows.
     */
    @Query("""
           SELECT DISTINCT es
           FROM ExamSchedule es
           JOIN FETCH es.exam
           JOIN FETCH es.subject
           LEFT JOIN FETCH es.classSubject cs
           WHERE ((cs.id IN :classSubjectIds)
               OR (cs IS NULL AND es.subject.id IN :subjectIds))
             AND es.examDate >= :fromDate
           ORDER BY es.examDate ASC, es.startTime ASC
           """)
    List<ExamSchedule> findUpcomingForStudent(
            List<Long> classSubjectIds,
            List<Long> subjectIds,
            java.time.LocalDate fromDate);

    /** Legacy compatibility query retained for older callers during migration. */
    @Query("""
           SELECT es
           FROM ExamSchedule es
           JOIN FETCH es.exam
           JOIN FETCH es.subject
           WHERE es.subject.id IN :subjectIds
             AND es.examDate >= :fromDate
           ORDER BY es.examDate ASC, es.startTime ASC
           """)
    List<ExamSchedule> findUpcomingBySubjectIds(List<Long> subjectIds, java.time.LocalDate fromDate);
}
