package com.collegeerp.Backend.result.controller;

import com.collegeerp.Backend.common.exception.ForbiddenException;
import com.collegeerp.Backend.result.dto.OverallResultResponse;
import com.collegeerp.Backend.result.dto.SemesterResultResponse;
import com.collegeerp.Backend.result.service.ResultService;
import com.collegeerp.Backend.schoolclass.repository.ClassEnrollmentRepository;
import com.collegeerp.Backend.security.UserPrincipal;
import com.collegeerp.Backend.student.service.StudentIdentityService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/results")
public class ResultController {

    private final ResultService resultService;
    private final StudentIdentityService studentIdentityService;
    private final ClassEnrollmentRepository classEnrollmentRepository;

    public ResultController(
            ResultService resultService,
            StudentIdentityService studentIdentityService,
            ClassEnrollmentRepository classEnrollmentRepository) {
        this.resultService = resultService;
        this.studentIdentityService = studentIdentityService;
        this.classEnrollmentRepository = classEnrollmentRepository;
    }

    @GetMapping("/student/{studentId}/semester")
    public SemesterResultResponse getSemesterResult(Authentication authentication,
                                                      @PathVariable Long studentId,
                                                      @RequestParam Integer semester,
                                                      @RequestParam String academicYear) {
        requireAuthorized(authentication, studentId);
        return resultService.getSemesterResult(studentId, semester, academicYear);
    }

    @GetMapping("/student/{studentId}/overall")
    public OverallResultResponse getOverallResult(Authentication authentication, @PathVariable Long studentId) {
        requireAuthorized(authentication, studentId);
        return resultService.getOverallResult(studentId);
    }

    /**
     * Students may read only their own domain Student record. Teachers may read results only
     * for students enrolled in a ClassSubject they actually teach. College admins may read all.
     */
    private void requireAuthorized(Authentication authentication, Long studentId) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String role = principal.getRole() == null ? "" : principal.getRole().trim();

        if ("ADMIN".equalsIgnoreCase(role) || "SUPER_ADMIN".equalsIgnoreCase(role)) {
            return;
        }
        if ("STUDENT".equalsIgnoreCase(role)) {
            if (!studentId.equals(studentIdentityService.requireStudentId(principal))) {
                throw new ForbiddenException("You can only view your own results");
            }
            return;
        }
        if ("TEACHER".equalsIgnoreCase(role)) {
            if (!classEnrollmentRepository.existsByStudentIdAndClassSubjectTeacherId(studentId, principal.getId())) {
                throw new ForbiddenException("You can only view results for students you teach");
            }
            return;
        }
        throw new ForbiddenException("You are not allowed to view student results");
    }
}
