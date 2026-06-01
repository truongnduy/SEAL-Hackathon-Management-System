package hackthon.mangaement.hackathon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import hackthon.mangaement.hackathon.model.organization.Prize;

import java.util.List;

@Repository
public interface PrizeRepository extends JpaRepository<Prize, Integer> {
    List<Prize> findByRoundId(Integer roundId);
    List<Prize> findByTeamId(Integer teamId);
}
