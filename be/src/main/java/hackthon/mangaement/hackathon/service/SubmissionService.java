package hackthon.mangaement.hackathon.service;

import hackthon.mangaement.hackathon.exception.BusinessRuleException;
import hackthon.mangaement.hackathon.exception.ConflictException;
import hackthon.mangaement.hackathon.exception.ResourceNotFoundException;
import hackthon.mangaement.hackathon.model.Team.Team;
import hackthon.mangaement.hackathon.model.User.User;
import hackthon.mangaement.hackathon.model.organization.Round;
import hackthon.mangaement.hackathon.model.organization.Submission;
import hackthon.mangaement.hackathon.model.organization.Track;
import hackthon.mangaement.hackathon.repository.RoundRepository;
import hackthon.mangaement.hackathon.repository.SubmissionRepository;
import hackthon.mangaement.hackathon.repository.TeamRepository;
import hackthon.mangaement.hackathon.repository.TrackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class SubmissionService {

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TrackRepository trackRepository;

    @Autowired
    private RoundRepository roundRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuditLogService auditLogService;

    public Submission submitProject(Integer teamId, Integer trackId, Integer roundId,
                                     String repoUrl, String demoUrl, String reportUrl, String slideUrl,
                                     String lateReason) {

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));

        if (team.getStatus() != Team.Status.ACTIVE) {
            throw new BusinessRuleException("Only ACTIVE teams can submit work.");
        }

        // Validate slide_url presence
        if (slideUrl == null || slideUrl.trim().isEmpty()) {
            throw new BusinessRuleException("Submission failure: Slide presentation URL is mandatory.");
        }

        // Validate XOR constraint
        if ((trackId != null && roundId != null) || (trackId == null && roundId == null)) {
            throw new BusinessRuleException("XOR violation: Must specify either trackId (preliminary) or roundId (final), not both.");
        }

        Round round;
        Track track = null;
        if (trackId != null) {
            track = trackRepository.findById(trackId)
                    .orElseThrow(() -> new ResourceNotFoundException("Track not found"));
            round = track.getRound();
            
            // Check uniqueness constraint
            Optional<Submission> existing = submissionRepository.findByTeamIdAndTrackId(teamId, trackId);
            if (existing.isPresent()) {
                throw new ConflictException("Your team has already submitted a project for this track.");
            }
        } else {
            round = roundRepository.findById(roundId)
                    .orElseThrow(() -> new ResourceNotFoundException("Round not found"));
            
            // Check uniqueness constraint
            Optional<Submission> existing = submissionRepository.findByTeamIdAndRoundId(teamId, roundId);
            if (existing.isPresent()) {
                throw new ConflictException("Your team has already submitted a project for this final round.");
            }
        }

        if (!round.getIsActive()) {
            throw new BusinessRuleException("Cannot submit: This round is not active.");
        }

        LocalDateTime now = LocalDateTime.now();
        boolean isLate = now.isAfter(round.getSubmissionDeadline());
        Submission.Status status = Submission.Status.SUBMITTED;

        if (isLate) {
            if (round.getLateSubmissionPolicy() == Round.LateSubmissionPolicy.HARD_LOCK) {
                throw new BusinessRuleException("LATE_SUBMISSION_BLOCKED: The submission deadline has passed and late submissions are locked for this round.");
            } else {
                status = Submission.Status.LATE;
            }
        }

        Submission submission = Submission.builder()
                .team(team)
                .track(track)
                .round(round)
                .repoUrl(repoUrl)
                .demoUrl(demoUrl)
                .reportUrl(reportUrl)
                .slideUrl(slideUrl)
                .isLate(isLate)
                .lateReason(isLate ? lateReason : null)
                .status(status)
                .submittedAt(now)
                .build();

        return submissionRepository.save(submission);
    }
}
