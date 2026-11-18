package com.medicita.service;

import com.medicita.dto.request.BloqueoRequest;
import com.medicita.dto.response.BloqueoResponse;
import com.medicita.entity.Bloqueo;
import com.medicita.exception.RecursoNoEncontradoException;
import com.medicita.exception.ReglaNegocioException;
import com.medicita.repository.BloqueoRepository;
import com.medicita.repository.CitaRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;

/** Bloqueos de agenda (ausencias, congresos...): quitan disponibilidad. */
@Service
@Transactional(readOnly = true)
public class BloqueoService {

    private final BloqueoRepository bloqueoRepository;
    private final CitaRepository citaRepository;
    private final ZoneId zona;

    public BloqueoService(
            BloqueoRepository bloqueoRepository,
            CitaRepository citaRepository,
            ZoneId zona) {
        this.bloqueoRepository = bloqueoRepository;
        this.citaRepository = citaRepository;
        this.zona = zona;
    }

    /** desde y hasta son fechas inclusivas en la zona del consultorio. */
    public Page<BloqueoResponse> listar(LocalDate desde, LocalDate hasta, Pageable pageable) {
        Specification<Bloqueo> filtros = (root, query, cb) -> {
            var condiciones = new ArrayList<Predicate>();
            if (desde != null) {
                condiciones.add(cb.greaterThan(
                        root.<OffsetDateTime>get("fin"),
                        desde.atStartOfDay(zona).toOffsetDateTime()));
            }
            if (hasta != null) {
                condiciones.add(cb.lessThan(
                        root.<OffsetDateTime>get("inicio"),
                        hasta.plusDays(1).atStartOfDay(zona).toOffsetDateTime()));
            }
            return cb.and(condiciones.toArray(new Predicate[0]));
        };
        return bloqueoRepository.findAll(filtros, pageable).map(BloqueoResponse::desde);
    }

    public BloqueoResponse obtener(Long id) {
        return BloqueoResponse.desde(buscar(id));
    }

    @Transactional
    public BloqueoResponse crear(BloqueoRequest request) {
        validar(request);
        Bloqueo bloqueo = new Bloqueo();
        aplicar(bloqueo, request);
        return BloqueoResponse.desde(bloqueoRepository.saveAndFlush(bloqueo));
    }

    @Transactional
    public BloqueoResponse actualizar(Long id, BloqueoRequest request) {
        Bloqueo bloqueo = buscar(id);
        validar(request);
        aplicar(bloqueo, request);
        return BloqueoResponse.desde(bloqueoRepository.saveAndFlush(bloqueo));
    }

    /** Eliminar un bloqueo devuelve esos horarios a la disponibilidad. */
    @Transactional
    public void eliminar(Long id) {
        bloqueoRepository.delete(buscar(id));
    }

    private void validar(BloqueoRequest request) {
        if (!request.fin().isAfter(request.inicio())) {
            throw new ReglaNegocioException("fin", "El fin debe ser posterior al inicio");
        }

        // No se puede bloquear un rango donde ya hay citas confirmadas por atender
        OffsetDateTime ahora = OffsetDateTime.now();
        long citasAfectadas = citaRepository
                .buscarConfirmadasEnIntervalo(request.inicio(), request.fin())
                .stream()
                .filter(c -> c.getFin().isAfter(ahora))
                .count();
        if (citasAfectadas > 0) {
            throw new ReglaNegocioException(
                    "Hay " + citasAfectadas + " cita(s) confirmada(s) en ese intervalo. "
                            + "Cancelalas primero");
        }
    }

    private void aplicar(Bloqueo bloqueo, BloqueoRequest request) {
        bloqueo.setInicio(request.inicio());
        bloqueo.setFin(request.fin());
        bloqueo.setMotivo(request.motivo());
    }

    private Bloqueo buscar(Long id) {
        return bloqueoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Bloqueo no encontrado"));
    }
}
