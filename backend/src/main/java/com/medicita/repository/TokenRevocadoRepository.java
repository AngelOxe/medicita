package com.medicita.repository;

import com.medicita.entity.TokenRevocado;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenRevocadoRepository
        extends JpaRepository<TokenRevocado, String> {
}