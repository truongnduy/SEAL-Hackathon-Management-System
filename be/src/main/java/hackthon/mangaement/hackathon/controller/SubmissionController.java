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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
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
        return ResponseEntity.ok(mapSubmissionToMap(sub));
    }

    @GetMapping("/submissions")
    public ResponseEntity<?> getSubmissions(@RequestParam(required = false) Integer roundId,
                                            @RequestParam(required = false) String status) {
        List<Submission> list = submissionService.getSubmissions(roundId, status);
        List<Map<String, Object>> mapped = list.stream()
                .map(this::mapSubmissionToMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(mapped);
    }

    @PatchMapping("/submissions/{id}/review-late")
    public ResponseEntity<?> reviewLateSubmission(@PathVariable Integer id,
                                                  @RequestBody Map<String, Object> req,
                                                  @AuthenticationPrincipal User coordinator,
                                                  HttpServletRequest servletRequest) {
        String decision = (String) req.get("decision");
        boolean approve = "APPROVE".equalsIgnoreCase(decision);
        String note = (String) req.get("note");
        submissionService.reviewLateSubmission(id, approve, note, coordinator, servletRequest.getRemoteAddr());
        return ResponseEntity.ok(Map.of("message", "Late submission review complete."));
    }

    @PatchMapping("/submissions/{id}/approve")
    public ResponseEntity<?> approveLateSubmission(@PathVariable Integer id,
                                                   @AuthenticationPrincipal User coordinator,
                                                   HttpServletRequest servletRequest) {
        submissionService.approveLateSubmission(id, coordinator, servletRequest.getRemoteAddr());
        return ResponseEntity.ok(Map.of("message", "Late submission approved successfully."));
    }

    @PatchMapping("/submissions/{id}/reject")
    public ResponseEntity<?> rejectLateSubmission(@PathVariable Integer id,
                                                  @RequestBody Map<String, Object> req,
                                                  @AuthenticationPrincipal User coordinator,
                                                  HttpServletRequest servletRequest) {
        String reason = (String) req.get("reason");
        submissionService.rejectLateSubmission(id, reason, coordinator, servletRequest.getRemoteAddr());
        return ResponseEntity.ok(Map.of("message", "Late submission rejected successfully."));
    }

    private Map<String, Object> mapSubmissionToMap(Submission sub) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", sub.getId());
        map.put("teamId", sub.getTeam() != null ? sub.getTeam().getId() : null);
        map.put("teamName", sub.getTeam() != null ? sub.getTeam().getTeamName() : null);
        map.put("trackId", sub.getTrack() != null ? sub.getTrack().getId() : null);
        map.put("trackName", sub.getTrack() != null ? sub.getTrack().getName() : null);
        map.put("roundId", sub.getRound() != null ? sub.getRound().getId() : null);
        map.put("repoUrl", sub.getRepoUrl());
        map.put("slideUrl", sub.getSlideUrl());
        map.put("demoUrl", sub.getDemoUrl());
        map.put("reportUrl", sub.getReportUrl());
        map.put("status", sub.getStatus() != null ? sub.getStatus().name() : null);
        map.put("isLate", sub.getIsLate());
        map.put("lateReason", sub.getLateReason());
        map.put("submittedAt", sub.getSubmittedAt() != null ? sub.getSubmittedAt().toString() : null);
        return map;
    }
}
