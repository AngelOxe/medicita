package com.medicita.repository;

import com.medicita.entity.Servicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServicioRepository
        extends JpaRepository<Servicio, Long>,
                JpaSpecificationExecutor<Servicio> {

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    @Query(value = "SELECT EXISTS (SELECT 1 FROM citas WHERE servicio_id = :id)", nativeQuery = true)
    boolean tieneCitas(@Param("id") Long id);

    /** Quita al servicio de los horarios que lo ofrecian antes de borrarlo. */
    @Modifying
    @Query(value = "DELETE FROM horario_servicio WHERE servicio_id = :id", nativeQuery = true)
    void quitarDeHorarios(@Param("id") Long id);
}