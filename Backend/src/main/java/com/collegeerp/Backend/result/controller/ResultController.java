package com.collegeerp.Backend.result.controller;

import com.collegeerp.Backend.common.exception.ForbiddenException;
import com.collegeerp.Backend.result.dto.OverallResultResponse;
import com.collegeerp.Backend.result.dto.SemesterResultResponse;
import com.collegeerp.Backend.result.service.ResultService;
import com.collegeerp.Backend.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/results")
public class ResultController {

    private static final String STUDENT_ROLE = "STUDENT";

    private final ResultService resultService;

    public ResultController(ResultService resultService) {
        this.resultService = resultService;
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
     * own - {@code #studentId} isn't validated against the caller anywhere else, so
     * without this a student token could read a classmate's grades just by changing the
     * path variable.
     */
    private void requireSelfOrStaff(Authentication authentication, Long studentId) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        if (STUDENT_ROLE.equals(principal.getRole()) && !studentId.equals(principal.getId())) {
            throw new ForbiddenException("You can only view your own results");
        }
    }
}
