package hackthon.mangaement.hackathon.controller;

import hackthon.mangaement.hackathon.model.User.User;
import hackthon.mangaement.hackathon.model.organization.Prize;
import hackthon.mangaement.hackathon.service.RankingService;
import hackthon.mangaement.hackathon.service.TransitionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class RankingController {

    @Autowired
    private RankingService rankingService;

    @Autowired
    private TransitionService transitionService;

    @GetMapping("/rounds/{roundId}/transition-details")
    public ResponseEntity<?> getTransitionDetails(@PathVariable Integer roundId) {
        TransitionService.RoundPromotionDetails details = transitionService.getTransitionDetails(roundId);
        return ResponseEntity.ok(details);
    }

    @PostMapping("/rounds/{roundId}/transition")
    public ResponseEntity<?> transitionRound(@PathVariable Integer roundId,
                                             @RequestBody Map<String, List<Integer>> req,
                                             @AuthenticationPrincipal User coordinator,
                                             HttpServletRequest servletRequest) {
        List<Integer> advancingTeamIds = req.get("advancingTeamIds");
        transitionService.confirmRoundTransition(roundId, advancingTeamIds, coordinator, servletRequest.getRemoteAddr());
        Map<String, String> resp = new HashMap<>();
        resp.put("message", "Round transition confirmed successfully.");
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/hackathons/{id}/prizes")
    public ResponseEntity<?> awardPrize(@PathVariable Integer id,
                                        @RequestBody Map<String, Object> req,
                                        @AuthenticationPrincipal User coordinator) {
        Prize prize = rankingService.awardPrize(
                (Integer) req.get("trackId"),
                (Integer) req.get("roundId"),
                (Integer) req.get("teamId"),
                (String) req.get("prizeName"),
                Prize.PrizeRank.valueOf((String) req.get("prizeRank")),
                (String) req.get("prizeValue"),
                (String) req.get("description"),
                coordinator
        );
        return ResponseEntity.ok(prize);
    }

    @PostMapping("/hackathons/{id}/finish")
    public ResponseEntity<?> publishResultsAndFinish(@PathVariable Integer id,
                                                     @AuthenticationPrincipal User coordinator,
                                                     HttpServletRequest servletRequest) {
        rankingService.publishResults(id, coordinator, servletRequest.getRemoteAddr());
        Map<String, String> resp = new HashMap<>();
        resp.put("message", "Hackathon completed and results published successfully.");
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/hackathons/{id}/export-rbl")
    public ResponseEntity<byte[]> exportRblDataset(@PathVariable Integer id) {
        String csv = rankingService.exportAnonymizedRblDataset(id);
        byte[] csvBytes = csv.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=anonymized_rbl_hackathon_" + id + ".csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvBytes);
    }
}
