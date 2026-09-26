package com.collegeerp.Backend.fee.repository;
import com.collegeerp.Backend.fee.entity.FeePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface FeePaymentRepository extends JpaRepository<FeePayment, Long> {
    List<FeePayment> findByStudentFeeIdOrderByPaidAtDesc(Long studentFeeId);
}
