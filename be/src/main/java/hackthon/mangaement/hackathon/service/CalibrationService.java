package hackthon.mangaement.hackathon.service;

import hackthon.mangaement.hackathon.model.User.User;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class CalibrationService {

    public List<Map<String, Object>> getAllSessions(User coordinator) {
        return List.of();
    }

    public Map<String, Object> getSessionById(Integer id, User user) {
        return Map.of("id", id, "status", "mocked");
    }

    public void submitCalibrationScore(Map<String, Object> payload, User judge) {
        System.out.println("Calibration score submitted by " + judge.getFullName());
    }
}
