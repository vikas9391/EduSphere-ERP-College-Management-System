package com.collegeerp.Backend.fee.service;

import com.collegeerp.Backend.common.exception.DuplicateResourceException;
import com.collegeerp.Backend.course.repository.CourseRepository;
import com.collegeerp.Backend.fee.dto.AssignFeeRequest;
import com.collegeerp.Backend.fee.dto.PaymentRequest;
import com.collegeerp.Backend.fee.entity.FeePayment.PaymentMethod;
import com.collegeerp.Backend.fee.entity.FeeStructure;
import com.collegeerp.Backend.fee.repository.FeePaymentRepository;
import com.collegeerp.Backend.fee.repository.FeeStructureRepository;
import com.collegeerp.Backend.fee.repository.StudentFeeRepository;
import com.collegeerp.Backend.student.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeeServiceTest {
    @Mock FeeStructureRepository structureRepository;
    @Mock StudentFeeRepository studentFeeRepository;
    @Mock FeePaymentRepository paymentRepository;
    @Mock StudentRepository studentRepository;
    @Mock CourseRepository courseRepository;

    @Test
    void rejectsDuplicateFeeAssignment() {
        when(studentFeeRepository.findByStudentIdAndFeeStructureId(1L, 2L)).thenReturn(Optional.of(new com.collegeerp.Backend.fee.entity.StudentFee()));
        FeeService service = service();
        assertThrows(DuplicateResourceException.class, () -> service.assign(new AssignFeeRequest(1L,2L,BigDecimal.ZERO)));
        verify(studentRepository, never()).findById(anyLong());
    }

    @Test
    void rejectsDiscountGreaterThanGrossFee() {
        FeeStructure fs = FeeStructure.builder().id(2L).name("Semester Fee").academicYear("2026-27")
                .tuitionFee(new BigDecimal("50000")).examinationFee(new BigDecimal("5000"))
                .libraryFee(BigDecimal.ZERO).otherFee(BigDecimal.ZERO).build();
        when(studentFeeRepository.findByStudentIdAndFeeStructureId(1L,2L)).thenReturn(Optional.empty());
        when(studentRepository.findById(1L)).thenReturn(Optional.of(com.collegeerp.Backend.student.entity.Student.builder().id(1L).build()));
        when(structureRepository.findById(2L)).thenReturn(Optional.of(fs));
        FeeService service = service();
        assertThrows(IllegalArgumentException.class, () -> service.assign(new AssignFeeRequest(1L,2L,new BigDecimal("60000"))));
        verify(studentFeeRepository, never()).save(any());
    }

    @Test
    void rejectsPaymentAboveOutstandingBalance() {
        var fee = com.collegeerp.Backend.fee.entity.StudentFee.builder()
                .id(9L).totalAmount(new BigDecimal("10000")).amountPaid(new BigDecimal("7500"))
                .feeStructure(FeeStructure.builder().id(2L).name("Semester Fee").academicYear("2026-27").build()).build();
        when(studentFeeRepository.findById(9L)).thenReturn(Optional.of(fee));
        FeeService service = service();
        assertThrows(IllegalArgumentException.class, () -> service.recordPayment(9L,new PaymentRequest(new BigDecimal("2500.01"),PaymentMethod.UPI,"TXN","")));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void fullPaymentMarksFeePaidAndCreatesReceipt() {
        var fs=FeeStructure.builder().id(2L).name("Semester Fee").academicYear("2026-27").build();
        var fee=com.collegeerp.Backend.fee.entity.StudentFee.builder().id(9L).totalAmount(new BigDecimal("10000")).amountPaid(BigDecimal.ZERO).status(com.collegeerp.Backend.fee.entity.StudentFee.Status.PENDING).feeStructure(fs).build();
        when(studentFeeRepository.findById(9L)).thenReturn(Optional.of(fee));
        when(studentFeeRepository.save(any())).thenReturn(fee);
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var result=service().recordPayment(9L,new PaymentRequest(new BigDecimal("10000"),PaymentMethod.UPI,"TXN-1",""));
        assertEquals(new BigDecimal("10000"),fee.getAmountPaid());
        assertEquals(com.collegeerp.Backend.fee.entity.StudentFee.Status.PAID,fee.getStatus());
        assertTrue(result.receiptNumber().startsWith("RCP-"));
    }

    private FeeService service(){return new FeeService(structureRepository,studentFeeRepository,paymentRepository,studentRepository,courseRepository);}
}
