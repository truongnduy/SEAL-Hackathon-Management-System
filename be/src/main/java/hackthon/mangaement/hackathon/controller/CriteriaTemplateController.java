package hackthon.mangaement.hackathon.controller;

import hackthon.mangaement.hackathon.model.organization.CriteriaTemplate;
import hackthon.mangaement.hackathon.service.CriteriaTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class CriteriaTemplateController {

    @Autowired
    private CriteriaTemplateService templateService;

    @GetMapping("/criteria-templates")
    public ResponseEntity<List<CriteriaTemplate>> getAllTemplates() {
        return ResponseEntity.ok(templateService.getAll());
    }

    @GetMapping("/criteria-templates/{id}")
    public ResponseEntity<CriteriaTemplate> getTemplateById(@PathVariable Integer id) {
        return ResponseEntity.ok(templateService.getById(id));
    }

    @PostMapping("/criteria-templates")
    public ResponseEntity<CriteriaTemplate> createTemplate(@RequestBody CriteriaTemplate template) {
        return ResponseEntity.ok(templateService.create(template));
    }

    @PutMapping("/criteria-templates/{id}")
    public ResponseEntity<CriteriaTemplate> updateTemplate(@PathVariable Integer id,
                                                           @RequestBody CriteriaTemplate details) {
        return ResponseEntity.ok(templateService.update(id, details));
    }

    @DeleteMapping("/criteria-templates/{id}")
    public ResponseEntity<?> deleteTemplate(@PathVariable Integer id) {
        templateService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Criteria template deleted successfully."));
    }

    @PostMapping("/tracks/{trackId}/criteria/templates/{templateId}/apply")
    public ResponseEntity<?> applyToTrack(@PathVariable Integer trackId,
                                          @PathVariable Integer templateId) {
        templateService.applyToTrack(trackId, templateId);
        return ResponseEntity.ok(Map.of("message", "Criteria template applied to track successfully."));
    }

    @PostMapping("/rounds/{roundId}/criteria/templates/{templateId}/apply")
    public ResponseEntity<?> applyToRound(@PathVariable Integer roundId,
                                          @PathVariable Integer templateId) {
        templateService.applyToRound(roundId, templateId);
        return ResponseEntity.ok(Map.of("message", "Criteria template applied to round successfully."));
    }
}
