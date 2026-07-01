package hackthon.mangaement.hackathon.service;

import hackthon.mangaement.hackathon.model.User.User;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class MeService {

    public List<Map<String, Object>> getBrowseableHackathons(User user) {
        // Return active hackathons that student can register for
        return List.of();
    }

    public void registerForHackathon(Integer hackathonId, User user) {
        // Registration logic
    }

    public List<Map<String, Object>> getJudgeSubmissions(User judge) {
        // Return assigned submissions for the judge
        return List.of();
    }
}
