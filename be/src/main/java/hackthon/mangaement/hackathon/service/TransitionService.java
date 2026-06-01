package hackthon.mangaement.hackathon.service;

import hackthon.mangaement.hackathon.exception.BusinessRuleException;
import hackthon.mangaement.hackathon.exception.ResourceNotFoundException;
import hackthon.mangaement.hackathon.model.Team.Team;
import hackthon.mangaement.hackathon.model.Team.WildcardReview;
import hackthon.mangaement.hackathon.model.User.User;
import hackthon.mangaement.hackathon.model.organization.Hackathon;
import hackthon.mangaement.hackathon.model.organization.Round;
import hackthon.mangaement.hackathon.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class TransitionService {

    @Autowired
    private RoundRepository roundRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private ScoreRepository scoreRepository;

    @Autowired
    private WildcardReviewRepository wildcardReviewRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public static class RoundPromotionDetails {
        public List<Map<String, Object>> advancers;
        public List<Map<String, Object>> ties;
        public List<Map<String, Object>> wildcardSuggestions;
    }

    public RoundPromotionDetails getTransitionDetails(Integer roundId) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round not found"));

        if (!round.getScoringLocked()) {
            throw new BusinessRuleException("Round scoring must be locked before calculating results.");
        }

        // Fetch leaderboard from DB view
        List<Map<String, Object>> leaderboard = jdbcTemplate.queryForList(
                "SELECT * FROM v_round_leaderboard WHERE round_id = ? ORDER BY track_id, assigned_group, weighted_avg_score DESC",
                roundId);

        // Group by (track_id, assigned_group)
        Map<String, List<Map<String, Object>>> brackets = new HashMap<>();
        for (Map<String, Object> row : leaderboard) {
            Integer trackId = (Integer) row.get("track_id");
            String group = (String) row.get("assigned_group");
            String key = trackId + ":" + (group != null ? group : "");
            brackets.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
        }

        List<Map<String, Object>> advancers = new ArrayList<>();
        List<Map<String, Object>> ties = new ArrayList<>();
        List<Map<String, Object>> nonAdvancers = new ArrayList<>();

        int topN = round.getTopNAdvance() != null ? round.getTopNAdvance() : 2;

        for (Map.Entry<String, List<Map<String, Object>>> entry : brackets.entrySet()) {
            List<Map<String, Object>> teams = entry.getValue();
            
            // Sort by score DESC
            teams.sort((t1, t2) -> {
                Double s1 = ((Number) t1.get("weighted_avg_score")).doubleValue();
                Double s2 = ((Number) t2.get("weighted_avg_score")).doubleValue();
                return s2.compareTo(s1);
            });

            if (teams.size() <= topN) {
                advancers.addAll(teams);
                continue;
            }

            // Find threshold score
            Map<String, Object> cutTeam = teams.get(topN - 1);
            Double cutScore = ((Number) cutTeam.get("weighted_avg_score")).doubleValue();

            // Check if there is a tie at the boundary
            List<Map<String, Object>> boundaryTies = teams.stream()
                    .filter(t -> Math.abs(((Number) t.get("weighted_avg_score")).doubleValue() - cutScore) < 0.0001)
                    .collect(Collectors.toList());

            if (boundaryTies.size() == 1) {
                // No tie, topN just advance
                advancers.addAll(teams.subList(0, topN));
                nonAdvancers.addAll(teams.subList(topN, teams.size()));
            } else {
                // There is a tie at the border (e.g. 2nd place and 3rd place have same score)
                // Filter which ones are strictly above the cutScore (they advance automatically)
                List<Map<String, Object>> strictAdvancers = teams.stream()
                        .filter(t -> ((Number) t.get("weighted_avg_score")).doubleValue() > cutScore)
                        .collect(Collectors.toList());

                advancers.addAll(strictAdvancers);

                // The tied ones go to the ties list for tiebreaking
                int slotsLeft = topN - strictAdvancers.size();
                for (Map<String, Object> t : boundaryTies) {
                    t.put("slots_available", slotsLeft);
                    ties.add(t);
                }

                // Strictly below cutScore
                List<Map<String, Object>> strictlyBelow = teams.stream()
                        .filter(t -> ((Number) t.get("weighted_avg_score")).doubleValue() < cutScore)
                        .collect(Collectors.toList());
                nonAdvancers.addAll(strictlyBelow);
            }
        }

        // Wild Card Suggestions:
        // Sort nonAdvancers by score DESC cross-bracket
        nonAdvancers.sort((t1, t2) -> {
            Double s1 = ((Number) t1.get("weighted_avg_score")).doubleValue();
            Double s2 = ((Number) t2.get("weighted_avg_score")).doubleValue();
            return s2.compareTo(s1);
        });

        int totalAdvancers = advancers.size();
        int minFinal = round.getMinTeamsFinal() != null ? round.getMinTeamsFinal() : 6;
        int wildcardSlots = Math.max(0, minFinal - totalAdvancers);

        List<Map<String, Object>> wildcardSuggestions = new ArrayList<>();
        if (wildcardSlots > 0 && round.getWildcardEnabled()) {
            wildcardSuggestions.addAll(nonAdvancers.subList(0, Math.min(wildcardSlots, nonAdvancers.size())));
        }

        RoundPromotionDetails details = new RoundPromotionDetails();
        details.advancers = advancers;
        details.ties = ties;
        details.wildcardSuggestions = wildcardSuggestions;
        return details;
    }

    public void confirmRoundTransition(Integer roundId, List<Integer> advancingTeamIds, User coordinator, String ipAddress) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round not found"));

        if (!round.getScoringLocked()) {
            throw new BusinessRuleException("Round scoring must be locked before publishing transition.");
        }

        Hackathon hackathon = round.getHackathon();

        // Get all active teams in this round
        List<Map<String, Object>> leaderboard = jdbcTemplate.queryForList(
                "SELECT DISTINCT team_id FROM v_round_leaderboard WHERE round_id = ?", roundId);

        Set<Integer> currentRoundTeamIds = leaderboard.stream()
                .map(r -> (Integer) r.get("team_id"))
                .collect(Collectors.toSet());

        Set<Integer> advancingSet = new HashSet<>(advancingTeamIds);

        // Update team statuses
        for (Integer teamId : currentRoundTeamIds) {
            Team team = teamRepository.findById(teamId).orElse(null);
            if (team != null) {
                if (advancingSet.contains(teamId)) {
                    // Stays ACTIVE
                    team.setStatus(Team.Status.ACTIVE);
                    notificationService.sendNotification(team.getLeader(), "RESULT_PUBLISHED", "Advanced to Final Round!",
                            "Congratulations! Your team '" + team.getTeamName() + "' has advanced to the Final Round.", "rounds", roundId);
                } else {
                    // ELIMINATED
                    team.setStatus(Team.Status.ELIMINATED);
                    team.setEliminatedAt(LocalDateTime.now());
                    team.setEliminationReason("Did not advance from preliminary round.");
                    teamRepository.save(team);

                    notificationService.sendNotification(team.getLeader(), "RESULT_PUBLISHED", "Preliminary Round Results",
                            "Your team '" + team.getTeamName() + "' did not advance to the Final Round. Thank you for participating!", "rounds", roundId);
                }
            }
        }

        // If wildcard reviews exist, mark them
        for (Integer teamId : advancingSet) {
            if (!currentRoundTeamIds.contains(teamId)) {
                // Team is a wildcard
                Team team = teamRepository.findById(teamId).orElse(null);
                if (team != null) {
                    team.setStatus(Team.Status.ACTIVE);
                    teamRepository.save(team);
                    
                    WildcardReview review = WildcardReview.builder()
                            .round(round)
                            .team(team)
                            .coordinatorApproved(true)
                            .coordinatorNote("Promoted as Wildcard")
                            .reviewedBy(coordinator)
                            .reviewedAt(LocalDateTime.now())
                            .build();
                    wildcardReviewRepository.save(review);

                    notificationService.sendNotification(team.getLeader(), "RESULT_PUBLISHED", "Advanced via Wildcard!",
                            "Congratulations! Your team '" + team.getTeamName() + "' has advanced to the Final Round via a Wildcard.", "rounds", roundId);
                }
            }
        }

        // Activate the next round (Chung kết)
        List<Round> rounds = roundRepository.findByHackathonIdOrderBySequenceOrderAsc(hackathon.getId());
        Round nextRound = null;
        for (Round r : rounds) {
            if (r.getSequenceOrder() > round.getSequenceOrder()) {
                nextRound = r;
                break;
            }
        }

        if (nextRound != null) {
            nextRound.setIsActive(true);
            roundRepository.save(nextRound);

            // Send notification to judges and teams
            List<User> participants = userRepository.findAll(); // simplified
            for (User u : participants) {
                if (u.getRole() == User.Role.JUDGE || u.getRole() == User.Role.STUDENT) {
                    notificationService.sendNotification(u, "ROUND_STARTED", "Round Started",
                            "The " + nextRound.getName() + " has officially started. Submissions are now open.", "rounds", nextRound.getId());
                }
            }
        }

        // Audit Logging
        Map<String, Object> detail = new HashMap<>();
        detail.put("roundId", roundId);
        detail.put("advancingTeamsCount", advancingTeamIds.size());
        auditLogService.logAction(coordinator, "ROUND_TRANSITION_CONFIRMED", "rounds", roundId, detail, ipAddress);
    }
}
