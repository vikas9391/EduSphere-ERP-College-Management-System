package com.collegeerp.Backend.fee.repository;
import com.collegeerp.Backend.fee.entity.StudentFee;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface StudentFeeRepository extends JpaRepository<StudentFee, Long> {
    List<StudentFee> findByStudentIdOrderByAssignedAtDesc(Long studentId);
    Optional<StudentFee> findByStudentIdAndFeeStructureId(Long studentId, Long feeStructureId);
}
