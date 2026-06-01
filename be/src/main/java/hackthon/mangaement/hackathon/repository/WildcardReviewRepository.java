package hackthon.mangaement.hackathon.repository;

import hackthon.mangaement.hackathon.model.Team.WildcardReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WildcardReviewRepository extends JpaRepository<WildcardReview, Integer> {
    List<WildcardReview> findByRoundId(Integer roundId);
}
