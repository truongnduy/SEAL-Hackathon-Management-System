package hackthon.mangaement.hackathon.controller;

import hackthon.mangaement.hackathon.model.User.User;
import hackthon.mangaement.hackathon.service.CalibrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class CalibrationController {

    @Autowired
    private CalibrationService calibrationService;

    @GetMapping("/calibration-sessions")
    public ResponseEntity<?> getAllCalibrationSessions(@AuthenticationPrincipal User coordinator) {
        return ResponseEntity.ok(calibrationService.getAllSessions(coordinator));
    }

    @GetMapping("/calibration-sessions/{id}")
    public ResponseEntity<?> getCalibrationSessionById(@PathVariable Integer id, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(calibrationService.getSessionById(id, user));
    }

    @PostMapping("/scores/calibration")
    public ResponseEntity<?> submitCalibrationScore(@RequestBody Map<String, Object> req, @AuthenticationPrincipal User judge) {
        calibrationService.submitCalibrationScore(req, judge);
        return ResponseEntity.ok(Map.of("message", "Calibration score submitted successfully."));
    }
}
