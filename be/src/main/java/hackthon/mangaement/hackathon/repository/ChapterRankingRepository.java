package hackthon.mangaement.hackathon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import hackthon.mangaement.hackathon.model.Chapter.ChapterRanking;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChapterRankingRepository extends JpaRepository<ChapterRanking, Integer> {
    List<ChapterRanking> findByHackathonIdOrderByRankAsc(Integer hackathonId);
    Optional<ChapterRanking> findByHackathonIdAndChapterId(Integer hackathonId, Integer chapterId);
}
