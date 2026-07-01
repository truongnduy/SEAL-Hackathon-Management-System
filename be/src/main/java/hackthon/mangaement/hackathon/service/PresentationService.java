package hackthon.mangaement.hackathon.service;

import org.springframework.stereotype.Service;

@Service
public class PresentationService {
    
    // Hàm giả lập quản lý đếm giờ (sẽ kết nối với WebSocket ở các phase sau)
    public void manageTimer(String action, Integer trackId) {
        System.out.println("WebSocket Broadcast Timer Action: " + action + " for track " + trackId);
    }

    public void shuffleQueue(Integer roundId, Integer trackId) {
        System.out.println("Shuffling queue for track " + trackId + " in round " + roundId);
    }

    public void nextInQueue(Integer roundId, Integer trackId) {
        System.out.println("Triggering next presentation for track " + trackId + " in round " + roundId);
    }

    public java.util.Map<String, Object> getTrackControllerStatus(Integer trackId) {
        return java.util.Map.of("trackId", trackId, "status", "active", "currentTeam", "Team A");
    }

    public java.util.Map<String, Object> getRoundControllerStatus(Integer roundId) {
        return java.util.Map.of("roundId", roundId, "status", "active", "currentTeam", "Team B");
    }
}
