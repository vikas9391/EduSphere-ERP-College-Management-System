package com.collegeerp.Backend.teacher.service;

import com.collegeerp.Backend.common.exception.BadRequestException;
import com.collegeerp.Backend.common.exception.DuplicateResourceException;
import com.collegeerp.Backend.common.exception.ResourceNotFoundException;
import com.collegeerp.Backend.teacher.dto.TeacherRequest;
import com.collegeerp.Backend.teacher.dto.TeacherResponse;
import com.collegeerp.Backend.teacher.entity.Teacher;
import com.collegeerp.Backend.teacher.repository.TeacherRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@Transactional
public class TeacherService {

    private static final Logger log = LoggerFactory.getLogger(TeacherService.class);

    private final TeacherRepository teacherRepository;
    private final PasswordEncoder passwordEncoder;

    public TeacherService(TeacherRepository teacherRepository, PasswordEncoder passwordEncoder) {
        this.teacherRepository = teacherRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public TeacherResponse createTeacher(TeacherRequest request) {

        if (!StringUtils.hasText(request.getPassword())) {
            throw new BadRequestException("Password is required to create a teacher");
        }

        if (teacherRepository.existsByEmployeeId(request.getEmployeeId())) {
            throw new DuplicateResourceException("Employee ID '" + request.getEmployeeId() + "' is already in use");
        }

        if (teacherRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email '" + request.getEmail() + "' is already in use");
        }

        Teacher teacher = Teacher.builder()
                .employeeId(request.getEmployeeId())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .gender(request.getGender())
                .qualification(request.getQualification())
                .specialization(request.getSpecialization())
                .experience(request.getExperience())
                .joiningDate(request.getJoiningDate())
                .createdAt(LocalDateTime.now())
                .build();

        teacher = teacherRepository.save(teacher);
        log.info("Created teacher id={} employeeId={}", teacher.getId(), teacher.getEmployeeId());

        return mapToResponse(teacher);
    }

    @Transactional(readOnly = true)
    public Page<TeacherResponse> getAllTeachers(Pageable pageable) {
        return teacherRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public TeacherResponse getTeacher(Long id) {
        return mapToResponse(findTeacherOrThrow(id));
    }

    public TeacherResponse updateTeacher(Long id, TeacherRequest request) {

        Teacher teacher = findTeacherOrThrow(id);

        if (!teacher.getEmail().equalsIgnoreCase(request.getEmail())
                && teacherRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email '" + request.getEmail() + "' is already in use");
        }

        teacher.setFirstName(request.getFirstName());
        teacher.setLastName(request.getLastName());
        teacher.setEmail(request.getEmail());

        if (StringUtils.hasText(request.getPassword())) {
            teacher.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        teacher.setPhone(request.getPhone());
        teacher.setGender(request.getGender());
        teacher.setQualification(request.getQualification());
        teacher.setSpecialization(request.getSpecialization());
        teacher.setExperience(request.getExperience());
        teacher.setJoiningDate(request.getJoiningDate());

        teacher = teacherRepository.save(teacher);
        log.info("Updated teacher id={}", teacher.getId());

        return mapToResponse(teacher);
    }

    public void deleteTeacher(Long id) {
        Teacher teacher = findTeacherOrThrow(id);
        teacherRepository.delete(teacher);
        log.info("Deleted teacher id={}", id);
    }

    private Teacher findTeacherOrThrow(Long id) {
        return teacherRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Teacher", id));
    }

    private TeacherResponse mapToResponse(Teacher teacher) {
        return TeacherResponse.builder()
                .id(teacher.getId())
                .employeeId(teacher.getEmployeeId())
                .firstName(teacher.getFirstName())
                .lastName(teacher.getLastName())
                .email(teacher.getEmail())
                .phone(teacher.getPhone())
                .gender(teacher.getGender())
                .qualification(teacher.getQualification())
                .specialization(teacher.getSpecialization())
                .experience(teacher.getExperience())
                .joiningDate(teacher.getJoiningDate())
                .build();
    }
}
