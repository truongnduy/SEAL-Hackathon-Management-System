package hackthon.mangaement.hackathon.controller;

import hackthon.mangaement.hackathon.exception.ResourceNotFoundException;
import hackthon.mangaement.hackathon.model.Judge.JudgeAssignment;
import hackthon.mangaement.hackathon.model.Judge.JudgeAssignment.AssignmentType;
import hackthon.mangaement.hackathon.model.User.User;
import hackthon.mangaement.hackathon.model.organization.Round;
import hackthon.mangaement.hackathon.model.organization.Track;
import hackthon.mangaement.hackathon.repository.JudgeAssignmentRepository;
import hackthon.mangaement.hackathon.repository.RoundRepository;
import hackthon.mangaement.hackathon.repository.TrackRepository;
import hackthon.mangaement.hackathon.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1")
public class JudgeAssignmentController {

    @Autowired
    private JudgeAssignmentRepository judgeAssignmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TrackRepository trackRepository;

    @Autowired
    private RoundRepository roundRepository;

    @PostMapping("/judge-assignments")
    public ResponseEntity<?> assignJudge(@RequestBody Map<String, Object> req,
                                         @AuthenticationPrincipal User coordinator) {
        Integer judgeId = ((Number) req.get("judgeId")).intValue();
        Integer trackId = req.get("trackId") != null ? ((Number) req.get("trackId")).intValue() : null;
        Integer roundId = req.get("roundId") != null ? ((Number) req.get("roundId")).intValue() : null;
        String typeStr = (String) req.get("assignmentType");
        AssignmentType assignmentType = typeStr != null ? AssignmentType.valueOf(typeStr.toUpperCase()) : AssignmentType.NORMAL;

        User judge = userRepository.findById(judgeId)
                .orElseThrow(() -> new ResourceNotFoundException("Judge not found."));

        Track track = null;
        if (trackId != null) {
            track = trackRepository.findById(trackId)
                    .orElseThrow(() -> new ResourceNotFoundException("Track not found."));
        }

        Round round = null;
        if (roundId != null) {
            round = roundRepository.findById(roundId)
                    .orElseThrow(() -> new ResourceNotFoundException("Round not found."));
        }

        // Check if assignment already exists
        Optional<JudgeAssignment> existing = Optional.empty();
        if (track != null) {
            existing = judgeAssignmentRepository.findByJudgeIdAndTrackId(judgeId, trackId);
        } else if (round != null) {
            existing = judgeAssignmentRepository.findByJudgeIdAndRoundId(judgeId, roundId);
        }

        JudgeAssignment assignment;
        if (existing.isPresent()) {
            assignment = existing.get();
            assignment.setAssignmentType(assignmentType);
        } else {
            assignment = JudgeAssignment.builder()
                    .judge(judge)
                    .track(track)
                    .round(round)
                    .assignmentType(assignmentType)
                    .assignedAt(LocalDateTime.now())
                    .assignedBy(coordinator)
                    .build();
        }

        return ResponseEntity.ok(judgeAssignmentRepository.save(assignment));
    }

    @DeleteMapping("/judge-assignments/{id}")
    public ResponseEntity<?> removeJudgeAssignment(@PathVariable Integer id) {
        JudgeAssignment assignment = judgeAssignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Judge assignment not found."));
        judgeAssignmentRepository.delete(assignment);
        return ResponseEntity.ok(Map.of("message", "Judge assignment removed successfully."));
    }

    @GetMapping("/tracks/{trackId}/judges")
    public ResponseEntity<?> getTrackJudges(@PathVariable Integer trackId) {
        List<JudgeAssignment> assignments = judgeAssignmentRepository.findByTrackId(trackId);
        return ResponseEntity.ok(mapAssignments(assignments));
    }

    @GetMapping("/rounds/{roundId}/judges")
    public ResponseEntity<?> getRoundJudges(@PathVariable Integer roundId) {
        List<JudgeAssignment> assignments = judgeAssignmentRepository.findByRoundId(roundId);
        return ResponseEntity.ok(mapAssignments(assignments));
    }

    private List<Map<String, Object>> mapAssignments(List<JudgeAssignment> assignments) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (JudgeAssignment ja : assignments) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", ja.getId());
            map.put("assignmentId", ja.getId());
            map.put("assignmentType", ja.getAssignmentType().name());
            
            Map<String, Object> judgeMap = new HashMap<>();
            judgeMap.put("id", ja.getJudge().getId());
            judgeMap.put("fullName", ja.getJudge().getFullName());
            judgeMap.put("email", ja.getJudge().getEmail());
            map.put("judge", judgeMap);
            
            list.add(map);
        }
        return list;
    }
}
