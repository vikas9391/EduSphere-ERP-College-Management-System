package com.collegeerp.Backend.tenant.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.collegeerp.Backend.common.UserRepository;
import com.collegeerp.Backend.common.exception.ResourceNotFoundException;
import com.collegeerp.Backend.course.repository.CourseRepository;
import com.collegeerp.Backend.department.repository.DepartmentRepository;
import com.collegeerp.Backend.enrollment.repository.EnrollmentRepository;
import com.collegeerp.Backend.student.repository.StudentRepository;
import com.collegeerp.Backend.subject.repository.SubjectRepository;
import com.collegeerp.Backend.teacher.repository.TeacherRepository;
import com.collegeerp.Backend.tenant.TenantContext;
import com.collegeerp.Backend.tenant.dto.TenantDetailsResponse;
import com.collegeerp.Backend.tenant.dto.TenantSummaryResponse;
import com.collegeerp.Backend.tenant.entity.Tenant;
import com.collegeerp.Backend.tenant.repository.TenantRepository;

/**
 * Backs {@code GET /api/tenants/{id}/details} — a super-admin-only "how much is this
 * college actually using the platform" snapshot. Every count below lives inside that
 * college's own isolated schema, so (like {@code AuthController#login} and
 * {@code TenantProvisioningService#seedAdminUser}) this has to point
 * {@link TenantContext} at the target schema before querying and clear it afterward,
 * even for a suspended college — suspension only blocks login, not this read-only view.
 */
@Service
public class TenantStatsService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final DepartmentRepository departmentRepository;
    private final CourseRepository courseRepository;
    private final SubjectRepository subjectRepository;
    private final EnrollmentRepository enrollmentRepository;

    public TenantStatsService(
            TenantRepository tenantRepository,
            UserRepository userRepository,
            TeacherRepository teacherRepository,
            StudentRepository studentRepository,
            DepartmentRepository departmentRepository,
            CourseRepository courseRepository,
            SubjectRepository subjectRepository,
            EnrollmentRepository enrollmentRepository) {

        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.departmentRepository = departmentRepository;
        this.courseRepository = courseRepository;
        this.subjectRepository = subjectRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    public TenantDetailsResponse getDetails(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> ResourceNotFoundException.of("College", tenantId));

        TenantContext.setCurrentTenant(tenant.getSchemaName());
        try {
            return new TenantDetailsResponse(
                    TenantSummaryResponse.from(tenant),
                    userRepository.count(),
                    teacherRepository.count(),
                    studentRepository.count(),
                    departmentRepository.count(),
                    courseRepository.count(),
                    subjectRepository.count(),
                    enrollmentRepository.count()
            );
        } finally {
            TenantContext.clear();
        }
    }
}
