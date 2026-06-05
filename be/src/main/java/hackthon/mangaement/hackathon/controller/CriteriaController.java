 package hackthon.mangaement.hackathon.controller;

import hackthon.mangaement.hackathon.exception.ResourceNotFoundException;
import hackthon.mangaement.hackathon.model.organization.Criteria;
import hackthon.mangaement.hackathon.model.organization.Criteria.CriteriaType;
import hackthon.mangaement.hackathon.model.organization.Round;
import hackthon.mangaement.hackathon.model.organization.Track;
import hackthon.mangaement.hackathon.repository.CriteriaRepository;
import hackthon.mangaement.hackathon.repository.RoundRepository;
import hackthon.mangaement.hackathon.repository.TrackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
@Transactional
public class CriteriaController {

    @Autowired
    private CriteriaRepository criteriaRepository;

    @Autowired
    private TrackRepository trackRepository;

    @Autowired
    private RoundRepository roundRepository;

    @GetMapping("/tracks/{trackId}/criteria")
    public ResponseEntity<?> getTrackCriteria(@PathVariable Integer trackId) {
        List<Criteria> list = criteriaRepository.findByTrackIdOrderByDisplayOrderAsc(trackId);
        Map<String, Object> resp = new HashMap<>();
        resp.put("items", mapCriteriaList(list));
        resp.put("weightSummary", calculateWeightSummary(list));
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/rounds/{roundId}/criteria")
    public ResponseEntity<?> getRoundCriteria(@PathVariable Integer roundId) {
        List<Criteria> list = criteriaRepository.findByRoundIdOrderByDisplayOrderAsc(roundId);
        Map<String, Object> resp = new HashMap<>();
        resp.put("items", mapCriteriaList(list));
        resp.put("weightSummary", calculateWeightSummary(list));
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/tracks/{trackId}/criteria")
    public ResponseEntity<?> createTrackCriterion(@PathVariable Integer trackId,
                                                  @RequestBody Map<String, Object> req) {
        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new ResourceNotFoundException("Track not found."));

        Criteria c = Criteria.builder()
                .track(track)
                .name((String) req.get("name"))
                .type(CriteriaType.valueOf(((String) req.get("type")).toUpperCase()))
                .weight(((Number) req.get("weight")).doubleValue())
                .maxScore(req.containsKey("maxScore") ? ((Number) req.get("maxScore")).intValue() : 10)
                .description((String) req.get("description"))
                .rubricUrl((String) req.get("rubricUrl"))
                .displayOrder(req.containsKey("displayOrder") ? ((Number) req.get("displayOrder")).intValue() : 0)
                .build();

        return ResponseEntity.ok(mapCriterion(criteriaRepository.save(c)));
    }

    @PostMapping("/rounds/{roundId}/criteria")
    public ResponseEntity<?> createRoundCriterion(@PathVariable Integer roundId,
                                                  @RequestBody Map<String, Object> req) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round not found."));

        Criteria c = Criteria.builder()
                .round(round)
                .name((String) req.get("name"))
                .type(CriteriaType.valueOf(((String) req.get("type")).toUpperCase()))
                .weight(((Number) req.get("weight")).doubleValue())
                .maxScore(req.containsKey("maxScore") ? ((Number) req.get("maxScore")).intValue() : 10)
                .description((String) req.get("description"))
                .rubricUrl((String) req.get("rubricUrl"))
                .displayOrder(req.containsKey("displayOrder") ? ((Number) req.get("displayOrder")).intValue() : 0)
                .build();

        return ResponseEntity.ok(mapCriterion(criteriaRepository.save(c)));
    }

    @PostMapping("/tracks/{trackId}/criteria/batch")
    public ResponseEntity<?> batchCreateTrackCriteria(@PathVariable Integer trackId,
                                                      @RequestBody Map<String, Object> req) {
        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new ResourceNotFoundException("Track not found."));

        List<Map<String, Object>> items = (List<Map<String, Object>>) req.get("items");
        List<Criteria> saved = new ArrayList<>();
        if (items != null) {
            for (Map<String, Object> item : items) {
                Criteria c = Criteria.builder()
                        .track(track)
                        .name((String) item.get("name"))
                        .type(CriteriaType.valueOf(((String) item.get("type")).toUpperCase()))
                        .weight(((Number) item.get("weight")).doubleValue())
                        .maxScore(item.containsKey("maxScore") ? ((Number) item.get("maxScore")).intValue() : 10)
                        .description((String) item.get("description"))
                        .rubricUrl((String) item.get("rubricUrl"))
                        .displayOrder(item.containsKey("displayOrder") ? ((Number) item.get("displayOrder")).intValue() : 0)
                        .build();
                saved.add(criteriaRepository.save(c));
            }
        }
        return ResponseEntity.ok(mapCriteriaList(saved));
    }

    @PostMapping("/rounds/{roundId}/criteria/batch")
    public ResponseEntity<?> batchCreateRoundCriteria(@PathVariable Integer roundId,
                                                      @RequestBody Map<String, Object> req) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round not found."));

        List<Map<String, Object>> items = (List<Map<String, Object>>) req.get("items");
        List<Criteria> saved = new ArrayList<>();
        if (items != null) {
            for (Map<String, Object> item : items) {
                Criteria c = Criteria.builder()
                        .round(round)
                        .name((String) item.get("name"))
                        .type(CriteriaType.valueOf(((String) item.get("type")).toUpperCase()))
                        .weight(((Number) item.get("weight")).doubleValue())
                        .maxScore(item.containsKey("maxScore") ? ((Number) item.get("maxScore")).intValue() : 10)
                        .description((String) item.get("description"))
                        .rubricUrl((String) item.get("rubricUrl"))
                        .displayOrder(item.containsKey("displayOrder") ? ((Number) item.get("displayOrder")).intValue() : 0)
                        .build();
                saved.add(criteriaRepository.save(c));
            }
        }
        return ResponseEntity.ok(mapCriteriaList(saved));
    }

    @GetMapping("/tracks/{trackId}/criteria/clone-sources")
    public ResponseEntity<?> getTrackCloneSources(@PathVariable Integer trackId) {
        List<Track> allTracks = trackRepository.findAll();
        List<Map<String, Object>> sources = new ArrayList<>();
        for (Track track : allTracks) {
            if (track.getId().equals(trackId)) {
                continue;
            }
            List<Criteria> criteria = criteriaRepository.findByTrackIdOrderByDisplayOrderAsc(track.getId());
            if (!criteria.isEmpty()) {
                Map<String, Object> src = new HashMap<>();
                src.put("trackId", track.getId());
                src.put("trackName", track.getName());
                src.put("criteriaCount", criteria.size());
                src.put("hackathonId", track.getRound() != null && track.getRound().getHackathon() != null 
                        ? track.getRound().getHackathon().getId() : null);
                sources.add(src);
            }
        }
        return ResponseEntity.ok(Map.of("sources", sources));
    }

    @PostMapping("/tracks/{trackId}/criteria/clone")
    public ResponseEntity<?> cloneCriteriaToTrack(@PathVariable Integer trackId,
                                                  @RequestBody Map<String, Object> req) {
        Track targetTrack = trackRepository.findById(trackId)
                .orElseThrow(() -> new ResourceNotFoundException("Track not found."));

        Boolean replaceExisting = (Boolean) req.get("replaceExisting");
        if (Boolean.TRUE.equals(replaceExisting)) {
            List<Criteria> existing = criteriaRepository.findByTrackIdOrderByDisplayOrderAsc(trackId);
            criteriaRepository.deleteAll(existing);
        }

        Integer sourceTrackId = req.get("sourceTrackId") != null ? ((Number) req.get("sourceTrackId")).intValue() : null;
        Integer sourceRoundId = req.get("sourceRoundId") != null ? ((Number) req.get("sourceRoundId")).intValue() : null;

        List<Criteria> sourceCriteriaList = new ArrayList<>();
        if (sourceTrackId != null) {
            sourceCriteriaList = criteriaRepository.findByTrackIdOrderByDisplayOrderAsc(sourceTrackId);
        } else if (sourceRoundId != null) {
            sourceCriteriaList = criteriaRepository.findByRoundIdOrderByDisplayOrderAsc(sourceRoundId);
        }

        List<Criteria> cloned = new ArrayList<>();
        for (Criteria sc : sourceCriteriaList) {
            Criteria c = Criteria.builder()
                    .track(targetTrack)
                    .name(sc.getName())
                    .type(sc.getType())
                    .weight(sc.getWeight())
                    .maxScore(sc.getMaxScore())
                    .description(sc.getDescription())
                    .rubricUrl(sc.getRubricUrl())
                    .displayOrder(sc.getDisplayOrder())
                    .sourceCriteria(sc)
                    .build();
            cloned.add(criteriaRepository.save(c));
        }

        return ResponseEntity.ok(Map.of("message", "Cloned successfully.", "count", cloned.size()));
    }

    @PostMapping("/rounds/{roundId}/criteria/clone")
    public ResponseEntity<?> cloneCriteriaToRound(@PathVariable Integer roundId,
                                                  @RequestBody Map<String, Object> req) {
        Round targetRound = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round not found."));

        Boolean replaceExisting = (Boolean) req.get("replaceExisting");
        if (Boolean.TRUE.equals(replaceExisting)) {
            List<Criteria> existing = criteriaRepository.findByRoundIdOrderByDisplayOrderAsc(roundId);
            criteriaRepository.deleteAll(existing);
        }

        Integer sourceTrackId = req.get("sourceTrackId") != null ? ((Number) req.get("sourceTrackId")).intValue() : null;
        Integer sourceRoundId = req.get("sourceRoundId") != null ? ((Number) req.get("sourceRoundId")).intValue() : null;

        List<Criteria> sourceCriteriaList = new ArrayList<>();
        if (sourceTrackId != null) {
            sourceCriteriaList = criteriaRepository.findByTrackIdOrderByDisplayOrderAsc(sourceTrackId);
        } else if (sourceRoundId != null) {
            sourceCriteriaList = criteriaRepository.findByRoundIdOrderByDisplayOrderAsc(sourceRoundId);
        }

        List<Criteria> cloned = new ArrayList<>();
        for (Criteria sc : sourceCriteriaList) {
            Criteria c = Criteria.builder()
                    .round(targetRound)
                    .name(sc.getName())
                    .type(sc.getType())
                    .weight(sc.getWeight())
                    .maxScore(sc.getMaxScore())
                    .description(sc.getDescription())
                    .rubricUrl(sc.getRubricUrl())
                    .displayOrder(sc.getDisplayOrder())
                    .sourceCriteria(sc)
                    .build();
            cloned.add(criteriaRepository.save(c));
        }

        return ResponseEntity.ok(Map.of("message", "Cloned successfully.", "count", cloned.size()));
    }

    @GetMapping("/criteria/{id}")
    public ResponseEntity<?> getCriterionById(@PathVariable Integer id) {
        Criteria c = criteriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Criteria not found."));
        return ResponseEntity.ok(mapCriterion(c));
    }

    @PutMapping("/criteria/{id}")
    public ResponseEntity<?> updateCriterion(@PathVariable Integer id,
                                             @RequestBody Map<String, Object> req) {
        Criteria c = criteriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Criteria not found."));

        if (req.containsKey("name")) c.setName((String) req.get("name"));
        if (req.containsKey("type")) c.setType(CriteriaType.valueOf(((String) req.get("type")).toUpperCase()));
        if (req.containsKey("weight")) c.setWeight(((Number) req.get("weight")).doubleValue());
        if (req.containsKey("maxScore")) c.setMaxScore(((Number) req.get("maxScore")).intValue());
        if (req.containsKey("description")) c.setDescription((String) req.get("description"));
        if (req.containsKey("rubricUrl")) c.setRubricUrl((String) req.get("rubricUrl"));
        if (req.containsKey("displayOrder")) c.setDisplayOrder(((Number) req.get("displayOrder")).intValue());

        return ResponseEntity.ok(mapCriterion(criteriaRepository.save(c)));
    }

    @DeleteMapping("/criteria/{id}")
    public ResponseEntity<?> deleteCriterion(@PathVariable Integer id) {
        Criteria c = criteriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Criteria not found."));
        criteriaRepository.delete(c);
        return ResponseEntity.ok(Map.of("message", "Criteria deleted successfully."));
    }

    @GetMapping("/tracks/{trackId}/criteria/weight-summary")
    public ResponseEntity<?> getTrackWeightSummary(@PathVariable Integer trackId) {
        List<Criteria> list = criteriaRepository.findByTrackIdOrderByDisplayOrderAsc(trackId);
        return ResponseEntity.ok(calculateWeightSummary(list));
    }

    @GetMapping("/rounds/{roundId}/criteria/weight-summary")
    public ResponseEntity<?> getRoundWeightSummary(@PathVariable Integer roundId) {
        List<Criteria> list = criteriaRepository.findByRoundIdOrderByDisplayOrderAsc(roundId);
        return ResponseEntity.ok(calculateWeightSummary(list));
    }

    private List<Map<String, Object>> mapCriteriaList(List<Criteria> list) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Criteria c : list) {
            out.add(mapCriterion(c));
        }
        return out;
    }

    private Map<String, Object> mapCriterion(Criteria c) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", c.getId());
        map.put("trackId", c.getTrack() != null ? c.getTrack().getId() : null);
        map.put("roundId", c.getRound() != null ? c.getRound().getId() : null);
        map.put("sourceCriteriaId", c.getSourceCriteria() != null ? c.getSourceCriteria().getId() : null);
        map.put("name", c.getName());
        map.put("type", c.getType().name());
        map.put("weight", c.getWeight());
        map.put("maxScore", c.getMaxScore());
        map.put("description", c.getDescription());
        map.put("rubricUrl", c.getRubricUrl());
        map.put("displayOrder", c.getDisplayOrder());
        return map;
    }

    private Map<String, Object> calculateWeightSummary(List<Criteria> list) {
        double total = 0;
        double technical = 0;
        double softSkill = 0;
        double penalty = 0;
        for (Criteria c : list) {
            if (c.getType() == CriteriaType.PENALTY) {
                penalty += c.getWeight();
            } else {
                total += c.getWeight();
                if (c.getType() == CriteriaType.TECHNICAL) {
                    technical += c.getWeight();
                } else if (c.getType() == CriteriaType.SOFT_SKILL) {
                    softSkill += c.getWeight();
                }
            }
        }
        Map<String, Object> summary = new HashMap<>();
        summary.put("total", total);
        summary.put("technical", technical);
        summary.put("softSkill", softSkill);
        summary.put("penalty", penalty);
        return summary;
    }
}
