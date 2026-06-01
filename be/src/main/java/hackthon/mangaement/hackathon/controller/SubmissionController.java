package hackthon.mangaement.hackathon.controller;

import hackthon.mangaement.hackathon.model.User.User;
import hackthon.mangaement.hackathon.model.organization.Submission;
import hackthon.mangaement.hackathon.service.SubmissionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SubmissionController {

    @Autowired
    private SubmissionService submissionService;

    @PostMapping("/submissions")
    public ResponseEntity<?> submitProject(@RequestBody Map<String, Object> req) {
        Submission sub = submissionService.submitProject(
                (Integer) req.get("teamId"),
                (Integer) req.get("trackId"),
                (Integer) req.get("roundId"),
                (String) req.get("repoUrl"),
                (String) req.get("demoUrl"),
                (String) req.get("reportUrl"),
                (String) req.get("slideUrl"),
                (String) req.get("lateReason")
        );
        return ResponseEntity.ok(sub);
    }

    @PostMapping("/submissions/{id}/review")
    public ResponseEntity<?> reviewLateSubmission(@PathVariable Integer id,
                                                  @RequestBody Map<String, Object> req,
                                                  @AuthenticationPrincipal User coordinator,
                                                  HttpServletRequest servletRequest) {
        boolean approve = (Boolean) req.get("approve");
        String note = (String) req.get("note");
        submissionService.reviewLateSubmission(id, approve, note, coordinator, servletRequest.getRemoteAddr());
        Map<String, String> resp = new HashMap<>();
        resp.put("message", "Late submission review complete.");
        return ResponseEntity.ok(resp);
    }
}
