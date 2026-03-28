package com.chronos.chronos.repositiory;

import com.chronos.chronos.model.CapsuleShareModel;
import com.chronos.chronos.model.CapsuleShareStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CapsuleShareRepo extends JpaRepository<CapsuleShareModel, Long> {

    @EntityGraph(attributePaths = { "capsule", "capsule.user" })
    List<CapsuleShareModel> findByRecipientEmailAndStatusInOrderByCreatedAtDesc(
            String recipientEmail,
            Collection<CapsuleShareStatus> statuses);
}
