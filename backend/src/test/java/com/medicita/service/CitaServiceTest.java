package com.medicita.service;

import com.medicita.dto.request.CitaRequest;
import com.medicita.dto.response.CitaResponse;
import com.medicita.dto.response.DisponibilidadResponse;
import com.medicita.dto.response.DisponibilidadResponse.Slot;
import com.medicita.entity.Cita;
import com.medicita.entity.EstadoCita;
import com.medicita.entity.HistorialEstado;
import com.medicita.entity.Rol;
import com.medicita.entity.Servicio;
import com.medicita.entity.Usuario;
import com.medicita.exception.RecursoNoEncontradoException;
import com.medicita.exception.ReglaNegocioException;
import com.medicita.repository.CitaRepository;
import com.medicita.repository.HistorialEstadoRepository;
import com.medicita.repository.ServicioRepository;
import com.medicita.repository.UsuarioRepository;
import com.medicita.security.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Reglas de citas: horario libre, permisos por rol, regla de 2 horas y cambio de estado. */
class CitaServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/Mexico_City");

    private CitaRepository citas;
    private HistorialEstadoRepository historial;
    private UsuarioRepository usuarios;
    private ServicioRepository servicios;
    private DisponibilidadService disponibilidad;
    private NotificacionService notificaciones;
    private CitaService servicio;

    private Usuario ana;
    private Usuario luis;
    private Usuario medico;
    private Servicio consulta;

    @BeforeEach
    void preparar() {
        citas = mock(CitaRepository.class);
        historial = mock(HistorialEstadoRepository.class);
        usuarios = mock(UsuarioRepository.class);
        servicios = mock(ServicioRepository.class);
        disponibilidad = mock(DisponibilidadService.class);
        notificaciones = mock(NotificacionService.class);
        servicio = new CitaService(citas, historial, usuarios, servicios, disponibilidad, notificaciones, ZONA);

        ana = usuario(10L, "Ana", "PACIENTE");
        luis = usuario(11L, "Luis", "PACIENTE");
        medico = usuario(2L, "Dr. Daniel", "MEDICO");
        consulta = new Servicio();
        ReflectionTestUtils.setField(consulta, "id", 1L);
        consulta.setNombre("Consulta general");
        consulta.setDuracionMin(30);
        consulta.setPrecio(new BigDecimal("400.00"));
        consulta.setActivo(true);

        when(usuarios.findById(10L)).thenReturn(Optional.of(ana));
        when(usuarios.findById(11L)).thenReturn(Optional.of(luis));
        when(usuarios.getReferenceById(any())).thenReturn(medico);
        when(servicios.findById(1L)).thenReturn(Optional.of(consulta));
        when(citas.saveAndFlush(any(Cita.class))).thenAnswer(i -> i.getArgument(0));
    }

    // ---------------------------------------------------------------- reservar

    @Test
    void pacienteReservaParaSiYElBackendCalculaFinPrecioYEstado() {
        OffsetDateTime inicio = futuro(48);
        libres(inicio);

        CitaResponse r = servicio.crear(new CitaRequest(1L, inicio, null), actor(ana));

        assertEquals("confirmada", r.estado());
        assertEquals(inicio.plusMinutes(30), r.fin());
        assertEquals(new BigDecimal("400.00"), r.precioReservado());
        assertEquals("Ana", r.paciente().nombre());

        ArgumentCaptor<HistorialEstado> h = ArgumentCaptor.forClass(HistorialEstado.class);
        verify(historial).save(h.capture());
        assertEquals(null, h.getValue().getEstadoAnterior());
        assertEquals("confirmada", h.getValue().getEstadoNuevo());
    }

    @Test
    void siElHorarioYaNoEstaLibreResponde422SinGuardar() {
        libres(futuro(72)); // el pedido NO esta entre los libres

        assertThrows(ReglaNegocioException.class,
                () -> servicio.crear(new CitaRequest(1L, futuro(48), null), actor(ana)));
        verify(citas, never()).saveAndFlush(any());
    }

    @Test
    void unPacienteNoPuedeReservarParaOtroPaciente() {
        libres(futuro(48));

        assertThrows(AccessDeniedException.class,
                () -> servicio.crear(new CitaRequest(1L, futuro(48), 11L), actor(ana)));
    }

    @Test
    void adminDebeIndicarElPaciente() {
        libres(futuro(48));

        assertThrows(ReglaNegocioException.class,
                () -> servicio.crear(new CitaRequest(1L, futuro(48), null), actor(medico)));
    }

    @Test
    void medicoReservaLaCitaDeUnPaciente() {
        OffsetDateTime inicio = futuro(48);
        libres(inicio);

        CitaResponse r = servicio.crear(new CitaRequest(1L, inicio, 11L), actor(medico));

        assertEquals("Luis", r.paciente().nombre());
    }

    @Test
    void servicioInactivoDa404() {
        consulta.setActivo(false);

        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.crear(new CitaRequest(1L, futuro(48), null), actor(ana)));
    }

    // ---------------------------------------------------------------- cancelar

    @Test
    void pacienteNoPuedeCancelarConMenosDeDosHoras() {
        Cita cita = cita(ana, futuro(1), "confirmada");
        when(citas.findById(5L)).thenReturn(Optional.of(cita));

        assertThrows(ReglaNegocioException.class, () -> servicio.cancelar(5L, actor(ana)));
        assertEquals("confirmada", cita.getEstado());
    }

    @Test
    void pacientePuedeCancelarConMasDeDosHorasYQuedaEnElHistorial() {
        Cita cita = cita(ana, futuro(3), "confirmada");
        when(citas.findById(5L)).thenReturn(Optional.of(cita));

        CitaResponse r = servicio.cancelar(5L, actor(ana));

        assertEquals("cancelada", r.estado());
        ArgumentCaptor<HistorialEstado> h = ArgumentCaptor.forClass(HistorialEstado.class);
        verify(historial).save(h.capture());
        assertEquals("confirmada", h.getValue().getEstadoAnterior());
        assertEquals("cancelada", h.getValue().getEstadoNuevo());
        verify(notificaciones).notificarCitaCancelada(cita);
    }

    @Test
    void unPacienteNoVeNiCancelaLasCitasDeOtro() {
        Cita ajena = cita(luis, futuro(48), "confirmada");
        when(citas.findById(5L)).thenReturn(Optional.of(ajena));

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.cancelar(5L, actor(ana)));
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.obtener(5L, actor(ana)));
    }

    @Test
    void medicoPuedeCancelarSinLimiteDeTiempo() {
        Cita cita = cita(ana, futuro(0), "confirmada");
        when(citas.findById(5L)).thenReturn(Optional.of(cita));

        assertEquals("cancelada", servicio.cancelar(5L, actor(medico)).estado());
    }

    @Test
    void soloSePuedeCancelarUnaCitaConfirmada() {
        Cita cita = cita(ana, futuro(48), "cancelada");
        when(citas.findById(5L)).thenReturn(Optional.of(cita));

        assertThrows(ReglaNegocioException.class, () -> servicio.cancelar(5L, actor(ana)));
    }

    // ---------------------------------------------------------------- marcar estado

    @Test
    void noSePuedeMarcarUnaCitaQueAunNoTermina() {
        Cita cita = cita(ana, futuro(24), "confirmada");
        when(citas.findById(5L)).thenReturn(Optional.of(cita));

        assertThrows(ReglaNegocioException.class,
                () -> servicio.cambiarEstado(5L, EstadoCita.completada, actor(medico)));
    }

    @Test
    void despuesDeTerminarSePuedeMarcarCompletadaOSinAsistencia() {
        Cita cita = cita(ana, futuro(-5), "confirmada");
        when(citas.findById(5L)).thenReturn(Optional.of(cita));

        assertEquals("completada",
                servicio.cambiarEstado(5L, EstadoCita.completada, actor(medico)).estado());
    }

    @Test
    void soloSeAceptanLosEstadosCompletadaYNoAsistio() {
        assertThrows(ReglaNegocioException.class,
                () -> servicio.cambiarEstado(5L, EstadoCita.cancelada, actor(medico)));
    }

    // ---------------------------------------------------------------- ayudas

    private OffsetDateTime futuro(int horas) {
        return OffsetDateTime.now(ZONA).plusHours(horas).withNano(0);
    }

    private void libres(OffsetDateTime... inicios) {
        List<Slot> slots = java.util.Arrays.stream(inicios)
                .map(i -> new Slot(i, i.plusMinutes(30))).toList();
        when(disponibilidad.obtener(any(), any()))
                .thenReturn(new DisponibilidadResponse(null, 1L, "America/Mexico_City", slots));
    }

    private Usuario usuario(Long id, String nombre, String rol) {
        Usuario u = new Usuario();
        ReflectionTestUtils.setField(u, "id", id);
        u.setNombre(nombre);
        u.setEmail(nombre.toLowerCase() + "@example.com");
        u.setActivo(true);
        Rol r = new Rol();
        r.setNombre(rol);
        u.setRol(r);
        return u;
    }

    private UsuarioAutenticado actor(Usuario u) {
        return new UsuarioAutenticado(u);
    }

    /** Cita que empieza en `inicio` y dura 30 minutos. */
    private Cita cita(Usuario paciente, OffsetDateTime inicio, String estado) {
        Cita c = new Cita();
        ReflectionTestUtils.setField(c, "id", 5L);
        c.setPaciente(paciente);
        c.setServicio(consulta);
        c.setInicio(inicio);
        c.setFin(inicio.plusMinutes(30));
        c.setPrecioReservado(new BigDecimal("400.00"));
        c.setEstado(estado);
        return c;
    }
}
