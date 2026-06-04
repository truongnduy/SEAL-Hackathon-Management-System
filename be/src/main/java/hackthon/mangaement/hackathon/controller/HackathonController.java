package hackthon.mangaement.hackathon.controller;

import hackthon.mangaement.hackathon.exception.ResourceNotFoundException;
import hackthon.mangaement.hackathon.model.User.User;
import hackthon.mangaement.hackathon.model.organization.Criteria;
import hackthon.mangaement.hackathon.model.organization.Event;
import hackthon.mangaement.hackathon.model.organization.Hackathon;
import hackthon.mangaement.hackathon.model.organization.Round;
import hackthon.mangaement.hackathon.model.organization.Track;
import hackthon.mangaement.hackathon.repository.HackathonRepository;
import hackthon.mangaement.hackathon.repository.RoundRepository;
import hackthon.mangaement.hackathon.repository.TrackRepository;
import hackthon.mangaement.hackathon.repository.EventRepository;
import hackthon.mangaement.hackathon.service.HackathonService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HackathonController {

    @Autowired
    private HackathonService hackathonService;

    @Autowired
    private HackathonRepository hackathonRepository;

    @Autowired
    private RoundRepository roundRepository;

    @Autowired
    private TrackRepository trackRepository;

    @Autowired
    private EventRepository eventRepository;

    @PostMapping("/hackathons")
    public ResponseEntity<?> createHackathon(@RequestBody Map<String, Object> req,
                                             @AuthenticationPrincipal User coordinator) {
        Hackathon h = hackathonService.createHackathon(
                (String) req.get("name"),
                (String) req.get("slug"),
                Hackathon.Season.valueOf((String) req.get("season")),
                (Integer) req.get("year"),
                (String) req.get("description"),
                (String) req.get("rules"),
                (Boolean) req.get("wildcardEnabled"),
                (Boolean) req.get("individualRankingEnabled"),
                coordinator
        );
        return ResponseEntity.ok(h);
    }

    @PostMapping("/hackathons/{id}/rounds")
    public ResponseEntity<?> createRound(@PathVariable Integer id,
                                         @RequestBody Map<String, Object> req) {
        Round r = hackathonService.createRound(
                id,
                (String) req.get("name"),
                (Integer) req.get("sequenceOrder"),
                (Boolean) req.get("isFinal"),
                Round.RoundType.valueOf((String) req.get("roundType")),
                (Integer) req.get("codingDurationHours"),
                LocalDateTime.parse((String) req.get("submissionDeadline")),
                Round.LateSubmissionPolicy.valueOf((String) req.get("lateSubmissionPolicy")),
                (String) req.get("problemStatementUrl"),
                (Integer) req.get("topNAdvance"),
                (Integer) req.get("minTeamsFinal"),
                (Boolean) req.get("wildcardEnabled"),
                Round.TiebreakRule.valueOf((String) req.get("tiebreakRule"))
        );
        return ResponseEntity.ok(r);
    }

    @PostMapping("/rounds/{roundId}/tracks")
    public ResponseEntity<?> createTrack(@PathVariable Integer roundId,
                                         @RequestBody Map<String, Object> req) {
        Track t = hackathonService.createTrack(
                roundId,
                (String) req.get("name"),
                (String) req.get("description"),
                (String) req.get("topic"),
                (Integer) req.get("maxTeams"),
                (Integer) req.get("maxTeamsPerGroup"),
                (Integer) req.get("minTeamSize"),
                (Integer) req.get("maxTeamSize"),
                (Integer) req.get("sequenceOrder")
        );
        return ResponseEntity.ok(t);
    }

    @PostMapping("/criteria")
    public ResponseEntity<?> createCriteria(@RequestBody Map<String, Object> req) {
        Criteria c = hackathonService.createCriteria(
                (Integer) req.get("trackId"),
                (Integer) req.get("roundId"),
                (String) req.get("name"),
                Criteria.CriteriaType.valueOf((String) req.get("type")),
                ((Number) req.get("weight")).doubleValue(),
                (Integer) req.get("maxScore"),
                (String) req.get("description"),
                (String) req.get("rubricUrl"),
                (Integer) req.get("displayOrder")
        );
        return ResponseEntity.ok(c);
    }

    @PostMapping("/hackathons/{id}/events")
    public ResponseEntity<?> createEvent(@PathVariable Integer id,
                                         @RequestBody Map<String, Object> req,
                                         @AuthenticationPrincipal User coordinator) {
        Event e = hackathonService.createEvent(
                id,
                (String) req.get("title"),
                Event.EventType.valueOf((String) req.get("type")),
                (String) req.get("description"),
                (String) req.get("location"),
                (String) req.get("meetUrl"),
                LocalDateTime.parse((String) req.get("startsAt")),
                req.get("endsAt") != null ? LocalDateTime.parse((String) req.get("endsAt")) : null,
                coordinator
        );
        return ResponseEntity.ok(e);
    }

    @PostMapping("/hackathons/{id}/ongoing")
    public ResponseEntity<?> startHackathon(@PathVariable Integer id,
                                            @AuthenticationPrincipal User coordinator,
                                            HttpServletRequest servletRequest) {
        hackathonService.transitionToOngoing(id, coordinator, servletRequest.getRemoteAddr());
        Map<String, String> resp = new HashMap<>();
        resp.put("message", "Hackathon status changed to ONGOING successfully.");
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/hackathons")
    public ResponseEntity<?> getAllHackathons() {
        return ResponseEntity.ok(hackathonRepository.findAll());
    }

    @GetMapping("/hackathons/get-all")
    public ResponseEntity<?> getAllHackathonsLegacy() {
        return ResponseEntity.ok(hackathonRepository.findAll());
    }

    @GetMapping("/hackathons/active")
    public ResponseEntity<?> getActiveHackathons() {
        return ResponseEntity.ok(hackathonRepository.findByStatus(Hackathon.Status.ONGOING));
    }

    @GetMapping("/hackathons/{id}")
    public ResponseEntity<?> getHackathonById(@PathVariable Integer id) {
        Hackathon h = hackathonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon not found with ID: " + id));
        return ResponseEntity.ok(h);
    }

    @PutMapping("/hackathons/{id}")
    public ResponseEntity<?> updateHackathon(@PathVariable Integer id,
                                             @RequestBody Map<String, Object> req) {
        Hackathon h = hackathonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon not found with ID: " + id));

        if (req.containsKey("name")) h.setName((String) req.get("name"));
        if (req.containsKey("slug")) h.setSlug((String) req.get("slug"));
        if (req.containsKey("season")) h.setSeason(Hackathon.Season.valueOf((String) req.get("season")));
        if (req.containsKey("year")) h.setYear(((Number) req.get("year")).intValue());
        if (req.containsKey("description")) h.setDescription((String) req.get("description"));
        if (req.containsKey("rules")) h.setRules((String) req.get("rules"));
        if (req.containsKey("wildcardEnabled")) h.setWildcardEnabled((Boolean) req.get("wildcardEnabled"));
        if (req.containsKey("individualRankingEnabled")) h.setIndividualRankingEnabled((Boolean) req.get("individualRankingEnabled"));

        return ResponseEntity.ok(hackathonRepository.save(h));
    }

    @DeleteMapping("/hackathons/{id}")
    public ResponseEntity<?> deleteHackathon(@PathVariable Integer id) {
        Hackathon h = hackathonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon not found with ID: " + id));
        hackathonRepository.delete(h);
        return ResponseEntity.ok(Map.of("message", "Hackathon deleted successfully."));
    }

    @GetMapping("/hackathons/{id}/rounds")
    public ResponseEntity<?> getRoundsByHackathon(@PathVariable Integer id) {
        return ResponseEntity.ok(roundRepository.findByHackathonIdOrderBySequenceOrderAsc(id));
    }

    @GetMapping("/hackathons/{id}/events")
    public ResponseEntity<?> getEventsByHackathon(@PathVariable Integer id) {
        return ResponseEntity.ok(eventRepository.findByHackathonIdOrderByStartsAtAsc(id));
    }

    @GetMapping("/rounds/{roundId}/tracks")
    public ResponseEntity<?> getTracksByRound(@PathVariable Integer roundId) {
        return ResponseEntity.ok(trackRepository.findByRoundIdOrderBySequenceOrderAsc(roundId));
    }

    @GetMapping("/hackathons/{id}/readiness")
    public ResponseEntity<?> getHackathonReadiness(@PathVariable Integer id) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("valid", true);
        resp.put("errors", List.of());
        return ResponseEntity.ok(resp);
    }

    @PatchMapping("/hackathons/{id}/status")
    public ResponseEntity<?> updateHackathonStatus(@PathVariable Integer id,
                                                   @RequestBody Map<String, Object> req,
                                                   @AuthenticationPrincipal User coordinator,
                                                   HttpServletRequest servletRequest) {
        Hackathon h = hackathonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon not found with ID: " + id));

        String targetStatus = (String) req.get("targetStatus");
        if ("ONGOING".equals(targetStatus)) {
            hackathonService.transitionToOngoing(id, coordinator, servletRequest.getRemoteAddr());
        } else {
            h.setStatus(Hackathon.Status.valueOf(targetStatus));
            hackathonRepository.save(h);
        }

        return ResponseEntity.ok(Map.of("message", "Hackathon status changed to " + targetStatus + " successfully."));
    }
}
