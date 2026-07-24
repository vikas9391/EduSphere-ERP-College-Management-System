package com.collegeerp.Backend.schoolclass.repository;

import com.collegeerp.Backend.schoolclass.entity.SchoolClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SchoolClassRepository extends JpaRepository<SchoolClass, Long> {

    @Query("SELECT c FROM SchoolClass c JOIN FETCH c.teacher WHERE c.teacher.id = :teacherId ORDER BY c.id DESC")
    List<SchoolClass> findAllByTeacherId(Long teacherId);

    @Query("SELECT c FROM SchoolClass c JOIN FETCH c.teacher WHERE c.id = :id")
    Optional<SchoolClass> findByIdWithTeacher(Long id);
}
