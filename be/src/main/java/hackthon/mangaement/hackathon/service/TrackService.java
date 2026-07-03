package hackthon.mangaement.hackathon.service;

import hackthon.mangaement.hackathon.exception.ResourceNotFoundException;
import hackthon.mangaement.hackathon.model.organization.Track;
import hackthon.mangaement.hackathon.repository.TrackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TrackService {

    @Autowired
    private TrackRepository trackRepository;

    public String getProblemStatement(Integer trackId) {
        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new ResourceNotFoundException("Track not found"));
        if (track.getDescription() != null && !track.getDescription().trim().isEmpty()) {
            return track.getDescription();
        }
        if (track.getRound() != null && track.getRound().getProblemStatementUrl() != null) {
            return track.getRound().getProblemStatementUrl();
        }
        return "No problem statement found for this track.";
    }
}
