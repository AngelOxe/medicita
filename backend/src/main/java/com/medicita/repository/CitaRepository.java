package com.medicita.repository;

import com.medicita.entity.Cita;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface CitaRepository
        extends JpaRepository<Cita, Long>,
                JpaSpecificationExecutor<Cita> {

    @Override
    @EntityGraph(attributePaths = {"paciente", "servicio"})
    Optional<Cita> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"paciente", "servicio"})
    Page<Cita> findAll(
        Specification<Cita> filtros,
        Pageable paginacion
    );

    @Query("""
        SELECT c
        FROM Cita c
        WHERE c.estado = 'confirmada'
          AND c.inicio < :fin
          AND c.fin > :inicio
        ORDER BY c.inicio
        """)
    List<Cita> buscarConfirmadasEnIntervalo(
        @Param("inicio") OffsetDateTime inicio,
        @Param("fin") OffsetDateTime fin
    );
}