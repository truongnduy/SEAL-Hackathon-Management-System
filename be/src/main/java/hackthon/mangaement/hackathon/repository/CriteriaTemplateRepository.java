package hackthon.mangaement.hackathon.repository;

import hackthon.mangaement.hackathon.model.organization.CriteriaTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CriteriaTemplateRepository extends JpaRepository<CriteriaTemplate, Integer> {
    Optional<CriteriaTemplate> findByName(String name);
}
