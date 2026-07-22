package hackthon.mangaement.hackathon.service;

import hackthon.mangaement.hackathon.exception.ResourceNotFoundException;
import hackthon.mangaement.hackathon.model.organization.*;
import hackthon.mangaement.hackathon.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CriteriaTemplateService {

    @Autowired
    private CriteriaTemplateRepository templateRepository;

    @Autowired
    private TrackRepository trackRepository;

    @Autowired
    private RoundRepository roundRepository;

    @Autowired
    private CriteriaRepository criteriaRepository;

    public List<CriteriaTemplate> getAll() {
        return templateRepository.findAll();
    }

    public CriteriaTemplate getById(Integer id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Criteria template not found with ID: " + id));
    }

    public CriteriaTemplate create(CriteriaTemplate template) {
        if (Boolean.TRUE.equals(template.getIsDefault())) {
            resetDefaultTemplates();
        }
        
        // Associate items with the template
        if (template.getItems() != null) {
            for (CriteriaTemplateItem item : template.getItems()) {
                item.setTemplate(template);
            }
        }
        
        return templateRepository.save(template);
    }

    public CriteriaTemplate update(Integer id, CriteriaTemplate details) {
        CriteriaTemplate template = getById(id);
        template.setName(details.getName());
        template.setDescription(details.getDescription());
        
        if (Boolean.TRUE.equals(details.getIsDefault())) {
            resetDefaultTemplates();
            template.setIsDefault(true);
        } else {
            template.setIsDefault(false);
        }

        // Clear existing items and rebuild to leverage JPA orphanRemoval
        template.getItems().clear();
        if (details.getItems() != null) {
            for (CriteriaTemplateItem item : details.getItems()) {
                item.setTemplate(template);
                template.getItems().add(item);
            }
        }

        return templateRepository.save(template);
    }

    public void delete(Integer id) {
        CriteriaTemplate template = getById(id);
        templateRepository.delete(template);
    }

    public void applyToTrack(Integer trackId, Integer templateId) {
        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new ResourceNotFoundException("Track not found with ID: " + trackId));

        CriteriaTemplate template = getById(templateId);

        // Delete existing criteria for the track
        List<Criteria> existing = criteriaRepository.findByTrackIdOrderByDisplayOrderAsc(trackId);
        criteriaRepository.deleteAll(existing);

        // Map template items to track criteria
        for (CriteriaTemplateItem item : template.getItems()) {
            Criteria c = Criteria.builder()
                    .track(track)
                    .round(null)
                    .name(item.getName())
                    .type(item.getType())
                    .weight(item.getWeight())
                    .maxScore(item.getMaxScore())
                    .description(item.getDescription())
                    .displayOrder(item.getDisplayOrder())
                    .build();
            criteriaRepository.save(c);
        }
    }

    public void applyToRound(Integer roundId, Integer templateId) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round not found with ID: " + roundId));

        CriteriaTemplate template = getById(templateId);

        // Delete existing criteria for the round
        List<Criteria> existing = criteriaRepository.findByRoundIdOrderByDisplayOrderAsc(roundId);
        criteriaRepository.deleteAll(existing);

        // Map template items to round criteria
        for (CriteriaTemplateItem item : template.getItems()) {
            Criteria c = Criteria.builder()
                    .track(null)
                    .round(round)
                    .name(item.getName())
                    .type(item.getType())
                    .weight(item.getWeight())
                    .maxScore(item.getMaxScore())
                    .description(item.getDescription())
                    .displayOrder(item.getDisplayOrder())
                    .build();
            criteriaRepository.save(c);
        }
    }

    private void resetDefaultTemplates() {
        List<CriteriaTemplate> all = templateRepository.findAll();
        for (CriteriaTemplate t : all) {
            if (Boolean.TRUE.equals(t.getIsDefault())) {
                t.setIsDefault(false);
                templateRepository.save(t);
            }
        }
    }
}
