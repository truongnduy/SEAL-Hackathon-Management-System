package hackthon.mangaement.hackathon.controller;

import hackthon.mangaement.hackathon.exception.ResourceNotFoundException;
import hackthon.mangaement.hackathon.model.organization.Round;
import hackthon.mangaement.hackathon.model.organization.Track;
import hackthon.mangaement.hackathon.model.User.User;
import hackthon.mangaement.hackathon.repository.RoundRepository;
import hackthon.mangaement.hackathon.repository.TrackRepository;
import hackthon.mangaement.hackathon.repository.UserRepository;
import hackthon.mangaement.hackathon.service.NotificationService;
import hackthon.mangaement.hackathon.service.RoundService;
import hackthon.mangaement.hackathon.service.TrackService;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1")
@Transactional
public class RoundController {

    @Autowired
    private RoundRepository roundRepository;

    @Autowired
    private TrackRepository trackRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private RoundService roundService;

    @Autowired
    private TrackService trackService;

    // --- ROUNDS ENDPOINTS ---

    @Cacheable(value = "rounds", key = "#id")
    @GetMapping("/rounds/{id}")
    public ResponseEntity<?> getRoundById(@PathVariable Integer id) {
        Round round = roundRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Round not found with ID: " + id));
        return ResponseEntity.ok(round);
    }

    @CacheEvict(value = "rounds", key = "#roundId")
    @PatchMapping("/rounds/{roundId}/transition")
    public ResponseEntity<?> transitionRound(@PathVariable Integer roundId, @RequestBody Map<String, Object> req,
            @AuthenticationPrincipal User coordinator) {
        return ResponseEntity.ok(Map.of("message", "Round transitioned manually (legacy)."));
    }

    @GetMapping("/rounds")
    public ResponseEntity<?> getAllRounds() {
        return ResponseEntity.ok(roundRepository.findAll());
    }

    @CacheEvict(value = "rounds", key = "#id")
    @PatchMapping("/rounds/{id}/advance")
    public ResponseEntity<?> advanceRound(@PathVariable Integer id, @RequestBody(required = false) Map<String, Object> req, @AuthenticationPrincipal User coordinator) {
        roundService.advanceRound(id, coordinator);
        return ResponseEntity.ok(Map.of("message", "Round advanced."));
    }

    @CacheEvict(value = "rounds", key = "#id")
    @PatchMapping("/rounds/{id}/publish")
    public ResponseEntity<?> publishRound(@PathVariable Integer id, @RequestBody(required = false) Map<String, Object> req, @AuthenticationPrincipal User coordinator) {
        roundService.publishRound(id, coordinator);
        return ResponseEntity.ok(Map.of("message", "Round published."));
    }

    @CacheEvict(value = "rounds", key = "#id")
    @PutMapping("/rounds/{id}")
    public ResponseEntity<?> updateRound(@PathVariable Integer id, @RequestBody Map<String, Object> req) {
        Round r = roundRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Round not found with ID: " + id));
        if (req.containsKey("name")) r.setName((String) req.get("name"));
        if (req.containsKey("sequenceOrder")) r.setSequenceOrder((Integer) req.get("sequenceOrder"));
        if (req.containsKey("isFinal")) r.setIsFinal((Boolean) req.get("isFinal"));
        if (req.containsKey("roundType")) r.setRoundType(Round.RoundType.valueOf((String) req.get("roundType")));
        if (req.containsKey("codingDurationHours")) r.setCodingDurationHours((Integer) req.get("codingDurationHours"));
        if (req.containsKey("submissionOpen")) {
            String subOpenStr = (String) req.get("submissionOpen");
            r.setSubmissionOpen(subOpenStr != null ? LocalDateTime.parse(subOpenStr) : null);
        }
        if (req.containsKey("submissionDeadline")) {
            r.setSubmissionDeadline(LocalDateTime.parse((String) req.get("submissionDeadline")));
        }
        if (req.containsKey("lateSubmissionPolicy")) {
            r.setLateSubmissionPolicy(Round.LateSubmissionPolicy.valueOf((String) req.get("lateSubmissionPolicy")));
        }
        if (req.containsKey("problemStatementUrl")) r.setProblemStatementUrl((String) req.get("problemStatementUrl"));
        if (req.containsKey("problemReleasedAt")) {
            String releasedAtStr = (String) req.get("problemReleasedAt");
            r.setProblemReleasedAt(releasedAtStr != null ? LocalDateTime.parse(releasedAtStr) : null);
        }
        if (req.containsKey("topNAdvance")) r.setTopNAdvance((Integer) req.get("topNAdvance"));
        if (req.containsKey("minTeamsFinal")) r.setMinTeamsFinal((Integer) req.get("minTeamsFinal"));
        if (req.containsKey("wildcardEnabled")) r.setWildcardEnabled((Boolean) req.get("wildcardEnabled"));
        if (req.containsKey("tiebreakRule")) {
            r.setTiebreakRule(Round.TiebreakRule.valueOf((String) req.get("tiebreakRule")));
        }
        if (req.containsKey("isActive")) r.setIsActive((Boolean) req.get("isActive"));
        return ResponseEntity.ok(roundRepository.save(r));
    }

    @CacheEvict(value = "rounds", key = "#id")
    @DeleteMapping("/rounds/{id}")
    public ResponseEntity<?> deleteRound(@PathVariable Integer id) {
        Round r = roundRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Round not found with ID: " + id));
        roundRepository.delete(r);
        return ResponseEntity.ok(Map.of("message", "Round deleted successfully."));
    }

    @CacheEvict(value = "rounds", key = "#id")
    @PatchMapping("/rounds/{id}/activate")
    public ResponseEntity<?> activateRound(@PathVariable Integer id) {
        Round round = roundRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Round not found with ID: " + id));
        round.setIsActive(true);
        Round saved = roundRepository.save(round);

        // Send ROUND_STARTED notifications to all judges and students
        try {
            List<User> participants = userRepository.findAll();
            for (User u : participants) {
                if (u.getRole() == User.Role.JUDGE || u.getRole() == User.Role.STUDENT) {
                    notificationService.sendNotification(
                            u,
                            "ROUND_STARTED",
                            "Round Started: " + round.getName(),
                            "The " + round.getName() + " has officially started. Submissions are now open.",
                            "rounds",
                            round.getId()
                    );
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to send ROUND_STARTED notifications: " + e.getMessage());
        }

        return ResponseEntity.ok(saved);
    }

    // --- TRACKS ENDPOINTS ---

    @GetMapping("/hackathons/{id}/tracks")
    public ResponseEntity<?> getTracksByHackathon(@PathVariable Integer id) {
        return ResponseEntity.ok(trackRepository.findByRoundHackathonIdOrderBySequenceOrderAsc(id));
    }

    @GetMapping("/tracks")
    public ResponseEntity<?> getAllTracks() {
        return ResponseEntity.ok(trackRepository.findAll());
    }

    @GetMapping("/tracks/{id}/problem-statement")
    public ResponseEntity<?> getTrackProblemStatement(@PathVariable Integer id) {
        return ResponseEntity.ok(trackService.getProblemStatement(id));
    }

    @Cacheable(value = "tracks", key = "#id")
    @GetMapping("/tracks/{id}")
    public ResponseEntity<?> getTrackById(@PathVariable Integer id) {
        Track track = trackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Track not found with ID: " + id));
        return ResponseEntity.ok(track);
    }

    @CacheEvict(value = "tracks", key = "#id")
    @PutMapping("/tracks/{id}")
    public ResponseEntity<?> updateTrack(@PathVariable Integer id, @RequestBody Map<String, Object> req) {
        Track t = trackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Track not found with ID: " + id));
        if (req.containsKey("name")) t.setName((String) req.get("name"));
        if (req.containsKey("description")) t.setDescription((String) req.get("description"));
        if (req.containsKey("topic")) t.setTopic((String) req.get("topic"));
        if (req.containsKey("maxTeams")) t.setMaxTeams((Integer) req.get("maxTeams"));
        if (req.containsKey("maxTeamsPerGroup")) t.setMaxTeamsPerGroup((Integer) req.get("maxTeamsPerGroup"));
        if (req.containsKey("minTeamSize")) t.setMinTeamSize((Integer) req.get("minTeamSize"));
        if (req.containsKey("maxTeamSize")) t.setMaxTeamSize((Integer) req.get("maxTeamSize"));
        if (req.containsKey("sequenceOrder")) t.setSequenceOrder((Integer) req.get("sequenceOrder"));
        if (req.containsKey("status")) {
            t.setStatus(Track.Status.valueOf((String) req.get("status")));
        }
        return ResponseEntity.ok(trackRepository.save(t));
    }

    @CacheEvict(value = "tracks", key = "#id")
    @PatchMapping("/tracks/{id}")
    public ResponseEntity<?> patchTrack(@PathVariable Integer id, @RequestBody Map<String, Object> req) {
        Track t = trackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Track not found with ID: " + id));
        if (req.containsKey("topic")) {
            t.setTopic((String) req.get("topic"));
        }
        return ResponseEntity.ok(trackRepository.save(t));
    }

    @CacheEvict(value = "tracks", key = "#id")
    @DeleteMapping("/tracks/{id}")
    public ResponseEntity<?> deleteTrack(@PathVariable Integer id) {
        Track t = trackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Track not found with ID: " + id));
        trackRepository.delete(t);
        return ResponseEntity.ok(Map.of("message", "Track deleted successfully."));
    }

    @GetMapping("/rounds/{id}/problem-statement")
    public ResponseEntity<?> getProblemStatement(@PathVariable Integer id) {
        Round r = roundRepository.findById(id).orElseThrow();
        // Giả sử content đề bài lưu trong problemStatementUrl (hoặc thêm cột problemContent)
        return ResponseEntity.ok(Map.of("content", r.getProblemStatementUrl() != null ? r.getProblemStatementUrl() : ""));
    }

    @CacheEvict(value = "rounds", key = "#id")
    @PostMapping("/rounds/{id}/release-problem")
    public ResponseEntity<?> releaseProblem(@PathVariable Integer id) {
        Round r = roundRepository.findById(id).orElseThrow();
        r.setProblemReleasedAt(LocalDateTime.now());
        roundRepository.save(r);
        return ResponseEntity.ok(Map.of("message", "Problem released successfully!"));
    }
}
