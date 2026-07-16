package hackthon.mangaement.hackathon.service;

import hackthon.mangaement.hackathon.exception.ResourceNotFoundException;
import hackthon.mangaement.hackathon.model.User.User;
import hackthon.mangaement.hackathon.model.organization.ExportJob;
import hackthon.mangaement.hackathon.model.organization.Hackathon;
import hackthon.mangaement.hackathon.repository.ExportJobRepository;
import hackthon.mangaement.hackathon.repository.HackathonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@Transactional
public class ExportJobService {

    @Autowired
    private ExportJobRepository exportJobRepository;

    @Autowired
    private HackathonRepository hackathonRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public ExportJob createExportJob(Integer hackathonId, String typeStr, User requestedBy) {
        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon not found"));

        ExportJob.Type type = ExportJob.Type.valueOf(typeStr.toUpperCase());

        ExportJob job = ExportJob.builder()
                .hackathon(hackathon)
                .type(type)
                .status(ExportJob.Status.PENDING)
                .requestedBy(requestedBy)
                .createdAt(LocalDateTime.now())
                .build();

        job = exportJobRepository.save(job);

        // Run the export task asynchronously
        final Integer jobId = job.getId();
        CompletableFuture.runAsync(() -> executeExport(jobId));

        return job;
    }

    public ExportJob getExportJob(Integer id) {
        return exportJobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Export job not found"));
    }

    public File getExportFile(Integer id) {
        ExportJob job = getExportJob(id);
        if (job.getStatus() != ExportJob.Status.DONE) {
            throw new IllegalStateException("Export job is not done yet.");
        }
        File file = new File("exports", "export_job_" + id + ".csv");
        if (!file.exists()) {
            throw new ResourceNotFoundException("Exported file not found on disk.");
        }
        return file;
    }

    private void executeExport(Integer jobId) {
        // We run in a new transaction context since it's asynchronous
        ExportJob job = exportJobRepository.findById(jobId).orElse(null);
        if (job == null) return;

        try {
            job.setStatus(ExportJob.Status.PROCESSING);
            job.setStartedAt(LocalDateTime.now());
            exportJobRepository.saveAndFlush(job);

            File exportsDir = new File("exports");
            if (!exportsDir.exists()) {
                exportsDir.mkdirs();
            }

            File file = new File(exportsDir, "export_job_" + jobId + ".csv");
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                switch (job.getType()) {
                    case CSV_SCORES:
                        generateCsvScores(job.getHackathon().getId(), writer);
                        break;
                    case CSV_RANKINGS:
                        generateCsvRankings(job.getHackathon().getId(), writer);
                        break;
                    case ANONYMIZED_RBL:
                        generateAnonymizedRbl(job.getHackathon().getId(), writer);
                        break;
                    case FULL_REPORT:
                        generateFullReport(job.getHackathon().getId(), writer);
                        break;
                }
            }

            job.setStatus(ExportJob.Status.DONE);
            job.setFileUrl("/api/v1/export-jobs/" + jobId + "/download");
            job.setFinishedAt(LocalDateTime.now());
            exportJobRepository.saveAndFlush(job);
        } catch (Exception e) {
            job.setStatus(ExportJob.Status.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setFinishedAt(LocalDateTime.now());
            exportJobRepository.saveAndFlush(job);
        }
    }

    private void generateCsvScores(Integer hackathonId, PrintWriter writer) {
        writer.println("\"Team Name\",\"Round\",\"Track\",\"Criteria\",\"Max Score\",\"Score Value\",\"Comment\",\"Judge Name\",\"Score Type\"");
        String sql = "SELECT t.team_name, r.name AS round_name, tr.name AS track_name, c.name AS criteria_name, c.max_score, s.score_value, s.comment, u.full_name, s.score_type " +
                     "FROM scores s " +
                     "JOIN submissions sub ON s.submission_id = sub.id " +
                     "JOIN teams t ON sub.team_id = t.id " +
                     "LEFT JOIN tracks tr ON sub.track_id = tr.id " +
                     "LEFT JOIN rounds r ON sub.round_id = r.id OR tr.round_id = r.id " +
                     "JOIN criteria c ON s.criteria_id = c.id " +
                     "JOIN users u ON s.judge_id = u.id " +
                     "WHERE r.hackathon_id = ?";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, hackathonId);
        for (Map<String, Object> row : rows) {
            writer.println(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s",
                    escapeCsv(row.get("team_name")),
                    escapeCsv(row.get("round_name")),
                    escapeCsv(row.get("track_name")),
                    escapeCsv(row.get("criteria_name")),
                    escapeCsv(row.get("max_score")),
                    escapeCsv(row.get("score_value")),
                    escapeCsv(row.get("comment")),
                    escapeCsv(row.get("full_name")),
                    escapeCsv(row.get("score_type"))
            ));
        }
    }

    private void generateCsvRankings(Integer hackathonId, PrintWriter writer) {
        writer.println("\"Round ID\",\"Track ID\",\"Team ID\",\"Team Name\",\"Assigned Group\",\"Judge Count\",\"Weighted Avg Score\"");
        String sql = "SELECT round_id, track_id, team_id, team_name, assigned_group, judge_count, weighted_avg_score " +
                     "FROM v_round_leaderboard " +
                     "WHERE round_id IN (SELECT id FROM rounds WHERE hackathon_id = ?) " +
                     "ORDER BY weighted_avg_score DESC";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, hackathonId);
        for (Map<String, Object> row : rows) {
            writer.println(String.format("%s,%s,%s,%s,%s,%s,%s",
                    escapeCsv(row.get("round_id")),
                    escapeCsv(row.get("track_id")),
                    escapeCsv(row.get("team_id")),
                    escapeCsv(row.get("team_name")),
                    escapeCsv(row.get("assigned_group")),
                    escapeCsv(row.get("judge_count")),
                    escapeCsv(row.get("weighted_avg_score"))
            ));
        }
    }

    private void generateAnonymizedRbl(Integer hackathonId, PrintWriter writer) {
        writer.println("\"Round ID\",\"Track ID\",\"Criterion ID\",\"Criterion Name\",\"Criterion Type\",\"Judge Type\",\"Score Value\"");
        String sql = "SELECT round_id, track_id, criterion_id, criterion_name, criterion_type, judge_type, score_value " +
                     "FROM v_rbl_anonymized " +
                     "WHERE round_id IN (SELECT id FROM rounds WHERE hackathon_id = ?)";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, hackathonId);
        for (Map<String, Object> row : rows) {
            writer.println(String.format("%s,%s,%s,%s,%s,%s,%s",
                    escapeCsv(row.get("round_id")),
                    escapeCsv(row.get("track_id")),
                    escapeCsv(row.get("criterion_id")),
                    escapeCsv(row.get("criterion_name")),
                    escapeCsv(row.get("criterion_type")),
                    escapeCsv(row.get("judge_type")),
                    escapeCsv(row.get("score_value"))
            ));
        }
    }

    private void generateFullReport(Integer hackathonId, PrintWriter writer) {
        writer.println("\"Section\",\"Key Description\",\"Value\"");
        
        // Count rounds
        Integer roundsCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM rounds WHERE hackathon_id = ?", Integer.class, hackathonId);
        writer.println(String.format("\"Rounds Count\",\"Number of rounds in hackathon\",%d", roundsCount));
        
        // Count teams
        Integer teamsCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM teams WHERE status = 'ACTIVE'", Integer.class);
        writer.println(String.format("\"Active Teams Count\",\"Total active teams in system\",%d", teamsCount));
        
        // Count submissions
        Integer submissionsCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM submissions s JOIN rounds r ON s.round_id = r.id OR s.track_id IN (SELECT id FROM tracks WHERE round_id = r.id) WHERE r.hackathon_id = ?", 
                Integer.class, hackathonId);
        writer.println(String.format("\"Submissions Count\",\"Total submissions in hackathon\",%d", submissionsCount));
    }

    private String escapeCsv(Object val) {
        if (val == null) return "";
        String s = val.toString();
        if (s.contains("\"") || s.contains(",") || s.contains("\n") || s.contains("\r")) {
            s = s.replace("\"", "\"\"");
            return "\"" + s + "\"";
        }
        return s;
    }
}
