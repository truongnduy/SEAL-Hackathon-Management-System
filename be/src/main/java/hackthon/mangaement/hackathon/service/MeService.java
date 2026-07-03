package hackthon.mangaement.hackathon.service;

import hackthon.mangaement.hackathon.exception.ResourceNotFoundException;
import hackthon.mangaement.hackathon.model.Judge.JudgeAssignment;
import hackthon.mangaement.hackathon.model.User.User;
import hackthon.mangaement.hackathon.model.organization.Hackathon;
import hackthon.mangaement.hackathon.model.organization.Submission;
import hackthon.mangaement.hackathon.repository.HackathonRepository;
import hackthon.mangaement.hackathon.repository.JudgeAssignmentRepository;
import hackthon.mangaement.hackathon.repository.SubmissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class MeService {

    @Autowired
    private HackathonRepository hackathonRepository;

    @Autowired
    private JudgeAssignmentRepository judgeAssignmentRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    public List<Map<String, Object>> getBrowseableHackathons(User user) {
        List<Hackathon> hackathons = hackathonRepository.findByStatus(Hackathon.Status.ONGOING);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Hackathon h : hackathons) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", h.getId());
            map.put("name", h.getName());
            map.put("slug", h.getSlug());
            map.put("description", h.getDescription());
            map.put("registrationStart", h.getRegistrationStart());
            map.put("registrationEnd", h.getRegistrationEnd());
            map.put("eventStart", h.getEventStart());
            map.put("eventEnd", h.getEventEnd());
            result.add(map);
        }
        return result;
    }

    public void registerForHackathon(Integer hackathonId, User user) {
        if (user.getRole() != User.Role.STUDENT) {
            throw new hackthon.mangaement.hackathon.exception.BusinessRuleException("Only students can register for hackathons.");
        }
        Hackathon h = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon not found"));
        if (h.getStatus() != Hackathon.Status.ONGOING) {
            throw new hackthon.mangaement.hackathon.exception.BusinessRuleException("Registration is not open for this hackathon.");
        }
        System.out.println("User " + user.getFullName() + " registered check for hackathon " + hackathonId);
    }

    public List<Map<String, Object>> getJudgeSubmissions(User judge) {
        List<JudgeAssignment> assignments = judgeAssignmentRepository.findByJudgeId(judge.getId());
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (JudgeAssignment ja : assignments) {
            List<Submission> submissions = new ArrayList<>();
            if (ja.getTrack() != null) {
                submissions = submissionRepository.findByTrackId(ja.getTrack().getId());
            } else if (ja.getRound() != null) {
                submissions = submissionRepository.findByRoundId(ja.getRound().getId());
            }
            
            for (Submission s : submissions) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", s.getId());
                map.put("teamId", s.getTeam().getId());
                map.put("teamName", s.getTeam().getTeamName());
                map.put("trackId", s.getTrack() != null ? s.getTrack().getId() : null);
                map.put("roundId", s.getRound() != null ? s.getRound().getId() : null);
                map.put("repoUrl", s.getRepoUrl());
                map.put("demoUrl", s.getDemoUrl());
                map.put("reportUrl", s.getReportUrl());
                map.put("slideUrl", s.getSlideUrl());
                map.put("status", s.getStatus().name());
                map.put("submittedAt", s.getSubmittedAt());
                result.add(map);
            }
        }
        return result;
    }
}
