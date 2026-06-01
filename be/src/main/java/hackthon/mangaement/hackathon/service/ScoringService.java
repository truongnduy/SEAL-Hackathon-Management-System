package hackthon.mangaement.hackathon.service;

import hackthon.mangaement.hackathon.exception.BusinessRuleException;
import hackthon.mangaement.hackathon.exception.ResourceNotFoundException;
import hackthon.mangaement.hackathon.model.Judge.JudgeAssignment;
import hackthon.mangaement.hackathon.model.User.User;
import hackthon.mangaement.hackathon.model.organization.Criteria;
import hackthon.mangaement.hackathon.model.organization.Round;
import hackthon.mangaement.hackathon.model.organization.Score;
import hackthon.mangaement.hackathon.model.organization.Submission;
import hackthon.mangaement.hackathon.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class ScoringService {

    @Autowired
    private ScoreRepository scoreRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CriteriaRepository criteriaRepository;

    @Autowired
    private RoundRepository roundRepository;

    @Autowired
    private JudgeAssignmentRepository judgeAssignmentRepository;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public Score submitScore(Integer submissionId, Integer judgeId, Integer criterionId,
                             Double scoreValue, String comment, Score.ScoreType scoreType) {

        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found"));

        if (submission.getStatus() == Submission.Status.REJECTED) {
            throw new BusinessRuleException("Cannot grade a REJECTED submission.");
        }

        User judge = userRepository.findById(judgeId)
                .orElseThrow(() -> new ResourceNotFoundException("Judge not found"));

        Criteria criteria = criteriaRepository.findById(criterionId)
                .orElseThrow(() -> new ResourceNotFoundException("Criteria not found"));

        Round round = submission.getRound() != null ? submission.getRound() : submission.getTrack().getRound();

        if (round.getScoringLocked()) {
            throw new BusinessRuleException("Cannot submit score: Round scoring is locked.");
        }

        // Validate Judge assignment mapping
        if (submission.getTrack() != null) {
            // Prelim: Judge must be assigned to this track
            judgeAssignmentRepository.findByJudgeIdAndTrackId(judgeId, submission.getTrack().getId())
                    .orElseThrow(() -> new BusinessRuleException("Judge is not assigned to this track."));
        } else {
            // Final: Judge must be assigned to this round (external)
            JudgeAssignment assignment = judgeAssignmentRepository.findByJudgeIdAndRoundId(judgeId, round.getId())
                    .orElseThrow(() -> new BusinessRuleException("Judge is not assigned to this final round."));
            if (assignment.getAssignmentType() != JudgeAssignment.AssignmentType.FINAL_EXTERNAL) {
                // Exceptional check: check trigger bypass for dept head
                if (!judge.getIsDeptHead()) {
                    throw new BusinessRuleException("Only EXTERNAL judges (or department heads with confirmed exception) can judge the final round.");
                }
            }
        }

        // Check if score already exists
        Optional<Score> existingScore = scoreRepository.findBySubmissionIdAndJudgeIdAndCriterionIdAndScoreType(
                submissionId, judgeId, criterionId, scoreType);

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
                    .criterion(criteria)
                    .scoreValue(scoreValue)
                    .comment(comment)
                    .scoreType(scoreType)
                    .isFinal(false)
                    .scoredAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
        }

        return scoreRepository.save(score);
    }

    public void lockRoundScoring(Integer roundId, User coordinator, boolean force, String forceReason, String ipAddress) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round not found"));

        if (round.getScoringLocked()) {
            throw new BusinessRuleException("Round scoring is already locked.");
        }

        // Compute completeness:
        // We can fetch progress stats from v_scoring_progress view
        double completionPct = 100.0;
        List<Map<String, Object>> progressList = jdbcTemplate.queryForList(
                "SELECT completion_pct FROM v_scoring_progress WHERE round_id = ?", roundId);
        
        if (!progressList.isEmpty()) {
            Number pct = (Number) progressList.get(0).get("completion_pct");
            if (pct != null) {
                completionPct = pct.doubleValue();
            }
        }

        boolean isComplete = completionPct >= 100.0;

        if (!isComplete && !force) {
            throw new BusinessRuleException("ROUND_LOCK_DENIED: Some judges have not completed grading. Use 'force lock' with a mandatory reason to lock anyway.");
        }

        if (!isComplete && force && (forceReason == null || forceReason.trim().isEmpty())) {
            throw new BusinessRuleException("MISSING_FORCE_LOCK_REASON: A force lock requires a mandatory explanation note.");
        }

        // Lock Round
        round.setScoringLocked(true);
        round.setScoringLockedAt(LocalDateTime.now());
        round.setScoringLockedBy(coordinator);
        round.setForceLocked(force);
        round.setForceLockReason(force ? forceReason : null);
        roundRepository.save(round);

        // Batch set all NORMAL scores in this round to isFinal = true
        List<Submission> submissions = submissionRepository.findByRoundId(roundId);
        if (submissions.isEmpty()) {
            // Also fetch submissions by tracks belonging to this round
            submissions = jdbcTemplate.query(
                    "SELECT s.id FROM submissions s JOIN tracks t ON t.id = s.track_id WHERE t.round_id = ?",
                    (rs, rowNum) -> submissionRepository.findById(rs.getInt("id")).orElse(null),
                    roundId);
        }

        for (Submission s : submissions) {
            if (s != null) {
                List<Score> scores = scoreRepository.findBySubmissionIdAndScoreType(s.getId(), Score.ScoreType.NORMAL);
                for (Score sc : scores) {
                    sc.setIsFinal(true);
                    scoreRepository.save(sc);
                }
            }
        }

        // Audit Logging
        Map<String, Object> detail = new HashMap<>();
        detail.put("roundId", roundId);
        detail.put("forceLocked", force);
        detail.put("forceLockReason", forceReason);
        detail.put("completionPct", completionPct);
        auditLogService.logAction(coordinator, force ? "FORCE_LOCK" : "ROUND_LOCK", "rounds", roundId, detail, ipAddress);
    }

    public List<Map<String, Object>> getScoreVarianceDashboard(Integer roundId) {
        return jdbcTemplate.queryForList(
                "SELECT * FROM v_judge_score_variance WHERE round_id = ?", roundId);
    }

    public List<Map<String, Object>> getScoringProgress(Integer roundId) {
        return jdbcTemplate.queryForList(
                "SELECT * FROM v_scoring_progress WHERE round_id = ?", roundId);
    }
}
