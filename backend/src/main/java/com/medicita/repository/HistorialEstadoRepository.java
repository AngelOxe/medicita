package com.medicita.repository;

import com.medicita.entity.HistorialEstado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistorialEstadoRepository
        extends JpaRepository<HistorialEstado, Long> {

    @EntityGraph(attributePaths = "cambiadoPor")
    Page<HistorialEstado> findByCita_IdOrderByFechaAscIdAsc(
        Long citaId,
        Pageable paginacion
    );
}