package com.medicita.repository;

import com.medicita.entity.Horario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface HorarioRepository
        extends JpaRepository<Horario, Long>,
                JpaSpecificationExecutor<Horario> {

    boolean existsByDiaSemanaAndHoraInicioAndHoraFin(
        Short diaSemana,
        LocalTime horaInicio,
        LocalTime horaFin
    );

    boolean existsByDiaSemanaAndHoraInicioAndHoraFinAndIdNot(
        Short diaSemana,
        LocalTime horaInicio,
        LocalTime horaFin,
        Long id
    );

    /** Todos los horarios activos con sus servicios (para revisar la cobertura de citas futuras). */
    @EntityGraph(attributePaths = "servicios")
    List<Horario> findByActivoTrue();

    /** Hay otro horario activo ese dia que se cruza con el rango (excluirId = -1 si es nuevo). */
    @Query("""
        SELECT COUNT(h) > 0
        FROM Horario h
        WHERE h.activo = true
          AND h.diaSemana = :dia
          AND h.horaInicio < :fin
          AND h.horaFin > :inicio
          AND h.id <> :excluirId
        """)
    boolean existeCruceActivo(
        @Param("dia") Short dia,
        @Param("inicio") LocalTime inicio,
        @Param("fin") LocalTime fin,
        @Param("excluirId") Long excluirId
    );

    @Override
    @EntityGraph(attributePaths = "servicios")
    Optional<Horario> findById(Long id);

    @EntityGraph(attributePaths = "servicios")
    List<Horario> findByDiaSemanaAndActivoTrueOrderByHoraInicioAsc(
        Short diaSemana
    );
}