package hackthon.mangaement.hackathon.service;

import hackthon.mangaement.hackathon.exception.BusinessRuleException;
import hackthon.mangaement.hackathon.exception.ResourceNotFoundException;
import hackthon.mangaement.hackathon.model.Chapter.Chapter;
import hackthon.mangaement.hackathon.model.Chapter.ChapterRanking;
import hackthon.mangaement.hackathon.model.Team.Team;
import hackthon.mangaement.hackathon.model.Team.TeamMember;
import hackthon.mangaement.hackathon.model.User.IndividualRanking;
import hackthon.mangaement.hackathon.model.User.User;
import hackthon.mangaement.hackathon.model.organization.Hackathon;
import hackthon.mangaement.hackathon.model.organization.Prize;
import hackthon.mangaement.hackathon.model.organization.Round;
import hackthon.mangaement.hackathon.model.organization.Track;
import hackthon.mangaement.hackathon.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class RankingService {

    @Autowired
    private HackathonRepository hackathonRepository;

    @Autowired
    private RoundRepository roundRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private PrizeRepository prizeRepository;

    @Autowired
    private ChapterRankingRepository chapterRankingRepository;

    @Autowired
    private IndividualRankingRepository individualRankingRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public Prize awardPrize(Integer trackId, Integer roundId, Integer teamId, String prizeName,
                            Prize.PrizeRank rank, String prizeValue, String description, User coordinator) {
        
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round not found"));

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));

        Track track = null;
        if (trackId != null) {
            track = track.getClass() != null ? null : null; // simplified mapping
        }

        Prize prize = Prize.builder()
                .track(track)
                .round(round)
                .team(team)
                .prizeName(prizeName)
                .prizeRank(rank)
                .prizeValue(prizeValue)
                .description(description)
                .awardedBy(coordinator)
                .build();

        return prizeRepository.save(prize);
    }

    public void publishResults(Integer hackathonId, User coordinator, String ipAddress) {
        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon not found"));

        if (hackathon.getStatus() != Hackathon.Status.PENDING_CONFIRM) {
            throw new BusinessRuleException("Hackathon must be in PENDING_CONFIRM status to publish results.");
        }

        Round finalRound = roundRepository.findByHackathonIdAndIsFinalTrue(hackathonId)
                .orElseThrow(() -> new ResourceNotFoundException("No final round found for this hackathon"));

        if (!finalRound.getScoringLocked()) {
            throw new BusinessRuleException("Final round scoring must be locked before publishing.");
        }

        // 1. Mark status as FINISHED
        hackathon.setStatus(Hackathon.Status.FINISHED);
        hackathon.setUpdatedAt(LocalDateTime.now());
        hackathonRepository.save(hackathon);

        // 2. Fetch Leaderboard for final round
        List<Map<String, Object>> finalLeaderboard = jdbcTemplate.queryForList(
                "SELECT * FROM v_round_leaderboard WHERE round_id = ? ORDER BY weighted_avg_score DESC",
                finalRound.getId());

        // 3. Update Chapter Rankings (cumulative best team score)
        List<Chapter> chapters = chapterRepository.findAll();
        for (Chapter chap : chapters) {
            // Find best team score of this chapter in this hackathon
            double bestScore = 0.0;
            int participatedCount = 0;
            
            List<Team> chapTeams = teamRepository.findByHackathonId(hackathonId).stream()
                    .filter(t -> t.getChapter() != null && t.getChapter().getId().equals(chap.getId()))
                    .toList();
            
            participatedCount = chapTeams.size();
            
            for (Map<String, Object> row : finalLeaderboard) {
                Integer teamId = (Integer) row.get("team_id");
                Team team = teamRepository.findById(teamId).orElse(null);
                if (team != null && team.getChapter() != null && team.getChapter().getId().equals(chap.getId())) {
                    double teamScore = ((Number) row.get("weighted_avg_score")).doubleValue();
                    if (teamScore > bestScore) {
                        bestScore = teamScore;
                    }
                }
            }

            // Find previous ranking to carry forward cumulative score
            double previousCumulative = 0.0;
            // Fetch previous hackathons (e.g. earlier year or semester)
            List<ChapterRanking> pastRankings = chapterRankingRepository.findAll().stream()
                    .filter(cr -> cr.getChapter().getId().equals(chap.getId()) && !cr.getHackathon().getId().equals(hackathonId))
                    .toList();
            if (!pastRankings.isEmpty()) {
                previousCumulative = pastRankings.stream().mapToDouble(ChapterRanking::getTotalScore).sum();
            }

            double newCumulative = previousCumulative + bestScore;

            ChapterRanking ranking = chapterRankingRepository.findByHackathonIdAndChapterId(hackathonId, chap.getId())
                    .orElse(ChapterRanking.builder().hackathon(hackathon).chapter(chap).build());
            
            ranking.setBestTeamScore(bestScore);
            ranking.setTotalScore(newCumulative);
            ranking.setTeamsParticipated(participatedCount);
            ranking.setCalculatedAt(LocalDateTime.now());
            chapterRankingRepository.save(ranking);
        }

        // Re-calculate ranks for Chapter Rankings
        List<ChapterRanking> allChapRankings = chapterRankingRepository.findByHackathonIdOrderByRankAsc(hackathonId);
        // Sort by totalScore DESC
        allChapRankings.sort((c1, c2) -> c2.getTotalScore().compareTo(c1.getTotalScore()));
        for (int i = 0; i < allChapRankings.size(); i++) {
            allChapRankings.get(i).setRank(i + 1);
            chapterRankingRepository.save(allChapRankings.get(i));
        }

        // 4. Update Individual Rankings (if enabled)
        if (hackathon.getIndividualRankingEnabled()) {
            for (Map<String, Object> row : finalLeaderboard) {
                Integer teamId = (Integer) row.get("team_id");
                double teamScore = ((Number) row.get("weighted_avg_score")).doubleValue();
                
                List<TeamMember> members = teamMemberRepository.findByTeamIdAndStatus(teamId, TeamMember.Status.ACCEPTED);
                for (TeamMember tm : members) {
                    User student = tm.getUser();
                    
                    double previousCumulative = 0.0;
                    List<IndividualRanking> pastIndRankings = individualRankingRepository.findAll().stream()
                            .filter(ir -> ir.getUser().getId().equals(student.getId()) && !ir.getHackathon().getId().equals(hackathonId))
                            .toList();
                    if (!pastIndRankings.isEmpty()) {
                        previousCumulative = pastIndRankings.stream().mapToDouble(IndividualRanking::getCumulativeScore).sum();
                    }

                    double newCumulative = previousCumulative + teamScore;

                    IndividualRanking ranking = individualRankingRepository.findByHackathonIdAndUserId(hackathonId, student.getId())
                            .orElse(IndividualRanking.builder().hackathon(hackathon).user(student).build());
                    
                    ranking.setScoreThisHackathon(teamScore);
                    ranking.setCumulativeScore(newCumulative);
                    ranking.setIsEnabled(true);
                    ranking.setCalculatedAt(LocalDateTime.now());
                    individualRankingRepository.save(ranking);
                }
            }

            // Re-calculate ranks for Individual Rankings
            List<IndividualRanking> allIndRankings = individualRankingRepository.findByHackathonIdOrderByRankAsc(hackathonId);
            allIndRankings.sort((i1, i2) -> i2.getCumulativeScore().compareTo(i1.getCumulativeScore()));
            for (int i = 0; i < allIndRankings.size(); i++) {
                allIndRankings.get(i).setRank(i + 1);
                individualRankingRepository.save(allIndRankings.get(i));
            }
        }

        // 5. Send RESULT_PUBLISHED notifications to all participants
        List<Team> allTeams = teamRepository.findByHackathonId(hackathonId);
        for (Team t : allTeams) {
            List<TeamMember> members = teamMemberRepository.findByTeamIdAndStatus(t.getId(), TeamMember.Status.ACCEPTED);
            for (TeamMember tm : members) {
                notificationService.sendNotification(tm.getUser(), "RESULT_PUBLISHED", "Official Results Published!",
                        "The official results and prizes for '" + hackathon.getName() + "' have been published. Go check the leaderboard!", "hackathons", hackathonId);
            }
        }

        // Audit Logging
        Map<String, Object> detail = new HashMap<>();
        detail.put("hackathonId", hackathonId);
        detail.put("newStatus", "FINISHED");
        auditLogService.logAction(coordinator, "HACKATHON_STATUS_CHANGE", "hackathons", hackathonId, detail, ipAddress);
    }

    public String exportAnonymizedRblDataset(Integer hackathonId) {
        // Query v_rbl_anonymized view
        List<Map<String, Object>> dataset = jdbcTemplate.queryForList(
                "SELECT round_id, track_id, criterion_name, criterion_type, judge_type, score_value, scored_at " +
                "FROM v_rbl_anonymized WHERE round_id IN (SELECT id FROM rounds WHERE hackathon_id = ?)",
                hackathonId);

        StringWriter sw = new StringWriter();
        sw.append("round_id,track_id,criterion_name,criterion_type,judge_type,score_value,scored_at\n");

        for (Map<String, Object> row : dataset) {
            sw.append(String.valueOf(row.get("round_id"))).append(",")
              .append(String.valueOf(row.get("track_id"))).append(",")
              .append(escapeCsv((String) row.get("criterion_name"))).append(",")
              .append(String.valueOf(row.get("criterion_type"))).append(",")
              .append(String.valueOf(row.get("judge_type"))).append(",")
              .append(String.valueOf(row.get("score_value"))).append(",")
              .append(String.valueOf(row.get("scored_at"))).append("\n");
        }

        return sw.toString();
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }
}
