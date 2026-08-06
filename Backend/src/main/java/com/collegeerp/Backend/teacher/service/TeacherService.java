package com.collegeerp.Backend.teacher.service;

import com.collegeerp.Backend.common.Role;
import com.collegeerp.Backend.common.RoleRepository;
import com.collegeerp.Backend.common.User;
import com.collegeerp.Backend.common.UserRepository;
import com.collegeerp.Backend.common.exception.BadRequestException;
import com.collegeerp.Backend.common.exception.DuplicateResourceException;
import com.collegeerp.Backend.common.exception.ResourceNotFoundException;
import com.collegeerp.Backend.teacher.dto.TeacherRequest;
import com.collegeerp.Backend.teacher.dto.TeacherResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * Teacher CRUD. There is no separate "teachers" table anymore - a teacher is just a
 * {@link User} row whose role is the built-in "TEACHER" system role (seeded in V22 /
 * TenantProvisioningService), with a handful of teacher-only profile columns
 * (employeeId, qualification, specialization, experience, joiningDate, gender, phone)
 * that stay null for every other role. Every FK that used to point at teachers.id
 * (subjects, classes, assignments, exam schedules) now points straight at users.id -
 * so the {@code id} on {@link TeacherResponse} is a {@code users.id}.
 */
@Service
@Transactional
public class TeacherService {

    private static final Logger log = LoggerFactory.getLogger(TeacherService.class);
    private static final String TEACHER_ROLE = "TEACHER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public TeacherService(UserRepository userRepository, RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public TeacherResponse createTeacher(TeacherRequest request) {

        if (!StringUtils.hasText(request.getPassword())) {
            throw new BadRequestException("Password is required to create a teacher");
        }

        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email '" + email + "' is already in use");
        }
        if (userRepository.existsByEmployeeId(request.getEmployeeId())) {
            throw new DuplicateResourceException("Employee ID '" + request.getEmployeeId() + "' is already in use");
        }

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName().trim())
                .lastName(StringUtils.hasText(request.getLastName()) ? request.getLastName().trim() : "-")
                .role(findTeacherRole())
                .isActive(true)
                .isEmailVerified(false)
                // Same convention as UserService#createUser - an admin picked this
                // password, so force a change before the teacher can use the account.
                .mustChangePassword(true)
                .employeeId(request.getEmployeeId())
                .phone(request.getPhone())
                .gender(request.getGender())
                .qualification(request.getQualification())
                .specialization(request.getSpecialization())
                .experience(request.getExperience())
                .joiningDate(request.getJoiningDate())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        user = userRepository.save(user);

        log.info("Created teacher (userId={}, employeeId={})", user.getId(), user.getEmployeeId());
        return mapToResponse(user);
    }

    @Transactional(readOnly = true)
    public Page<TeacherResponse> getAllTeachers(Pageable pageable) {
        return userRepository.findAllByRole_Name(TEACHER_ROLE, pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public TeacherResponse getTeacher(Long id) {
        return mapToResponse(findTeacherOrThrow(id));
    }

    public TeacherResponse updateTeacher(Long id, TeacherRequest request) {

        User user = findTeacherOrThrow(id);
        String email = request.getEmail().trim().toLowerCase();

        boolean emailChanged = !user.getEmail().equalsIgnoreCase(email);
        if (emailChanged && userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email '" + email + "' is already in use");
        }

        boolean employeeIdChanged = !request.getEmployeeId().equals(user.getEmployeeId());
        if (employeeIdChanged && userRepository.existsByEmployeeId(request.getEmployeeId())) {
            throw new DuplicateResourceException("Employee ID '" + request.getEmployeeId() + "' is already in use");
        }

        user.setEmail(email);
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(StringUtils.hasText(request.getLastName()) ? request.getLastName().trim() : "-");
        user.setEmployeeId(request.getEmployeeId());
        user.setPhone(request.getPhone());
        user.setGender(request.getGender());
        user.setQualification(request.getQualification());
        user.setSpecialization(request.getSpecialization());
        user.setExperience(request.getExperience());
        user.setJoiningDate(request.getJoiningDate());
        if (StringUtils.hasText(request.getPassword())) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            user.setMustChangePassword(true);
        }
        user.setUpdatedAt(LocalDateTime.now());
        user = userRepository.save(user);

        log.info("Updated teacher (userId={})", user.getId());
        return mapToResponse(user);
    }

    public void deleteTeacher(Long id) {
        User user = findTeacherOrThrow(id);
        userRepository.delete(user);
        log.info("Deleted teacher (userId={})", id);
    }

    private Role findTeacherRole() {
        return roleRepository.findByName(TEACHER_ROLE)
                .orElseThrow(() -> new BadRequestException(
                        "The built-in 'TEACHER' role is missing for this tenant - contact support"));
    }

    private User findTeacherOrThrow(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Teacher", id));
        if (!TEACHER_ROLE.equals(user.getRole().getName())) {
            throw ResourceNotFoundException.of("Teacher", id);
        }
        return user;
    }

    private TeacherResponse mapToResponse(User user) {
        return TeacherResponse.builder()
                .id(user.getId())
                .employeeId(user.getEmployeeId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .gender(user.getGender())
                .qualification(user.getQualification())
                .specialization(user.getSpecialization())
                .experience(user.getExperience())
                .joiningDate(user.getJoiningDate())
                .roleName(user.getRole().getName())
                .isActive(user.isActive())
                .mustChangePassword(user.isMustChangePassword())
                .build();
    }
}
