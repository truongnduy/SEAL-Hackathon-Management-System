package hackthon.mangaement.hackathon.service;

import hackthon.mangaement.hackathon.exception.BusinessRuleException;
import hackthon.mangaement.hackathon.exception.ResourceNotFoundException;
import hackthon.mangaement.hackathon.model.Judge.JudgeAssignment;
import hackthon.mangaement.hackathon.model.User.User;
import hackthon.mangaement.hackathon.model.organization.Criteria;
import hackthon.mangaement.hackathon.model.organization.Round;
import hackthon.mangaement.hackathon.model.organization.Score;
import hackthon.mangaement.hackathon.model.organization.Submission;
import hackthon.mangaement.hackathon.model.organization.Track;
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

    public Map<String, Object> getScoreBreakdown(Integer roundId, Integer submissionId) {
        Submission sub = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found"));

        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round not found"));

        List<Criteria> criteriaList;
        List<User> assignedJudges = new ArrayList<>();

        if (sub.getTrack() != null) {
            criteriaList = criteriaRepository.findByTrackIdOrderByDisplayOrderAsc(sub.getTrack().getId());
            List<JudgeAssignment> assignments = judgeAssignmentRepository.findByTrackId(sub.getTrack().getId());
            for (JudgeAssignment ja : assignments) {
                if (ja.getJudge() != null) assignedJudges.add(ja.getJudge());
            }
        } else {
            criteriaList = criteriaRepository.findByRoundIdOrderByDisplayOrderAsc(round.getId());
            List<JudgeAssignment> assignments = judgeAssignmentRepository.findByRoundId(round.getId());
            for (JudgeAssignment ja : assignments) {
                if (ja.getJudge() != null) assignedJudges.add(ja.getJudge());
            }
        }

        List<Score> scores = scoreRepository.findBySubmissionIdAndScoreType(submissionId, Score.ScoreType.NORMAL);

        List<Map<String, Object>> cells = new ArrayList<>();
        Map<Integer, List<Double>> criterionScoresMap = new HashMap<>();
        List<Double> allScoresList = new ArrayList<>();

        // Construct judges list with both ID/Name and Ordinal/Label
        List<Map<String, Object>> judgesList = new ArrayList<>();
        Map<Integer, Integer> judgeIdToOrdinal = new HashMap<>();
        int ordinal = 1;
        assignedJudges.sort((j1, j2) -> j1.getId().compareTo(j2.getId()));
        
        for (User j : assignedJudges) {
            Map<String, Object> jMap = new HashMap<>();
            jMap.put("judgeId", j.getId());
            jMap.put("judgeName", j.getFullName());
            jMap.put("judgeEmail", j.getEmail());
            jMap.put("userType", j.getUserType().name());
            jMap.put("ordinal", ordinal);
            jMap.put("label", "Giám khảo " + ordinal);
            
            LocalDateTime lastScored = scores.stream()
                    .filter(s -> s.getJudge().getId().equals(j.getId()))
                    .map(Score::getUpdatedAt)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);
            jMap.put("lastScoredAt", lastScored != null ? lastScored.toString() : null);
            
            judgesList.add(jMap);
            judgeIdToOrdinal.put(j.getId(), ordinal);
            ordinal++;
        }

        for (Score s : scores) {
            Map<String, Object> cell = new HashMap<>();
            cell.put("judgeId", s.getJudge().getId());
            
            Integer ord = judgeIdToOrdinal.get(s.getJudge().getId());
            cell.put("judgeOrdinal", ord != null ? ord : 0);
            
            cell.put("criterionId", s.getCriterion().getId());
            cell.put("scoreValue", s.getScoreValue());
            cell.put("comment", s.getComment());
            cells.add(cell);
            
            criterionScoresMap.computeIfAbsent(s.getCriterion().getId(), k -> new ArrayList<>()).add(s.getScoreValue());
            allScoresList.add(s.getScoreValue());
        }

        List<Map<String, Object>> critList = new ArrayList<>();
        for (Criteria c : criteriaList) {
            Map<String, Object> cMap = new HashMap<>();
            cMap.put("criterionId", c.getId());
            cMap.put("name", c.getName());
            cMap.put("weight", c.getWeight());
            critList.add(cMap);
        }

        List<Map<String, Object>> critStatsList = new ArrayList<>();
        List<Map<String, Object>> critAveragesList = new ArrayList<>();
        for (Criteria c : criteriaList) {
            List<Double> sList = criterionScoresMap.getOrDefault(c.getId(), new ArrayList<>());
            double mean = sList.isEmpty() ? 0.0 : sList.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double variance = calculateVariance(sList, mean);
            
            long gradedJudgesCount = scores.stream()
                    .filter(s -> s.getCriterion().getId().equals(c.getId()))
                    .map(s -> s.getJudge().getId())
                    .distinct()
                    .count();
            int missingCount = Math.max(0, assignedJudges.size() - (int) gradedJudgesCount);

            Map<String, Object> stats = new HashMap<>();
            stats.put("criterionId", c.getId());
            stats.put("mean", mean);
            stats.put("variance", variance);
            stats.put("missingCount", missingCount);
            critStatsList.add(stats);

            Map<String, Object> avg = new HashMap<>();
            avg.put("criterionId", c.getId());
            avg.put("average", mean);
            critAveragesList.add(avg);
        }

        double overallMean = allScoresList.isEmpty() ? 0.0 : allScoresList.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double overallVariance = calculateVariance(allScoresList, overallMean);

        Map<String, Object> response = new HashMap<>();
        response.put("teamName", sub.getTeam() != null ? sub.getTeam().getTeamName() : null);
        response.put("overallMean", overallMean);
        response.put("overallVariance", overallVariance);
        response.put("criteria", critList);
        response.put("judges", judgesList);
        response.put("cells", cells);
        response.put("criterionStats", critStatsList);
        response.put("criterionAverages", critAveragesList);

        return response;
    }

    private double calculateVariance(List<Double> values, double mean) {
        if (values == null || values.size() < 2) return 0.0;
        double sumDiffs = 0.0;
        for (double v : values) {
            sumDiffs += Math.pow(v - mean, 2);
        }
        return sumDiffs / (values.size() - 1);
    }

    public Map<String, Object> getScoreBreakdownAll(Integer roundId, Integer trackId) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round not found"));

        List<Track> tracks = trackRepository.findByRoundIdOrderBySequenceOrderAsc(roundId);
        List<Map<String, Object>> trackSummaries = new ArrayList<>();

        for (Track t : tracks) {
            Map<String, Object> tSum = new HashMap<>();
            tSum.put("trackId", t.getId());
            tSum.put("trackName", t.getName());
            
            List<Submission> tSubs = submissionRepository.findByTrackId(t.getId());
            tSum.put("submissionCount", tSubs.size());

            List<Criteria> tCriteria = criteriaRepository.findByTrackIdOrderByDisplayOrderAsc(t.getId());
            List<JudgeAssignment> tJudges = judgeAssignmentRepository.findByTrackId(t.getId());

            List<Map<String, Object>> progressList = new ArrayList<>();
            for (JudgeAssignment ja : tJudges) {
                if (ja.getJudge() == null) continue;
                User judge = ja.getJudge();
                List<Score> judgeScores = scoreRepository.findByJudgeId(judge.getId());

                int completed = 0;
                for (Submission sub : tSubs) {
                    long scoredCount = judgeScores.stream()
                            .filter(s -> s.getSubmission().getId().equals(sub.getId()) && 
                                         s.getScoreType() == Score.ScoreType.NORMAL &&
                                         tCriteria.stream().anyMatch(c -> c.getId().equals(s.getCriterion().getId())))
                            .count();
                    if (scoredCount >= tCriteria.size() && !tCriteria.isEmpty()) {
                        completed++;
                    }
                }

                Map<String, Object> progress = new HashMap<>();
                progress.put("judgeName", judge.getFullName());
                progress.put("completedCount", completed);
                progress.put("totalCount", tSubs.size());
                progress.put("percentage", tSubs.isEmpty() ? 100.0 : (double) completed / tSubs.size() * 100.0);
                progressList.add(progress);
            }
            tSum.put("judgeProgress", progressList);
            trackSummaries.add(tSum);
        }

        if (tracks.isEmpty()) {
            Map<String, Object> tSum = new HashMap<>();
            tSum.put("trackId", null);
            tSum.put("trackName", "Chung kết");

            List<Submission> rSubs = submissionRepository.findByRoundId(roundId);
            tSum.put("submissionCount", rSubs.size());

            List<Criteria> rCriteria = criteriaRepository.findByRoundIdOrderByDisplayOrderAsc(roundId);
            List<JudgeAssignment> rJudges = judgeAssignmentRepository.findByRoundId(roundId);

            List<Map<String, Object>> progressList = new ArrayList<>();
            for (JudgeAssignment ja : rJudges) {
                if (ja.getJudge() == null) continue;
                User judge = ja.getJudge();
                List<Score> judgeScores = scoreRepository.findByJudgeId(judge.getId());

                int completed = 0;
                for (Submission sub : rSubs) {
                    long scoredCount = judgeScores.stream()
                            .filter(s -> s.getSubmission().getId().equals(sub.getId()) && 
                                         s.getScoreType() == Score.ScoreType.NORMAL &&
                                         rCriteria.stream().anyMatch(c -> c.getId().equals(s.getCriterion().getId())))
                            .count();
                    if (scoredCount >= rCriteria.size() && !rCriteria.isEmpty()) {
                        completed++;
                    }
                }

                Map<String, Object> progress = new HashMap<>();
                progress.put("judgeName", judge.getFullName());
                progress.put("completedCount", completed);
                progress.put("totalCount", rSubs.size());
                progress.put("percentage", rSubs.isEmpty() ? 100.0 : (double) completed / rSubs.size() * 100.0);
                progressList.add(progress);
            }
            tSum.put("judgeProgress", progressList);
            trackSummaries.add(tSum);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("tracks", trackSummaries);

        if (trackId != null) {
            List<Submission> targetSubs;
            List<Criteria> targetCriteria;
            List<User> targetJudges = new ArrayList<>();

            if (trackId == 0) {
                targetSubs = submissionRepository.findByRoundId(roundId);
                targetCriteria = criteriaRepository.findByRoundIdOrderByDisplayOrderAsc(roundId);
                List<JudgeAssignment> assignments = judgeAssignmentRepository.findByRoundId(roundId);
                for (JudgeAssignment ja : assignments) {
                    if (ja.getJudge() != null) targetJudges.add(ja.getJudge());
                }
            } else {
                targetSubs = submissionRepository.findByTrackId(trackId);
                targetCriteria = criteriaRepository.findByTrackIdOrderByDisplayOrderAsc(trackId);
                List<JudgeAssignment> assignments = judgeAssignmentRepository.findByTrackId(trackId);
                for (JudgeAssignment ja : assignments) {
                    if (ja.getJudge() != null) targetJudges.add(ja.getJudge());
                }
            }

            List<Map<String, Object>> criteriaJsonList = new ArrayList<>();
            for (Criteria c : targetCriteria) {
                Map<String, Object> cMap = new HashMap<>();
                cMap.put("criterionId", c.getId());
                cMap.put("name", c.getName());
                cMap.put("weight", c.getWeight());
                criteriaJsonList.add(cMap);
            }

            List<Map<String, Object>> judgesJsonList = new ArrayList<>();
            for (User j : targetJudges) {
                Map<String, Object> jMap = new HashMap<>();
                jMap.put("judgeId", j.getId());
                jMap.put("judgeName", j.getFullName());
                jMap.put("judgeEmail", j.getEmail());
                jMap.put("userType", j.getUserType().name());
                judgesJsonList.add(jMap);
            }

            List<Map<String, Object>> teamsJsonList = new ArrayList<>();
            for (Submission sub : targetSubs) {
                Map<String, Object> teamMap = new HashMap<>();
                teamMap.put("teamId", sub.getTeam() != null ? sub.getTeam().getId() : null);
                teamMap.put("teamName", sub.getTeam() != null ? sub.getTeam().getTeamName() : null);
                teamMap.put("submissionId", sub.getId());

                List<Score> subScores = scoreRepository.findBySubmissionIdAndScoreType(sub.getId(), Score.ScoreType.NORMAL);
                List<Map<String, Object>> cells = new ArrayList<>();
                List<Double> allScores = new ArrayList<>();
                for (Score s : subScores) {
                    Map<String, Object> cell = new HashMap<>();
                    cell.put("judgeId", s.getJudge().getId());
                    cell.put("criterionId", s.getCriterion().getId());
                    cell.put("scoreValue", s.getScoreValue());
                    cell.put("comment", s.getComment());
                    cells.add(cell);
                    allScores.add(s.getScoreValue());
                }
                teamMap.put("cells", cells);

                double overallMean = allScores.isEmpty() ? 0.0 : allScores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                teamMap.put("overallMean", overallMean);

                teamsJsonList.add(teamMap);
            }

            response.put("criteria", criteriaJsonList);
            response.put("judges", judgesJsonList);
            response.put("teams", teamsJsonList);
        }

        return response;
    }
}
