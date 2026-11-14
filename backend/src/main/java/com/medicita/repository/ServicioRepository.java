package com.medicita.repository;

import com.medicita.entity.Servicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ServicioRepository
        extends JpaRepository<Servicio, Long>,
                JpaSpecificationExecutor<Servicio> {
}