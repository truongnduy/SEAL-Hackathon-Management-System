package hackthon.mangaement.hackathon.service;

import hackthon.mangaement.hackathon.model.User.User;
import org.springframework.stereotype.Service;

@Service
public class RoundService {

    public void advanceRound(Integer roundId, User coordinator) {
        // Logic to advance round (e.g. state transitions)
        System.out.println("Round " + roundId + " advanced by " + coordinator.getFullName());
    }

    public void publishRound(Integer roundId, User coordinator) {
        // Logic to publish round results
        System.out.println("Round " + roundId + " published by " + coordinator.getFullName());
    }
}
