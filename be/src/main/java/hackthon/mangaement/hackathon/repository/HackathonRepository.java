package hackthon.mangaement.hackathon.repository;

import hackthon.mangaement.hackathon.model.organization.Hackathon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface HackathonRepository extends JpaRepository<Hackathon, Integer> {
    Optional<Hackathon> findBySlug(String slug);
    boolean existsByName(String name);
    java.util.List<Hackathon> findByStatus(Hackathon.Status status);
}
