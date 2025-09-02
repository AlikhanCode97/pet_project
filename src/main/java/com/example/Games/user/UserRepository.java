package com.example.Games.user;

import com.example.Games.user.role.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    List<User> findAllByRole_Name(RoleType role); // Using enum RoleType

    Optional<User> findByBalance_Id(Long balanceId);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
