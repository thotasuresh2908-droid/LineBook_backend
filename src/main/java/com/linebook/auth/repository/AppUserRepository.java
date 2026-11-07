package com.linebook.auth.repository;

import com.linebook.auth.domain.AppUser;
import com.linebook.auth.domain.UserRole;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, String> {

    Optional<AppUser> findByEmail(String email);

    Optional<AppUser> findByAgentId(String agentId);

    boolean existsByEmail(String email);

    boolean existsByRole(UserRole role);

    List<AppUser> findAllByOrderByEmailAsc();
}
