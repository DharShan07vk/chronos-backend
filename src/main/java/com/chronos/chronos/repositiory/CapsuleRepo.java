package com.chronos.chronos.repositiory;

import com.chronos.chronos.model.CapsuleModel;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CapsuleRepo extends JpaRepository<CapsuleModel, Long> {
    @EntityGraph(attributePaths = { "user" })
    List<CapsuleModel> findByUserEmailOrderByCreatedAtDesc(String email);

    List<CapsuleModel> findByUnlockDate(LocalDateTime unlockDate);

    List<CapsuleModel> findByUnlockDateBetween(LocalDateTime start, LocalDateTime end);
}
