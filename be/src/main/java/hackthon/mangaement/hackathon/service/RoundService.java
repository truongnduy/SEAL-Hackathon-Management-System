package hackthon.mangaement.hackathon.service;

import hackthon.mangaement.hackathon.exception.ResourceNotFoundException;
import hackthon.mangaement.hackathon.model.User.User;
import hackthon.mangaement.hackathon.model.organization.Round;
import hackthon.mangaement.hackathon.repository.RoundRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class RoundService {

    @Autowired
    private RoundRepository roundRepository;

    public void advanceRound(Integer roundId, User coordinator) {
        Round currentRound = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round not found"));

        currentRound.setIsActive(false);
        roundRepository.save(currentRound);

        // Find next round in sequence
        List<Round> rounds = roundRepository.findByHackathonIdOrderBySequenceOrderAsc(currentRound.getHackathon().getId());
        Round nextRound = null;
        for (Round r : rounds) {
            if (r.getSequenceOrder() > currentRound.getSequenceOrder()) {
                nextRound = r;
                break;
            }
        }

        if (nextRound != null) {
            nextRound.setIsActive(true);
            roundRepository.save(nextRound);
        }
    }

    public void publishRound(Integer roundId, User coordinator) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round not found"));
        
        // Simulating publishing (can be expanded to set a published status if added later)
        System.out.println("Round " + roundId + " results published by " + coordinator.getFullName());
    }
}
