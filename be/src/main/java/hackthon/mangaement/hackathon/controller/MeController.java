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

    @GetMapping("/judge-track-assignments")
    public ResponseEntity<?> getJudgeTrackAssignments(@AuthenticationPrincipal User judge) {
        return ResponseEntity.ok(scoringService.getJudgeTrackAssignments(judge));
    }

    @GetMapping("/judge-final-assignments")
    public ResponseEntity<?> getJudgeFinalAssignments(@AuthenticationPrincipal User judge) {
        return ResponseEntity.ok(scoringService.getJudgeFinalAssignments(judge));
    }

    @GetMapping("/scores")
    public ResponseEntity<?> getMyScores(@AuthenticationPrincipal User judge, @RequestParam Integer roundId) {
        return ResponseEntity.ok(scoringService.getMyScoresForRound(judge, roundId));
    }

    @PatchMapping("/scores/{scoreId}/comment")
    public ResponseEntity<?> updateScoreComment(@AuthenticationPrincipal User judge,
                                                @PathVariable Integer scoreId,
                                                @RequestBody Map<String, String> req) {
        scoringService.updateScoreComment(judge, scoreId, req.get("comment"));
        return ResponseEntity.ok(Map.of("message", "Comment updated successfully."));
    }

    @PatchMapping("/scoring-completion")
    public ResponseEntity<?> updateScoringCompletion(@AuthenticationPrincipal User judge,
                                                     @RequestBody Map<String, Object> req) {
        Integer assignmentId = (Integer) req.get("assignmentId");
        String completionStatus = (String) req.get("completionStatus");
        scoringService.updateScoringCompletion(judge, assignmentId, completionStatus);
        return ResponseEntity.ok(Map.of("message", "Scoring completion status updated."));
    }

    @GetMapping("/judge/presentation-scoring-status")
    public ResponseEntity<?> getPresentationScoringStatus(@AuthenticationPrincipal User judge,
                                                          @RequestParam Integer roundId,
                                                          @RequestParam(required = false) Integer trackId) {
        return ResponseEntity.ok(scoringService.getPresentationScoringStatus(judge, roundId, trackId));
    }

    @PostMapping("/judge/submissions/{submissionId}/confirm-scoring")
    public ResponseEntity<?> confirmSubmissionScoring(@AuthenticationPrincipal User judge,
                                                      @PathVariable Integer submissionId) {
        scoringService.confirmSubmissionScoring(judge, submissionId);
        return ResponseEntity.ok(Map.of("message", "Submission scoring confirmed."));
    }
}
