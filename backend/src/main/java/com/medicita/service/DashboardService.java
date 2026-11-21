package com.medicita.service;

import com.medicita.dto.response.ResumenResponse;
import com.medicita.repository.CitaRepository;
import com.medicita.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

/** Calcula los totales del dashboard con consultas de conteo (no descarga las citas). */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final CitaRepository citaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ZoneId zona;

    public DashboardService(CitaRepository citaRepository, UsuarioRepository usuarioRepository, ZoneId zona) {
        this.citaRepository = citaRepository;
        this.usuarioRepository = usuarioRepository;
        this.zona = zona;
    }

    public ResumenResponse resumen() {
        var ahora = OffsetDateTime.now(zona);
        var inicioDia = ahora.toLocalDate().atStartOfDay(zona).toOffsetDateTime();

        Map<String, Long> porEstado = new LinkedHashMap<>();
        for (String estado : new String[]{"confirmada", "completada", "cancelada", "no_asistio"}) {
            porEstado.put(estado, 0L);
        }
        for (Object[] fila : citaRepository.contarPorEstado()) {
            porEstado.put((String) fila[0], (Long) fila[1]);
        }

        var lunes = ahora.toLocalDate().with(java.time.DayOfWeek.MONDAY);
        var semana = new java.util.ArrayList<ResumenResponse.Dia>();
        for (int i = 0; i < 7; i++) {
            var dia = lunes.plusDays(i);
            var desde = dia.atStartOfDay(zona).toOffsetDateTime();
            semana.add(new ResumenResponse.Dia(dia,
                    citaRepository.countByInicioGreaterThanEqualAndInicioLessThanAndEstadoNot(
                            desde, desde.plusDays(1), "cancelada")));
        }

        return new ResumenResponse(
                citaRepository.countByInicioGreaterThanEqualAndInicioLessThanAndEstadoNot(
                        inicioDia, inicioDia.plusDays(1), "cancelada"),
                citaRepository.countByEstadoAndInicioAfter("confirmada", ahora),
                usuarioRepository.countByRolNombreAndActivoTrue("PACIENTE"),
                porEstado.get("completada"),
                porEstado,
                semana);
    }
}
