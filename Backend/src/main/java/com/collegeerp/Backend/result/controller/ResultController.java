package com.collegeerp.Backend.result.controller;

import com.collegeerp.Backend.common.exception.ForbiddenException;
import com.collegeerp.Backend.result.dto.OverallResultResponse;
import com.collegeerp.Backend.result.dto.SemesterResultResponse;
import com.collegeerp.Backend.result.service.ResultService;
import com.collegeerp.Backend.security.UserPrincipal;
import com.collegeerp.Backend.student.service.StudentIdentityService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/results")
public class ResultController {

    private static final String STUDENT_ROLE = "STUDENT";

    private final ResultService resultService;
    private final StudentIdentityService studentIdentityService;

    public ResultController(ResultService resultService, StudentIdentityService studentIdentityService) {
        this.resultService = resultService;
        this.studentIdentityService = studentIdentityService;
    }

    @GetMapping("/student/{studentId}/semester")
    public SemesterResultResponse getSemesterResult(Authentication authentication,
                                                      @PathVariable Long studentId,
                                                      @RequestParam Integer semester,
                                                      @RequestParam String academicYear) {
        requireSelfOrStaff(authentication, studentId);
        return resultService.getSemesterResult(studentId, semester, academicYear);
    }

    @GetMapping("/student/{studentId}/overall")
    public OverallResultResponse getOverallResult(Authentication authentication, @PathVariable Long studentId) {
        requireSelfOrStaff(authentication, studentId);
        return resultService.getOverallResult(studentId);
    }

    /**
     * ADMIN/TEACHER may look up any student's results; a STUDENT may only look up their
     * own domain Student record. UserPrincipal.id is the authenticated User id and must
     * never be compared directly with Student.id.
     */
    private void requireSelfOrStaff(Authentication authentication, Long studentId) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        if (STUDENT_ROLE.equalsIgnoreCase(principal.getRole())
                && !studentId.equals(studentIdentityService.requireStudentId(principal))) {
            throw new ForbiddenException("You can only view your own results");
        }
    }
}
