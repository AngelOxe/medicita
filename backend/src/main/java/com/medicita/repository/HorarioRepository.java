package com.medicita.repository;

import com.medicita.entity.Horario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HorarioRepository extends JpaRepository<Horario, Long> {

    @Override
    @EntityGraph(attributePaths = "servicios")
    Optional<Horario> findById(Long id);

    @EntityGraph(attributePaths = "servicios")
    List<Horario> findByDiaSemanaAndActivoTrueOrderByHoraInicioAsc(
        Short diaSemana
    );
}