package com.medicita.service;

import com.medicita.dto.response.DisponibilidadResponse;
import com.medicita.dto.response.DisponibilidadResponse.Slot;
import com.medicita.entity.Bloqueo;
import com.medicita.entity.Cita;
import com.medicita.entity.Horario;
import com.medicita.entity.Servicio;
import com.medicita.exception.RecursoNoEncontradoException;
import com.medicita.repository.BloqueoRepository;
import com.medicita.repository.CitaRepository;
import com.medicita.repository.HorarioRepository;
import com.medicita.repository.ServicioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Calcula las horas libres de un servicio en una fecha. */
@Service
@Transactional(readOnly = true)
public class DisponibilidadService {

    /** Cada cuantos minutos puede empezar una cita dentro de una jornada. */
    static final int PASO_MINUTOS = 30;

    private final ServicioRepository servicioRepository;
    private final HorarioRepository horarioRepository;
    private final CitaRepository citaRepository;
    private final BloqueoRepository bloqueoRepository;
    private final ZoneId zona;

    public DisponibilidadService(
            ServicioRepository servicioRepository,
            HorarioRepository horarioRepository,
            CitaRepository citaRepository,
            BloqueoRepository bloqueoRepository,
            ZoneId zona) {
        this.servicioRepository = servicioRepository;
        this.horarioRepository = horarioRepository;
        this.citaRepository = citaRepository;
        this.bloqueoRepository = bloqueoRepository;
        this.zona = zona;
    }

    public DisponibilidadResponse obtener(Long servicioId, LocalDate fecha) {
        Servicio servicio = servicioRepository.findById(servicioId)
                .filter(Servicio::isActivo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado"));
        int duracion = servicio.getDuracionMin();

        // 1) Horarios activos de ese dia de la semana que ofrecen el servicio (relacion N:M)
        short dia = (short) fecha.getDayOfWeek().getValue();
        List<Horario> jornadas = horarioRepository
                .findByDiaSemanaAndActivoTrueOrderByHoraInicioAsc(dia)
                .stream()
                .filter(h -> h.getServicios().stream()
                        .anyMatch(s -> s.getId().equals(servicioId)))
                .toList();

        // 2) Lo que ya ocupa ese dia: citas confirmadas y bloqueos
        OffsetDateTime inicioDia = fecha.atStartOfDay(zona).toOffsetDateTime();
        OffsetDateTime finDia = fecha.plusDays(1).atStartOfDay(zona).toOffsetDateTime();
        List<Cita> citas = citaRepository.buscarConfirmadasEnIntervalo(inicioDia, finDia);
        List<Bloqueo> bloqueos = bloqueoRepository.buscarEnIntervalo(inicioDia, finDia);

        // 3) Se recorre cada jornada de PASO_MINUTOS en PASO_MINUTOS
        OffsetDateTime ahora = OffsetDateTime.now();
        List<Slot> slots = new ArrayList<>();

        for (Horario jornada : jornadas) {
            int desde = jornada.getHoraInicio().toSecondOfDay() / 60;
            int hasta = jornada.getHoraFin().toSecondOfDay() / 60;

            // La cita debe terminar dentro de la jornada
            for (int minuto = desde; minuto + duracion <= hasta; minuto += PASO_MINUTOS) {
                OffsetDateTime inicio = fecha.atStartOfDay(zona)
                        .plusMinutes(minuto).toOffsetDateTime();
                OffsetDateTime fin = inicio.plusMinutes(duracion);

                boolean libre = inicio.isAfter(ahora)
                        && citas.stream().noneMatch(c -> seCruzan(inicio, fin, c.getInicio(), c.getFin()))
                        && bloqueos.stream().noneMatch(b -> seCruzan(inicio, fin, b.getInicio(), b.getFin()));

                if (libre) {
                    slots.add(new Slot(inicio, fin));
                }
            }
        }

        slots.sort(Comparator.comparing(Slot::inicio));
        return new DisponibilidadResponse(fecha, servicioId, zona.getId(), slots);
    }

    /** Intervalos semiabiertos [inicio, fin): 10:00-10:30 y 10:30-11:00 NO se cruzan. */
    private boolean seCruzan(
            OffsetDateTime inicioA, OffsetDateTime finA,
            OffsetDateTime inicioB, OffsetDateTime finB) {
        return inicioA.isBefore(finB) && finA.isAfter(inicioB);
    }
}
