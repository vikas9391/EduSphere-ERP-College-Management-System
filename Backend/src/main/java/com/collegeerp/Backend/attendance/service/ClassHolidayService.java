package com.collegeerp.Backend.attendance.service;

import com.collegeerp.Backend.attendance.dto.ClassHolidayRequest;
import com.collegeerp.Backend.attendance.dto.ClassHolidayResponse;
import com.collegeerp.Backend.attendance.entity.ClassHoliday;
import com.collegeerp.Backend.attendance.repository.AttendanceRepository;
import com.collegeerp.Backend.attendance.repository.ClassHolidayRepository;
import com.collegeerp.Backend.common.exception.BadRequestException;
import com.collegeerp.Backend.common.exception.DuplicateResourceException;
import com.collegeerp.Backend.common.exception.ResourceNotFoundException;
import com.collegeerp.Backend.schoolclass.entity.SchoolClass;
import com.collegeerp.Backend.schoolclass.repository.ClassEnrollmentRepository;
import com.collegeerp.Backend.schoolclass.repository.ClassSubjectRepository;
import com.collegeerp.Backend.schoolclass.repository.SchoolClassRepository;
import com.collegeerp.Backend.security.UserPrincipal;
import com.collegeerp.Backend.student.service.StudentIdentityService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ClassHolidayService {
    private final ClassHolidayRepository holidayRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final StudentIdentityService studentIdentityService;

    public ClassHolidayService(ClassHolidayRepository holidayRepository,
                                SchoolClassRepository schoolClassRepository,
                                ClassSubjectRepository classSubjectRepository,
                                ClassEnrollmentRepository classEnrollmentRepository,
                                AttendanceRepository attendanceRepository,
                                StudentIdentityService studentIdentityService) {
        this.holidayRepository = holidayRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.classSubjectRepository = classSubjectRepository;
        this.classEnrollmentRepository = classEnrollmentRepository;
        this.attendanceRepository = attendanceRepository;
        this.studentIdentityService = studentIdentityService;
    }

    @Transactional(readOnly = true)
    public List<ClassHolidayResponse> list(Long classId, UserPrincipal principal) {
        SchoolClass schoolClass = getClass(classId);
        requireCanView(schoolClass, principal);
        return holidayRepository.findAllBySchoolClassIdOrderByHolidayDateDesc(classId).stream().map(this::map).toList();
    }

    public ClassHolidayResponse create(ClassHolidayRequest request, UserPrincipal principal) {
        if (request.getClassId() == null || request.getHolidayDate() == null) throw new BadRequestException("Class and holiday date are required");
        SchoolClass schoolClass = getClass(request.getClassId());
        requireCanManage(schoolClass, principal);
        if (holidayRepository.existsBySchoolClassIdAndHolidayDate(schoolClass.getId(), request.getHolidayDate())) {
            throw new DuplicateResourceException("A holiday already exists for this class on " + request.getHolidayDate());
        }
        ClassHoliday holiday = ClassHoliday.builder()
                .schoolClass(schoolClass)
                .holidayDate(request.getHolidayDate())
                .reason(normalize(request.getReason()))
                .createdAt(LocalDateTime.now())
                .build();
        holidayRepository.save(holiday);
        // A holiday supersedes any attendance already marked for the class on that date.
        attendanceRepository.deleteByClassEnrollmentClassSubjectSchoolClassIdAndAttendanceDate(schoolClass.getId(), request.getHolidayDate());
        return map(holiday);
    }

    public void delete(Long id, UserPrincipal principal) {
        ClassHoliday holiday = holidayRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Class holiday", id));
        requireCanManage(holiday.getSchoolClass(), principal);
        holidayRepository.delete(holiday);
    }

    public boolean isHoliday(Long classId, LocalDate date) {
        return holidayRepository.existsBySchoolClassIdAndHolidayDate(classId, date);
    }

    private SchoolClass getClass(Long id) {
        return schoolClassRepository.findByIdWithTeacher(id).orElseThrow(() -> ResourceNotFoundException.of("Class", id));
    }

    private void requireCanView(SchoolClass schoolClass, UserPrincipal principal) {
        if (isAdmin(principal)) return;
        if ("TEACHER".equalsIgnoreCase(principal.getRole())) {
            if (schoolClass.getTeacher() != null && principal.getId().equals(schoolClass.getTeacher().getId())) return;
            boolean assigned = classSubjectRepository.findAllByTeacherId(principal.getId()).stream().anyMatch(cs -> cs.getSchoolClass().getId().equals(schoolClass.getId()));
            if (assigned) return;
        }
        if ("STUDENT".equalsIgnoreCase(principal.getRole())) {
            Long studentId = studentIdentityService.requireStudentId(principal);
            if (classEnrollmentRepository.findAllByStudentId(studentId).stream().anyMatch(e -> e.getClassSubject().getSchoolClass().getId().equals(schoolClass.getId()))) return;
        }
        throw new AccessDeniedException("You are not allowed to view holidays for this class");
    }

    private void requireCanManage(SchoolClass schoolClass, UserPrincipal principal) {
        if (isAdmin(principal)) return;
        if ("TEACHER".equalsIgnoreCase(principal.getRole()) && schoolClass.getTeacher() != null && principal.getId().equals(schoolClass.getTeacher().getId())) return;
        throw new AccessDeniedException("Only the class owner or an administrator can manage class holidays");
    }

    private boolean isAdmin(UserPrincipal principal) { return "ADMIN".equalsIgnoreCase(principal.getRole()) || "SUPER_ADMIN".equalsIgnoreCase(principal.getRole()); }
    private String normalize(String value) { if (value == null) return null; String v = value.trim(); return v.isEmpty() ? null : v; }
    private ClassHolidayResponse map(ClassHoliday holiday) {
        return ClassHolidayResponse.builder().id(holiday.getId()).classId(holiday.getSchoolClass().getId()).className(holiday.getSchoolClass().getName()).holidayDate(holiday.getHolidayDate()).reason(holiday.getReason()).build();
    }
}
