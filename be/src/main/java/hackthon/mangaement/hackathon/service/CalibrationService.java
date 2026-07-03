package hackthon.mangaement.hackathon.service;

import hackthon.mangaement.hackathon.exception.ResourceNotFoundException;
import hackthon.mangaement.hackathon.model.User.User;
import hackthon.mangaement.hackathon.model.organization.CalibrationSession;
import hackthon.mangaement.hackathon.model.organization.Score;
import hackthon.mangaement.hackathon.model.organization.Submission;
import hackthon.mangaement.hackathon.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class CalibrationService {

    @Autowired
    private CalibrationSessionRepository calibrationSessionRepository;

    @Autowired
    private ScoreRepository scoreRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CriteriaRepository criteriaRepository;

    public List<Map<String, Object>> getAllSessions(User coordinator) {
        List<CalibrationSession> sessions = calibrationSessionRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (CalibrationSession session : sessions) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", session.getId());
            map.put("roundId", session.getRound().getId());
            map.put("sampleSubmissionId", session.getSampleSubmission() != null ? session.getSampleSubmission().getId() : null);
            map.put("status", session.getStatus().name());
            map.put("targetScore", session.getTargetScore());
            map.put("instructions", session.getInstructions());
            map.put("startedAt", session.getStartedAt());
            map.put("endedAt", session.getEndedAt());
            result.add(map);
        }
        return result;
    }

    public Map<String, Object> getSessionById(Integer id, User user) {
        CalibrationSession session = calibrationSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Calibration session not found"));
        Map<String, Object> map = new HashMap<>();
        map.put("id", session.getId());
        map.put("roundId", session.getRound().getId());
        map.put("sampleSubmissionId", session.getSampleSubmission() != null ? session.getSampleSubmission().getId() : null);
        map.put("status", session.getStatus().name());
        map.put("targetScore", session.getTargetScore());
        map.put("instructions", session.getInstructions());
        map.put("startedAt", session.getStartedAt());
        map.put("endedAt", session.getEndedAt());
        return map;
    }

    public void submitCalibrationScore(Map<String, Object> payload, User judge) {
        Integer submissionId = (Integer) payload.get("submissionId");
        Integer calibrationSessionId = (Integer) payload.get("calibrationSessionId");
        Integer criterionId = (Integer) payload.get("criterionId");
        Double scoreValue = ((Number) payload.get("scoreValue")).doubleValue();
        String comment = (String) payload.get("comment");

        CalibrationSession session = calibrationSessionRepository.findById(calibrationSessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Calibration session not found"));
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found"));

        Optional<Score> existingScore = scoreRepository.findBySubmissionIdAndJudgeIdAndCriterionIdAndScoreType(
                submissionId, judge.getId(), criterionId, Score.ScoreType.CALIBRATION);

        Score score;
        if (existingScore.isPresent()) {
            score = existingScore.get();
            score.setScoreValue(scoreValue);
            score.setComment(comment);
            score.setUpdatedAt(LocalDateTime.now());
        } else {
            score = Score.builder()
                    .submission(submission)
                    .judge(judge)
                    .criterion(criteriaRepository.findById(criterionId).orElseThrow(() -> new ResourceNotFoundException("Criterion not found")))
                    .scoreValue(scoreValue)
                    .comment(comment)
                    .scoreType(Score.ScoreType.CALIBRATION)
                    .calibrationSession(session)
                    .isFinal(false)
                    .scoredAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .version(0)
                    .build();
        }

        scoreRepository.save(score);
    }
}
