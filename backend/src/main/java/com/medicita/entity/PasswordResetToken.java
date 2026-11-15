package com.medicita.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expira_en", nullable = false)
    private OffsetDateTime expiraEn;

    @Column(nullable = false)
    private boolean usado = false;

    public PasswordResetToken() {
    }

    public PasswordResetToken(
            Usuario usuario,
            String tokenHash,
            OffsetDateTime expiraEn) {
        this.usuario = usuario;
        this.tokenHash = tokenHash;
        this.expiraEn = expiraEn;
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public OffsetDateTime getExpiraEn() {
        return expiraEn;
    }

    public boolean isUsado() {
        return usado;
    }

    public void setUsado(boolean usado) {
        this.usado = usado;
    }
}