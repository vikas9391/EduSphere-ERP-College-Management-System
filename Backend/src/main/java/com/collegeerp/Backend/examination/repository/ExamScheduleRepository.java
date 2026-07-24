package com.collegeerp.Backend.examination.repository;

import com.collegeerp.Backend.examination.entity.ExamSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ExamScheduleRepository extends JpaRepository<ExamSchedule, Long> {

    boolean existsByExamIdAndSubjectId(Long examId, Long subjectId);

    @Query("""
           SELECT es
           FROM ExamSchedule es
           JOIN FETCH es.exam
           JOIN FETCH es.subject
           LEFT JOIN FETCH es.invigilator
           WHERE es.exam.id = :examId
           """)
    List<ExamSchedule> findByExamIdWithDetails(Long examId);

    @Query("""
           SELECT es
           FROM ExamSchedule es
           JOIN FETCH es.exam
           JOIN FETCH es.subject
           LEFT JOIN FETCH es.invigilator
           WHERE es.id = :id
           """)
    Optional<ExamSchedule> findByIdWithDetails(Long id);

    /**
     * Used by the student self-service dashboard/timetable views: all exam schedules for
     * a given set of subjects (the student's enrolled subjects) on or after {@code fromDate}.
     */
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
