package hackthon.mangaement.hackathon.repository;

import hackthon.mangaement.hackathon.model.User.IndividualRanking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface IndividualRankingRepository extends JpaRepository<IndividualRanking, Integer> {
    List<IndividualRanking> findByHackathonIdOrderByRankAsc(Integer hackathonId);
    Optional<IndividualRanking> findByHackathonIdAndUserId(Integer hackathonId, Integer userId);
}
