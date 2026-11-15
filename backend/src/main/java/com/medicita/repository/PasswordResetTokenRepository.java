package com.medicita.repository;

import com.medicita.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, Long> {

    @EntityGraph(attributePaths = "usuario")
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
}