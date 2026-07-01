package hackthon.mangaement.hackathon.service;

import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class TrackService {

    public Map<String, Object> getProblemStatement(Integer trackId) {
        return Map.of("content", "Track Problem Statement for track: " + trackId);
    }
}
