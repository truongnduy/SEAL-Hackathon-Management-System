package hackthon.mangaement.hackathon.service;

import hackthon.mangaement.hackathon.exception.BusinessRuleException;
import hackthon.mangaement.hackathon.exception.ResourceNotFoundException;
import hackthon.mangaement.hackathon.model.Judge.JudgeAssignment;
import hackthon.mangaement.hackathon.model.Mentor.MentorAssignment;
import hackthon.mangaement.hackathon.model.User.User;
import hackthon.mangaement.hackathon.model.organization.Criteria;
import hackthon.mangaement.hackathon.model.organization.Event;
import hackthon.mangaement.hackathon.model.organization.Hackathon;
import hackthon.mangaement.hackathon.model.organization.Round;
import hackthon.mangaement.hackathon.model.organization.Track;
import hackthon.mangaement.hackathon.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class HackathonService {

    @Autowired
    private HackathonRepository hackathonRepository;

    @Autowired
    private RoundRepository roundRepository;

    @Autowired
    private TrackRepository trackRepository;

    @Autowired
    private CriteriaRepository criteriaRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private JudgeAssignmentRepository judgeAssignmentRepository;

    @Autowired
    private MentorAssignmentRepository mentorAssignmentRepository;

    @Autowired
    private AuditLogService auditLogService;

    public Hackathon createHackathon(String name, String slug, Hackathon.Season season, Integer year,
                                     String description, String rules, Boolean wildcardEnabled,
                                     Boolean individualRankingEnabled, User coordinator) {
        Hackathon hackathon = Hackathon.builder()
                .name(name)
                .slug(slug)
                .season(season)
                .year(year)
                .status(Hackathon.Status.DRAFT)
                .description(description)
                .rules(rules)
                .wildcardEnabled(wildcardEnabled)
                .individualRankingEnabled(individualRankingEnabled)
                .createdBy(coordinator)
                .build();
        return hackathonRepository.save(hackathon);
    }

    public Round createRound(Integer hackathonId, String name, Integer sequenceOrder, Boolean isFinal,
                             Round.RoundType roundType, Integer codingDurationHours, LocalDateTime deadline,
                             Round.LateSubmissionPolicy latePolicy, String problemUrl, Integer topNAdvance,
                             Integer minTeamsFinal, Boolean wildcardEnabled, Round.TiebreakRule tiebreakRule) {
        
        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon not found"));

        Round round = Round.builder()
                .hackathon(hackathon)
                .name(name)
                .sequenceOrder(sequenceOrder)
                .isFinal(isFinal)
                .roundType(roundType)
                .codingDurationHours(codingDurationHours)
                .submissionDeadline(deadline)
                .lateSubmissionPolicy(latePolicy)
                .problemStatementUrl(problemUrl)
                .topNAdvance(topNAdvance)
                .minTeamsFinal(minTeamsFinal)
                .wildcardEnabled(wildcardEnabled)
                .tiebreakRule(tiebreakRule)
                .build();

        return roundRepository.save(round);
    }

    public Track createTrack(Integer roundId, String name, String description, String topic,
                             Integer maxTeams, Integer maxTeamsPerGroup, Integer minSize, Integer maxSize,
                             Integer sequenceOrder) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round not found"));

        Track track = Track.builder()
                .round(round)
                .name(name)
                .description(description)
                .topic(topic)
                .maxTeams(maxTeams)
                .maxTeamsPerGroup(maxTeamsPerGroup)
                .minTeamSize(minSize)
                .maxTeamSize(maxSize)
                .sequenceOrder(sequenceOrder)
                .build();

        return trackRepository.save(track);
    }

    public Criteria createCriteria(Integer trackId, Integer roundId, String name, Criteria.CriteriaType type,
                                   Double weight, Integer maxScore, String description, String rubricUrl,
                                   Integer displayOrder) {
        Track track = null;
        if (trackId != null) {
            track = trackRepository.findById(trackId)
                    .orElseThrow(() -> new ResourceNotFoundException("Track not found"));
        }

        Round round = null;
        if (roundId != null) {
            round = roundRepository.findById(roundId)
                    .orElseThrow(() -> new ResourceNotFoundException("Round not found"));
        }

        Criteria criteria = Criteria.builder()
                .track(track)
                .round(round)
                .name(name)
                .type(type)
                .weight(weight)
                .maxScore(maxScore)
                .description(description)
                .rubricUrl(rubricUrl)
                .displayOrder(displayOrder)
                .build();

        return criteriaRepository.save(criteria);
    }

    public JudgeAssignment assignJudge(User judge, Track track, Round round, JudgeAssignment.AssignmentType type, User coordinator) {
        JudgeAssignment assignment = JudgeAssignment.builder()
                .judge(judge)
                .track(track)
                .round(round)
                .assignmentType(type)
                .assignedBy(coordinator)
                .build();
        return judgeAssignmentRepository.save(assignment);
    }

    public MentorAssignment assignMentor(User mentor, Track track, User coordinator) {
        // Validation check for conflict Mentor = Judge in same Track
        if (judgeAssignmentRepository.findByJudgeIdAndTrackId(mentor.getId(), track.getId()).isPresent()) {
            throw new BusinessRuleException("CONFLICT_SAME_TRACK: A user cannot be assigned as a Judge and a Mentor in the same Track.");
        }

        MentorAssignment assignment = MentorAssignment.builder()
                .mentor(mentor)
                .track(track)
                .assignedBy(coordinator)
                .build();
        return mentorAssignmentRepository.save(assignment);
    }

    public Event createEvent(Integer hackathonId, String title, Event.EventType type, String description,
                             String location, String meetUrl, LocalDateTime startsAt, LocalDateTime endsAt,
                             User coordinator) {
        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon not found"));

        Event event = Event.builder()
                .hackathon(hackathon)
                .title(title)
                .type(type)
                .description(description)
                .location(location)
                .meetUrl(meetUrl)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .createdBy(coordinator)
                .build();
        return eventRepository.save(event);
    }

    public void transitionToOngoing(Integer hackathonId, User coordinator, String ipAddress) {
        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon not found"));

        if (hackathon.getStatus() != Hackathon.Status.DRAFT) {
            throw new BusinessRuleException("Only DRAFT hackathons can transition to ONGOING.");
        }

        List<String> errors = new ArrayList<>();

        // Fetch Rounds
        List<Round> rounds = roundRepository.findByHackathonIdOrderBySequenceOrderAsc(hackathonId);
        
        // 1. Validate: At least 1 Round PRELIMINARY with child tracks
        boolean hasPrelimWithTracks = false;
        long finalRoundCount = 0;
        Round finalRound = null;

        for (Round r : rounds) {
            if (r.getRoundType() == Round.RoundType.PRELIMINARY || r.getRoundType() == Round.RoundType.SEMIFINAL) {
                List<Track> tracks = trackRepository.findByRoundIdOrderBySequenceOrderAsc(r.getId());
                if (!tracks.isEmpty()) {
                    hasPrelimWithTracks = true;
                }
            } else if (r.getIsFinal() && r.getRoundType() == Round.RoundType.FINAL) {
                finalRoundCount++;
                finalRound = r;
            }
        }

        if (!hasPrelimWithTracks) {
            errors.add("Gatekeeper failure: Hackathon must have at least one preliminary or semifinal round containing child tracks.");
        }

        // 2. Validate: Exactly 1 Round FINAL
        if (finalRoundCount != 1) {
            errors.add("Gatekeeper failure: Hackathon must contain exactly one FINAL round.");
        }

        // 3. Validate: Every track's criteria weights sum to 1.0
        for (Round r : rounds) {
            if (r.getRoundType() == Round.RoundType.PRELIMINARY || r.getRoundType() == Round.RoundType.SEMIFINAL) {
                List<Track> tracks = trackRepository.findByRoundIdOrderBySequenceOrderAsc(r.getId());
                for (Track t : tracks) {
                    List<Criteria> criteria = criteriaRepository.findByTrackIdOrderByDisplayOrderAsc(t.getId());
                    if (criteria.isEmpty()) {
                        errors.add("Track '" + t.getName() + "' in Round '" + r.getName() + "' has no evaluation criteria.");
                    } else {
                        double totalWeight = criteria.stream()
                                .filter(c -> c.getType() != Criteria.CriteriaType.PENALTY)
                                .mapToDouble(Criteria::getWeight)
                                .sum();
                        if (Math.abs(totalWeight - 1.0) > 0.001) {
                            errors.add("Track '" + t.getName() + "' criteria weights must sum to exactly 1.0 (currently " + totalWeight + ").");
                        }
                    }
                }
            }
        }

        // 4. Validate: Round FINAL criteria weights sum to 1.0
        if (finalRound != null) {
            List<Criteria> finalCriteria = criteriaRepository.findByRoundIdOrderByDisplayOrderAsc(finalRound.getId());
            if (finalCriteria.isEmpty()) {
                errors.add("Final round '" + finalRound.getName() + "' has no evaluation criteria.");
            } else {
                double totalWeight = finalCriteria.stream()
                        .filter(c -> c.getType() != Criteria.CriteriaType.PENALTY)
                        .mapToDouble(Criteria::getWeight)
                        .sum();
                if (Math.abs(totalWeight - 1.0) > 0.001) {
                    errors.add("Final round criteria weights must sum to exactly 1.0 (currently " + totalWeight + ").");
                }
            }
        }

        // 5. Validate: At least 1 event of type KICKOFF
        List<Event> events = eventRepository.findByHackathonIdOrderByStartsAtAsc(hackathonId);
        boolean hasKickoff = events.stream().anyMatch(e -> e.getType() == Event.EventType.KICKOFF);
        if (!hasKickoff) {
            errors.add("Gatekeeper failure: Hackathon must schedule at least one KICKOFF event.");
        }

        // Validate event time sequencing: WORKSHOP < KICKOFF < PRESENTATION < AWARDS
        Event workshop = null;
        Event kickoff = null;
        Event presentation = null;
        Event awards = null;

        for (Event e : events) {
            if (e.getType() == Event.EventType.WORKSHOP) workshop = e;
            else if (e.getType() == Event.EventType.KICKOFF) kickoff = e;
            else if (e.getType() == Event.EventType.PRESENTATION) presentation = e;
            else if (e.getType() == Event.EventType.AWARDS) awards = e;
        }

        if (workshop != null && kickoff != null && workshop.getStartsAt().isAfter(kickoff.getStartsAt())) {
            errors.add("Event sequencing violation: WORKSHOP must be scheduled before KICKOFF.");
        }
        if (kickoff != null && presentation != null && kickoff.getStartsAt().isAfter(presentation.getStartsAt())) {
            errors.add("Event sequencing violation: KICKOFF must be scheduled before PRESENTATION.");
        }
        if (presentation != null && awards != null && presentation.getStartsAt().isAfter(awards.getStartsAt())) {
            errors.add("Event sequencing violation: PRESENTATION must be scheduled before AWARDS.");
        }

        if (!errors.isEmpty()) {
            throw new BusinessRuleException("Gatekeeper check failed: " + String.join(" | ", errors));
        }

        hackathon.setStatus(Hackathon.Status.ONGOING);
        hackathonRepository.save(hackathon);

        // Audit Logging
        Map<String, Object> detail = new HashMap<>();
        detail.put("hackathonId", hackathonId);
        detail.put("newStatus", "ONGOING");
        auditLogService.logAction(coordinator, "HACKATHON_STATUS_CHANGE", "hackathons", hackathonId, detail, ipAddress);
    }
}
