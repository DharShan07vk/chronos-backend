package com.chronos.chronos.repositiory;

import java.util.Optional;

import com.chronos.chronos.model.UserModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepo extends JpaRepository<UserModel, Long> {
    Optional<UserModel> findByEmail(String email);

    boolean existsByEmail(String email);

}
