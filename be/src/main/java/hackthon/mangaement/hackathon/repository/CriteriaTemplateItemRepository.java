package hackthon.mangaement.hackathon.repository;

import hackthon.mangaement.hackathon.model.organization.CriteriaTemplateItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CriteriaTemplateItemRepository extends JpaRepository<CriteriaTemplateItem, Integer> {
    List<CriteriaTemplateItem> findByTemplateIdOrderByDisplayOrderAsc(Integer templateId);
}
