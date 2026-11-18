package com.medicita.repository;

import com.medicita.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, Long> {

    @EntityGraph(attributePaths = "usuario")
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /** Al pedir un enlace nuevo, los anteriores del mismo usuario dejan de servir. */
    @Modifying
    @Query("""
        UPDATE PasswordResetToken t
        SET t.usado = true
        WHERE t.usuario.id = :usuarioId AND t.usado = false
        """)
    int invalidarPendientes(@Param("usuarioId") Long usuarioId);
}