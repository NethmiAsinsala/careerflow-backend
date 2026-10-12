package com.careerflow.repository;
import com.careerflow.entity.ContentReport;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface ContentReportRepository extends JpaRepository<ContentReport,Long> {
 Page<ContentReport> findByReporterId(Long reporterId,Pageable pageable);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from ContentReport r where r.id = :id")
 Optional<ContentReport> findForUpdate(@Param("id") Long id);
}
