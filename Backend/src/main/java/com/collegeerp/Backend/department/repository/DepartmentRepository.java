package com.collegeerp.Backend.department.repository;

import com.collegeerp.Backend.department.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    boolean existsByCode(String code);
}