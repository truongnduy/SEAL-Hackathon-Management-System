package hackthon.mangaement.hackathon.controller;

import hackthon.mangaement.hackathon.model.User.User;
import hackthon.mangaement.hackathon.model.organization.ExportJob;
import hackthon.mangaement.hackathon.service.ExportJobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class ExportJobController {

    @Autowired
    private ExportJobService exportJobService;

    @PostMapping("/hackathons/{id}/export-jobs")
    public ResponseEntity<?> createExportJob(@PathVariable Integer id,
                                             @RequestBody Map<String, Object> req,
                                             @AuthenticationPrincipal User user) {
        String type = (String) req.getOrDefault("type", "CSV_SCORES");
        ExportJob job = exportJobService.createExportJob(id, type, user);
        return ResponseEntity.ok(mapExportJobToMap(job));
    }

    @GetMapping("/export-jobs/{id}")
    public ResponseEntity<?> getExportJobStatus(@PathVariable Integer id) {
        ExportJob job = exportJobService.getExportJob(id);
        return ResponseEntity.ok(mapExportJobToMap(job));
    }

    @GetMapping("/export-jobs/{id}/download")
    public ResponseEntity<Resource> downloadExportFile(@PathVariable Integer id) {
        File file = exportJobService.getExportFile(id);
        Resource resource = new FileSystemResource(file);
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                .body(resource);
    }

    private Map<String, Object> mapExportJobToMap(ExportJob job) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", job.getId());
        map.put("hackathonId", job.getHackathon() != null ? job.getHackathon().getId() : null);
        map.put("type", job.getType() != null ? job.getType().name() : null);
        map.put("status", job.getStatus() != null ? job.getStatus().name() : null);
        map.put("fileUrl", job.getFileUrl());
        map.put("errorMessage", job.getErrorMessage());
        map.put("requestedBy", job.getRequestedBy() != null ? job.getRequestedBy().getId() : null);
        map.put("startedAt", job.getStartedAt() != null ? job.getStartedAt().toString() : null);
        map.put("finishedAt", job.getFinishedAt() != null ? job.getFinishedAt().toString() : null);
        map.put("createdAt", job.getCreatedAt() != null ? job.getCreatedAt().toString() : null);
        return map;
    }
}
