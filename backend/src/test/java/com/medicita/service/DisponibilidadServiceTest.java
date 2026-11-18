package com.medicita.service;

import com.medicita.dto.response.DisponibilidadResponse;
import com.medicita.entity.Bloqueo;
import com.medicita.entity.Cita;
import com.medicita.entity.Horario;
import com.medicita.entity.Servicio;
import com.medicita.exception.RecursoNoEncontradoException;
import com.medicita.repository.BloqueoRepository;
import com.medicita.repository.CitaRepository;
import com.medicita.repository.HorarioRepository;
import com.medicita.repository.ServicioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Reglas de disponibilidad: duracion, bloqueos, citas y limite exacto de los intervalos. */
class DisponibilidadServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/Mexico_City");
    // Un lunes lejano, para que siempre sea futuro
    private static final LocalDate LUNES = LocalDate.of(2099, 1, 5);

    private ServicioRepository servicios;
    private HorarioRepository horarios;
    private CitaRepository citas;
    private BloqueoRepository bloqueos;
    private DisponibilidadService servicio;

    @BeforeEach
    void preparar() {
        servicios = mock(ServicioRepository.class);
        horarios = mock(HorarioRepository.class);
        citas = mock(CitaRepository.class);
        bloqueos = mock(BloqueoRepository.class);
        servicio = new DisponibilidadService(servicios, horarios, citas, bloqueos, ZONA);

        Servicio consulta = servicioConId(1L, 30);
        when(servicios.findById(1L)).thenReturn(Optional.of(consulta));
        when(horarios.findByDiaSemanaAndActivoTrueOrderByHoraInicioAsc(anyShort()))
                .thenReturn(List.of(jornada("09:00", "11:00", consulta)));
        when(citas.buscarConfirmadasEnIntervalo(any(), any())).thenReturn(List.of());
        when(bloqueos.buscarEnIntervalo(any(), any())).thenReturn(List.of());
    }

    @Test
    void generaHorariosCadaMediaHoraDentroDeLaJornada() {
        DisponibilidadResponse r = servicio.obtener(1L, LUNES);

        // Jornada 09:00-11:00, cita de 30 min: 09:00, 09:30, 10:00, 10:30
        assertEquals(4, r.slots().size());
        assertEquals("America/Mexico_City", r.zonaHoraria());
        assertEquals(LocalTime.of(9, 0), r.slots().get(0).inicio().atZoneSameInstant(ZONA).toLocalTime());
        assertEquals(LocalTime.of(10, 30), r.slots().get(3).inicio().atZoneSameInstant(ZONA).toLocalTime());
    }

    @Test
    void unaCitaConfirmadaOcultaSusHorariosPeroElLimiteExactoQuedaLibre() {
        Cita cita = new Cita();
        cita.setInicio(hora(10, 0));
        cita.setFin(hora(10, 30));
        when(citas.buscarConfirmadasEnIntervalo(any(), any())).thenReturn(List.of(cita));

        List<LocalTime> horas = horasLibres(servicio.obtener(1L, LUNES));

        assertTrue(!horas.contains(LocalTime.of(10, 0)));      // ocupado
        assertTrue(horas.contains(LocalTime.of(10, 30)));      // 10:30 empieza justo cuando termina la otra
        assertTrue(horas.contains(LocalTime.of(9, 30)));       // termina 10:00: tampoco se cruza
    }

    @Test
    void unBloqueoQuitaLosHorariosQueSeCruzan() {
        Bloqueo bloqueo = new Bloqueo();
        bloqueo.setInicio(hora(9, 15));
        bloqueo.setFin(hora(10, 15));
        bloqueo.setMotivo("Ausencia");
        when(bloqueos.buscarEnIntervalo(any(), any())).thenReturn(List.of(bloqueo));

        List<LocalTime> horas = horasLibres(servicio.obtener(1L, LUNES));

        // 09:00-09:30 se cruza (09:15), 09:30 y 10:00 tambien; solo queda 10:30
        assertEquals(List.of(LocalTime.of(10, 30)), horas);
    }

    @Test
    void unServicioMasLargoReduceLosHorariosPosibles() {
        Servicio larga = servicioConId(2L, 90);
        when(servicios.findById(2L)).thenReturn(Optional.of(larga));
        when(horarios.findByDiaSemanaAndActivoTrueOrderByHoraInicioAsc(anyShort()))
                .thenReturn(List.of(jornada("09:00", "11:00", larga)));

        // 90 min dentro de 09:00-11:00: solo 09:00 y 09:30
        assertEquals(List.of(LocalTime.of(9, 0), LocalTime.of(9, 30)),
                horasLibres(servicio.obtener(2L, LUNES)));
    }

    @Test
    void sinHorarioQueOfrezcaElServicioNoHayHorarios() {
        Servicio otro = servicioConId(9L, 30);
        when(horarios.findByDiaSemanaAndActivoTrueOrderByHoraInicioAsc(anyShort()))
                .thenReturn(List.of(jornada("09:00", "11:00", otro)));

        assertTrue(servicio.obtener(1L, LUNES).slots().isEmpty());
    }

    @Test
    void servicioInactivoOInexistenteDa404() {
        Servicio inactivo = servicioConId(3L, 30);
        inactivo.setActivo(false);
        when(servicios.findById(3L)).thenReturn(Optional.of(inactivo));
        when(servicios.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.obtener(3L, LUNES));
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.obtener(99L, LUNES));
    }

    @Test
    void nuncaOfreceHorariosPasados() {
        LocalDate ayer = LocalDate.now(ZONA).minusDays(1);
        when(horarios.findByDiaSemanaAndActivoTrueOrderByHoraInicioAsc(anyShort()))
                .thenReturn(List.of(jornada("09:00", "11:00", servicioConId(1L, 30))));

        assertTrue(servicio.obtener(1L, ayer).slots().isEmpty());
    }

    // ------------------------------------------------------------ ayudas

    private List<LocalTime> horasLibres(DisponibilidadResponse r) {
        return r.slots().stream()
                .map(s -> s.inicio().atZoneSameInstant(ZONA).toLocalTime())
                .toList();
    }

    private OffsetDateTime hora(int h, int m) {
        return LUNES.atTime(h, m).atZone(ZONA).toOffsetDateTime();
    }

    private Servicio servicioConId(Long id, int duracion) {
        Servicio s = new Servicio();
        ReflectionTestUtils.setField(s, "id", id);
        s.setNombre("Servicio " + id);
        s.setDuracionMin(duracion);
        s.setActivo(true);
        return s;
    }

    private Horario jornada(String inicio, String fin, Servicio... ofrecidos) {
        Horario h = new Horario();
        h.setDiaSemana((short) 1);
        h.setHoraInicio(LocalTime.parse(inicio));
        h.setHoraFin(LocalTime.parse(fin));
        h.setActivo(true);
        h.setServicios(Set.of(ofrecidos));
        return h;
    }
}
