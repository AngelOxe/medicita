package com.medicita.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "tokens_revocados")
public class TokenRevocado {

    @Id
    @Column(length = 100)
    private String jti;

    @Column(name = "expira_en", nullable = false)
    private OffsetDateTime expiraEn;

    public TokenRevocado() {
    }

    public TokenRevocado(String jti, OffsetDateTime expiraEn) {
        this.jti = jti;
        this.expiraEn = expiraEn;
    }

    public String getJti() {
        return jti;
    }

    public OffsetDateTime getExpiraEn() {
        return expiraEn;
    }
}