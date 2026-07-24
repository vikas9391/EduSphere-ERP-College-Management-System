package com.collegeerp.Backend.department.service;

import com.collegeerp.Backend.common.exception.DuplicateResourceException;
import com.collegeerp.Backend.common.exception.ResourceNotFoundException;
import com.collegeerp.Backend.department.dto.DepartmentRequest;
import com.collegeerp.Backend.department.dto.DepartmentResponse;
import com.collegeerp.Backend.department.entity.Department;
import com.collegeerp.Backend.department.repository.DepartmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class DepartmentService {

    private static final Logger log = LoggerFactory.getLogger(DepartmentService.class);

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    public DepartmentResponse createDepartment(DepartmentRequest request) {

        if (departmentRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException("Department code '" + request.getCode() + "' already exists");
        }

        Department department = Department.builder()
                .code(request.getCode())
                .name(request.getName())
                .hod(request.getHod())
                .description(request.getDescription())
                .createdAt(LocalDateTime.now())
                .build();

        department = departmentRepository.save(department);
        log.info("Created department id={} code={}", department.getId(), department.getCode());

        return mapToResponse(department);
    }

    @Transactional(readOnly = true)
    public Page<DepartmentResponse> getAllDepartments(Pageable pageable) {
        return departmentRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getDepartment(Long id) {
        return mapToResponse(findDepartmentOrThrow(id));
    }

    public DepartmentResponse updateDepartment(Long id, DepartmentRequest request) {

        Department department = findDepartmentOrThrow(id);

        if (!department.getCode().equals(request.getCode())
                && departmentRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException("Department code '" + request.getCode() + "' already exists");
        }

        department.setCode(request.getCode());
        department.setName(request.getName());
        department.setHod(request.getHod());
        department.setDescription(request.getDescription());

        department = departmentRepository.save(department);
        log.info("Updated department id={}", department.getId());

        return mapToResponse(department);
    }

    public void deleteDepartment(Long id) {
        Department department = findDepartmentOrThrow(id);
        // Relies on GlobalExceptionHandler#handleDataIntegrityViolation to translate a
        // FK violation (e.g. Courses still pointing at this department) into a clean 409
        // rather than an opaque 500 - avoids a direct dependency on the course package here.
        departmentRepository.delete(department);
        log.info("Deleted department id={}", id);
    }

    private Department findDepartmentOrThrow(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Department", id));
    }

    private DepartmentResponse mapToResponse(Department department) {
        return DepartmentResponse.builder()
                .id(department.getId())
                .code(department.getCode())
                .name(department.getName())
                .hod(department.getHod())
                .description(department.getDescription())
                .build();
    }
}
