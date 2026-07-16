package hackthon.mangaement.hackathon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import hackthon.mangaement.hackathon.model.organization.ExportJob;

@Repository
public interface ExportJobRepository extends JpaRepository<ExportJob, Integer> {
}
