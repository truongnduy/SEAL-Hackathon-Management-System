package hackthon.mangaement.hackathon.controller;

import hackthon.mangaement.hackathon.service.PresentationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/presentation")
public class PresentationController {

    @Autowired
    private PresentationService presentationService;

    @GetMapping("/queue")
    public ResponseEntity<?> getPresentationQueue() {
        return ResponseEntity.ok(List.of());
    }

    @PostMapping("/timer/start")
    public ResponseEntity<?> startTimer(@RequestBody Map<String, Integer> req) {
        presentationService.manageTimer("START", req.get("trackId"));
        return ResponseEntity.ok(Map.of("message", "Timer started"));
    }

    @PostMapping("/timer/pause")
    public ResponseEntity<?> pauseTimer(@RequestBody Map<String, Integer> req) {
        presentationService.manageTimer("PAUSE", req.get("trackId"));
        return ResponseEntity.ok(Map.of("message", "Timer paused"));
    }

    @PostMapping("/timer/resume")
    public ResponseEntity<?> resumeTimer(@RequestBody Map<String, Integer> req) {
        presentationService.manageTimer("RESUME", req.get("trackId"));
        return ResponseEntity.ok(Map.of("message", "Timer resumed"));
    }

    @PostMapping("/timer/reset")
    public ResponseEntity<?> resetTimer(@RequestBody Map<String, Integer> req) {
        presentationService.manageTimer("RESET", req.get("trackId"));
        return ResponseEntity.ok(Map.of("message", "Timer reset"));
    }

    @PostMapping("/timer/qa")
    public ResponseEntity<?> qaTimer(@RequestBody Map<String, Integer> req) {
        presentationService.manageTimer("QA", req.get("trackId"));
        return ResponseEntity.ok(Map.of("message", "QA Timer started"));
    }

    @PostMapping("/queue/shuffle")
    public ResponseEntity<?> shuffleQueue(@RequestBody Map<String, Object> req) {
        Integer roundId = req.get("roundId") != null ? (Integer) req.get("roundId") : null;
        Integer trackId = req.get("trackId") != null ? (Integer) req.get("trackId") : null;
        presentationService.shuffleQueue(roundId, trackId);
        return ResponseEntity.ok(Map.of("message", "Queue shuffled"));
    }

    @PatchMapping("/queue/next")
    public ResponseEntity<?> nextInQueue(@RequestParam(required = false) Integer roundId, @RequestParam(required = false) Integer trackId, @RequestBody(required = false) Map<String, Object> req) {
        presentationService.nextInQueue(roundId, trackId);
        return ResponseEntity.ok(Map.of("message", "Next presentation triggered"));
    }

    @GetMapping("/tracks/{trackId}/controller")
    public ResponseEntity<?> getTrackController(@PathVariable Integer trackId) {
        return ResponseEntity.ok(presentationService.getTrackControllerStatus(trackId));
    }

    @GetMapping("/rounds/{roundId}/controller")
    public ResponseEntity<?> getRoundController(@PathVariable Integer roundId) {
        return ResponseEntity.ok(presentationService.getRoundControllerStatus(roundId));
    }
}
