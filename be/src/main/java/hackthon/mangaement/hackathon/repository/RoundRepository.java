package hackthon.mangaement.hackathon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import hackthon.mangaement.hackathon.model.organization.Round;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoundRepository extends JpaRepository<Round, Integer> {
    List<Round> findByHackathonIdOrderBySequenceOrderAsc(Integer hackathonId);
    Optional<Round> findByHackathonIdAndSequenceOrder(Integer hackathonId, Integer sequenceOrder);
    Optional<Round> findByHackathonIdAndIsFinalTrue(Integer hackathonId);
}
