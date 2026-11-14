package com.medicita.repository;

import com.medicita.entity.Bloqueo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface BloqueoRepository
        extends JpaRepository<Bloqueo, Long>,
                JpaSpecificationExecutor<Bloqueo> {

    @Query("""
        SELECT b
        FROM Bloqueo b
        WHERE b.inicio < :fin
          AND b.fin > :inicio
        ORDER BY b.inicio
        """)
    List<Bloqueo> buscarEnIntervalo(
        @Param("inicio") OffsetDateTime inicio,
        @Param("fin") OffsetDateTime fin
    );
}