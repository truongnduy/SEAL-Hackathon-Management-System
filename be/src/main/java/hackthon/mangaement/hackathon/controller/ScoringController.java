package hackthon.mangaement.hackathon.controller;

import hackthon.mangaement.hackathon.model.User.User;
import hackthon.mangaement.hackathon.model.organization.Score;
import hackthon.mangaement.hackathon.service.ScoringService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ScoringController {

    @Autowired
    private ScoringService scoringService;

    @PostMapping("/scores")
    public ResponseEntity<?> submitScore(@RequestBody Map<String, Object> req,
                                         @AuthenticationPrincipal User judge) {
        Score score = scoringService.submitScore(
                (Integer) req.get("submissionId"),
                judge.getId(),
                (Integer) req.get("criterionId"),
                ((Number) req.get("scoreValue")).doubleValue(),
                (String) req.get("comment"),
                Score.ScoreType.valueOf((String) req.get("scoreType"))
        );
        return ResponseEntity.ok(score);
    }

    @PostMapping("/rounds/{roundId}/lock")
    public ResponseEntity<?> lockRoundScoring(@PathVariable Integer roundId,
                                              @RequestBody Map<String, Object> req,
                                              @AuthenticationPrincipal User coordinator,
                                              HttpServletRequest servletRequest) {
        boolean force = (Boolean) req.getOrDefault("force", false);
        String reason = (String) req.get("forceReason");
        scoringService.lockRoundScoring(roundId, coordinator, force, reason, servletRequest.getRemoteAddr());
        Map<String, String> resp = new HashMap<>();
        resp.put("message", "Round scoring locked successfully.");
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/rounds/{roundId}/variance")
    public ResponseEntity<?> getVarianceDashboard(@PathVariable Integer roundId) {
        List<Map<String, Object>> variance = scoringService.getScoreVarianceDashboard(roundId);
        return ResponseEntity.ok(variance);
    }

    @GetMapping("/rounds/{roundId}/progress")
    public ResponseEntity<?> getScoringProgress(@PathVariable Integer roundId) {
        List<Map<String, Object>> progress = scoringService.getScoringProgress(roundId);
        return ResponseEntity.ok(progress);
    }
}
