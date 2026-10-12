package com.careerflow.repository;
import com.careerflow.entity.Certification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface CertificationRepository extends JpaRepository<Certification,Long> {
 List<Certification> findByJobSeekerIdOrderByIdAsc(Long id);
}
