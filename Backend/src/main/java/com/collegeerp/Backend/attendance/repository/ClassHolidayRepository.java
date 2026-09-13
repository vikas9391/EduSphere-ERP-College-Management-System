package com.collegeerp.Backend.attendance.repository;

import com.collegeerp.Backend.attendance.entity.ClassHoliday;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ClassHolidayRepository extends JpaRepository<ClassHoliday, Long> {
    List<ClassHoliday> findAllBySchoolClassIdOrderByHolidayDateDesc(Long classId);
    Optional<ClassHoliday> findBySchoolClassIdAndHolidayDate(Long classId, LocalDate holidayDate);
    boolean existsBySchoolClassIdAndHolidayDate(Long classId, LocalDate holidayDate);
}
