package hackthon.mangaement.hackathon.controller;

import hackthon.mangaement.hackathon.model.User.User;
import hackthon.mangaement.hackathon.model.organization.Submission;
import hackthon.mangaement.hackathon.repository.SubmissionRepository;
import hackthon.mangaement.hackathon.service.MeService;
import hackthon.mangaement.hackathon.service.NotificationService;
import hackthon.mangaement.hackathon.service.ScoringService;
import hackthon.mangaement.hackathon.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/me")
public class MeController {

    @Autowired
    private MeService meService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private ScoringService scoringService;

    @GetMapping("/hackathons/browse")
    public ResponseEntity<?> browseHackathons(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(meService.getBrowseableHackathons(user));
    }

    @PostMapping("/hackathons/{id}/register")
    public ResponseEntity<?> registerHackathon(@PathVariable Integer id, @AuthenticationPrincipal User user) {
        meService.registerForHackathon(id, user);
        return ResponseEntity.ok(Map.of("message", "Registered to hackathon successfully."));
    }

    @GetMapping("/judge/submissions")
    public ResponseEntity<?> getJudgeSubmissions(@AuthenticationPrincipal User judge) {
        return ResponseEntity.ok(meService.getJudgeSubmissions(judge));
    }

    @GetMapping("/notifications")
    public ResponseEntity<?> getMyNotifications(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(notificationService.getNotificationsForUser(user.getId()));
    }

    @PatchMapping("/notifications/read")
    public ResponseEntity<?> markNotificationsAsRead(@RequestBody(required = false) Map<String, Object> req, @AuthenticationPrincipal User user) {
        notificationService.markAllAsRead(user.getId());
        return ResponseEntity.ok(Map.of("message", "Notifications marked as read."));
    }

    @GetMapping("/teams/{teamId}/rounds/{roundId}/score-breakdown")
    public ResponseEntity<?> getTeamScoreBreakdown(@PathVariable Integer teamId,
                                                   @PathVariable Integer roundId) {
        Submission sub = submissionRepository.findByTeamIdAndRoundId(teamId, roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found for Team ID " + teamId + " and Round ID " + roundId));
        Map<String, Object> breakdown = scoringService.getScoreBreakdown(roundId, sub.getId());
        return ResponseEntity.ok(breakdown);
    }
}
