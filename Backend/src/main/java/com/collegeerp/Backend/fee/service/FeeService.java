package com.collegeerp.Backend.fee.service;

import com.collegeerp.Backend.common.exception.DuplicateResourceException;
import com.collegeerp.Backend.common.exception.ResourceNotFoundException;
import com.collegeerp.Backend.course.entity.Course;
import com.collegeerp.Backend.course.repository.CourseRepository;
import com.collegeerp.Backend.fee.dto.*;
import com.collegeerp.Backend.fee.entity.*;
import com.collegeerp.Backend.fee.repository.*;
import com.collegeerp.Backend.student.entity.Student;
import com.collegeerp.Backend.student.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class FeeService {
    private final FeeStructureRepository structureRepository;
    private final StudentFeeRepository studentFeeRepository;
    private final FeePaymentRepository paymentRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;

    public FeeService(FeeStructureRepository structureRepository, StudentFeeRepository studentFeeRepository,
                      FeePaymentRepository paymentRepository, StudentRepository studentRepository,
                      CourseRepository courseRepository) {
        this.structureRepository = structureRepository;
        this.studentFeeRepository = studentFeeRepository;
        this.paymentRepository = paymentRepository;
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
    }

    @Transactional
    public FeeStructure createStructure(FeeStructureRequest r) {
        Course course = r.courseId() == null ? null : courseRepository.findById(r.courseId())
                .orElseThrow(() -> ResourceNotFoundException.of("Course", r.courseId()));
        return structureRepository.save(FeeStructure.builder()
                .course(course).academicYear(r.academicYear().trim()).semester(r.semester())
                .name(r.name().trim()).tuitionFee(r.tuitionFee()).examinationFee(r.examinationFee())
                .libraryFee(r.libraryFee()).otherFee(r.otherFee()).dueDate(r.dueDate())
                .createdAt(LocalDateTime.now()).build());
    }

    public List<FeeStructure> getStructures() { return structureRepository.findAll(); }

    @Transactional
    public FeeResponse assign(AssignFeeRequest r) {
        if (studentFeeRepository.findByStudentIdAndFeeStructureId(r.studentId(), r.feeStructureId()).isPresent())
            throw new DuplicateResourceException("This fee is already assigned to the student");
        Student student = studentRepository.findById(r.studentId())
                .orElseThrow(() -> ResourceNotFoundException.of("Student", r.studentId()));
        FeeStructure fs = structureRepository.findById(r.feeStructureId())
                .orElseThrow(() -> ResourceNotFoundException.of("Fee structure", r.feeStructureId()));
        BigDecimal gross = fs.getTuitionFee().add(fs.getExaminationFee()).add(fs.getLibraryFee()).add(fs.getOtherFee());
        if (r.discount().compareTo(gross) > 0) throw new IllegalArgumentException("Discount cannot exceed total fee");
        StudentFee fee = studentFeeRepository.save(StudentFee.builder().student(student).feeStructure(fs)
                .discount(r.discount()).totalAmount(gross.subtract(r.discount())).amountPaid(BigDecimal.ZERO)
                .status(StudentFee.Status.PENDING).assignedAt(LocalDateTime.now()).build());
        return toResponse(fee);
    }

    public List<FeeResponse> getAllFees() { return studentFeeRepository.findAllByOrderByAssignedAtDesc().stream().map(this::toResponse).toList(); }

    public List<FeeResponse> getStudentFees(Long studentId) {
        return studentFeeRepository.findByStudentIdOrderByAssignedAtDesc(studentId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public PaymentResponse recordPayment(Long studentFeeId, PaymentRequest r) {
        StudentFee fee = studentFeeRepository.findById(studentFeeId)
                .orElseThrow(() -> ResourceNotFoundException.of("Student fee", studentFeeId));
        BigDecimal balance = fee.getTotalAmount().subtract(fee.getAmountPaid());
        if (r.amount().compareTo(balance) > 0) throw new IllegalArgumentException("Payment exceeds outstanding balance");
        BigDecimal paid = fee.getAmountPaid().add(r.amount());
        fee.setAmountPaid(paid);
        fee.setStatus(paid.compareTo(fee.getTotalAmount()) >= 0 ? StudentFee.Status.PAID : StudentFee.Status.PARTIALLY_PAID);
        studentFeeRepository.save(fee);
        String receipt = "RCP-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        FeePayment payment = paymentRepository.save(FeePayment.builder().studentFee(fee).amount(r.amount())
                .paymentMethod(r.paymentMethod()).transactionReference(r.transactionReference())
                .receiptNumber(receipt).paidAt(LocalDateTime.now()).notes(r.notes()).build());
        return toPaymentResponse(payment);
    }

    public List<PaymentResponse> getPayments(Long studentFeeId) {
        return paymentRepository.findByStudentFeeIdOrderByPaidAtDesc(studentFeeId).stream().map(this::toPaymentResponse).toList();
    }

    public FeeResponse getFee(Long id) {
        return toResponse(studentFeeRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Student fee", id)));
    }

    private FeeResponse toResponse(StudentFee f) {
        BigDecimal balance = f.getTotalAmount().subtract(f.getAmountPaid());
        String status = f.getStatus().name();
        if (balance.signum() > 0 && f.getFeeStructure().getDueDate() != null && f.getFeeStructure().getDueDate().isBefore(LocalDate.now()))
            status = "OVERDUE";
        return new FeeResponse(f.getId(), f.getStudent().getId(),
                f.getStudent().getFirstName() + (f.getStudent().getLastName() == null ? "" : " " + f.getStudent().getLastName()),
                f.getFeeStructure().getId(), f.getFeeStructure().getName(), f.getFeeStructure().getAcademicYear(),
                f.getFeeStructure().getSemester(), f.getDiscount(), f.getTotalAmount(), f.getAmountPaid(),
                balance, status, f.getFeeStructure().getDueDate());
    }

    private PaymentResponse toPaymentResponse(FeePayment p) {
        return new PaymentResponse(p.getId(), p.getStudentFee().getId(), p.getAmount(), p.getPaymentMethod(),
                p.getTransactionReference(), p.getReceiptNumber(), p.getPaidAt(), p.getNotes());
    }
}
