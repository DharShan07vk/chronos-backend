package com.chronos.chronos.repositiory;

import com.chronos.chronos.model.CapsuleModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CapsuleRepo extends JpaRepository<CapsuleModel,Long> {
    List<CapsuleModel> findByUserEmailOrderByCreatedAtDesc(String email);
}
