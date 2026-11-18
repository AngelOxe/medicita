package com.medicita.service;

import com.medicita.dto.request.CitaRequest;
import com.medicita.dto.response.CitaResponse;
import com.medicita.entity.Cita;
import com.medicita.entity.EstadoCita;
import com.medicita.entity.HistorialEstado;
import com.medicita.entity.Servicio;
import com.medicita.entity.Usuario;
import com.medicita.exception.RecursoNoEncontradoException;
import com.medicita.exception.ReglaNegocioException;
import com.medicita.repository.CitaRepository;
import com.medicita.repository.HistorialEstadoRepository;
import com.medicita.repository.ServicioRepository;
import com.medicita.repository.UsuarioRepository;
import com.medicita.security.UsuarioAutenticado;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Reglas de las citas.
 *
 * Que dos citas confirmadas no se empalmen se cuida en TRES capas:
 *  1) Aqui, en codigo: el horario pedido debe estar entre los libres (mensaje claro al usuario).
 *  2) @Transactional: la cita y su historial se guardan juntos o no se guarda nada.
 *  3) La restriccion EXCLUDE de PostgreSQL: si dos personas reservan a la vez, la base rechaza
 *     a una (SQLState 23P01) y el GlobalExceptionHandler lo convierte en 422.
 */
@Service
public class CitaService {

    /** El paciente solo puede cancelar si faltan al menos 2 horas. */
    private static final Duration ANTICIPACION_MINIMA = Duration.ofHours(2);

    private final CitaRepository citaRepository;
    private final HistorialEstadoRepository historialRepository;
    private final UsuarioRepository usuarioRepository;
    private final ServicioRepository servicioRepository;
    private final DisponibilidadService disponibilidadService;
    private final NotificacionService notificacionService;
    private final ZoneId zona;

    public CitaService(
            CitaRepository citaRepository,
            HistorialEstadoRepository historialRepository,
            UsuarioRepository usuarioRepository,
            ServicioRepository servicioRepository,
            DisponibilidadService disponibilidadService,
            NotificacionService notificacionService,
            ZoneId zona) {
        this.citaRepository = citaRepository;
        this.historialRepository = historialRepository;
        this.usuarioRepository = usuarioRepository;
        this.servicioRepository = servicioRepository;
        this.disponibilidadService = disponibilidadService;
        this.notificacionService = notificacionService;
        this.zona = zona;
    }

    // ================================================================ reservar

    @Transactional
    public CitaResponse crear(CitaRequest request, UsuarioAutenticado actor) {
        Usuario paciente = resolverPaciente(request, actor);

        Servicio servicio = servicioRepository.findById(request.servicioId())
                .filter(Servicio::isActivo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado"));

        // Capa 1: el horario debe ser uno de los libres para ese servicio y ese dia
        LocalDate fecha = request.inicio().atZoneSameInstant(zona).toLocalDate();
        boolean libre = disponibilidadService.obtener(servicio.getId(), fecha).slots().stream()
                .anyMatch(slot -> slot.inicio().isEqual(request.inicio()));
        if (!libre) {
            throw new ReglaNegocioException("inicio", "El horario seleccionado ya no esta disponible");
        }

        // El backend calcula todo lo demas: el cliente no puede mandar fin, precio ni estado
        Cita cita = new Cita();
        cita.setPaciente(paciente);
        cita.setServicio(servicio);
        cita.setInicio(request.inicio());
        cita.setFin(request.inicio().plusMinutes(servicio.getDuracionMin()));
        cita.setPrecioReservado(servicio.getPrecio());
        cita.setEstado(EstadoCita.confirmada.name());

        // saveAndFlush manda el INSERT ahora: si la BD detecta un empalme, el error sale aqui (capa 3)
        citaRepository.saveAndFlush(cita);
        registrarHistorial(cita, null, EstadoCita.confirmada, actor);

        notificacionService.notificarCitaConfirmada(cita);
        return CitaResponse.desde(cita);
    }

    /** PACIENTE reserva para si mismo. ADMIN y MEDICO deben indicar al paciente. */
    private Usuario resolverPaciente(CitaRequest request, UsuarioAutenticado actor) {
        if (esPaciente(actor)) {
            if (request.pacienteId() != null && !request.pacienteId().equals(actor.getId())) {
                throw new AccessDeniedException("Un paciente solo puede reservar para si mismo");
            }
            return usuarioRepository.findById(actor.getId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        }

        if (request.pacienteId() == null) {
            throw new ReglaNegocioException("pacienteId", "Selecciona un paciente");
        }
        return usuarioRepository.findById(request.pacienteId())
                .filter(u -> u.isActivo() && "PACIENTE".equals(u.getRol().getNombre()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Paciente no encontrado"));
    }

    // ================================================================ consultar

    /** Citas del paciente autenticado. Solo ve las suyas: el filtro lo pone el backend, no el cliente. */
    @Transactional(readOnly = true)
    public Page<CitaResponse> listarMias(
            Long pacienteId, CitaRequest.Tipo tipo, EstadoCita estado, Pageable pageable) {

        OffsetDateTime ahora = OffsetDateTime.now();

        Specification<Cita> filtros = (root, query, cb) -> {
            var condiciones = new ArrayList<Predicate>();
            condiciones.add(cb.equal(root.get("paciente").get("id"), pacienteId));

            // Proxima = confirmada que todavia no empieza
            Predicate esProxima = cb.and(
                    cb.equal(root.get("estado"), EstadoCita.confirmada.name()),
                    cb.greaterThan(root.<OffsetDateTime>get("inicio"), ahora));

            if (tipo == CitaRequest.Tipo.proximas) {
                condiciones.add(esProxima);
            } else if (tipo == CitaRequest.Tipo.historial) {
                condiciones.add(cb.not(esProxima));
            }
            if (estado != null) {
                condiciones.add(cb.equal(root.get("estado"), estado.name()));
            }
            return cb.and(condiciones.toArray(new Predicate[0]));
        };

        return citaRepository.findAll(filtros, pageable).map(CitaResponse::desde);
    }

    /** Agenda completa para ADMIN y MEDICO, con filtros que viajan como parametros al servidor. */
    @Transactional(readOnly = true)
    public Page<CitaResponse> listar(
            String search, EstadoCita estado, LocalDate fecha, Pageable pageable) {

        Specification<Cita> filtros = (root, query, cb) -> {
            var condiciones = new ArrayList<Predicate>();

            if (search != null && !search.isBlank()) {
                String patron = "%" + escaparBusqueda(search.strip().toLowerCase(Locale.ROOT)) + "%";
                condiciones.add(cb.like(
                        cb.lower(root.get("paciente").<String>get("nombre")), patron, '\\'));
            }
            if (estado != null) {
                condiciones.add(cb.equal(root.get("estado"), estado.name()));
            }
            if (fecha != null) {
                condiciones.add(cb.greaterThanOrEqualTo(
                        root.<OffsetDateTime>get("inicio"),
                        fecha.atStartOfDay(zona).toOffsetDateTime()));
                condiciones.add(cb.lessThan(
                        root.<OffsetDateTime>get("inicio"),
                        fecha.plusDays(1).atStartOfDay(zona).toOffsetDateTime()));
            }
            return cb.and(condiciones.toArray(new Predicate[0]));
        };

        return citaRepository.findAll(filtros, pageable).map(CitaResponse::desde);
    }

    @Transactional(readOnly = true)
    public CitaResponse obtener(Long id, UsuarioAutenticado actor) {
        return CitaResponse.desde(buscarVisible(id, actor));
    }

    // ================================================================ cambiar estado

    /**
     * Cancelar. PACIENTE: solo una cita propia y con al menos 2 horas de anticipacion.
     * ADMIN y MEDICO: cualquier cita confirmada, sin limite de tiempo.
     */
    @Transactional
    public CitaResponse cancelar(Long id, UsuarioAutenticado actor) {
        Cita cita = buscarVisible(id, actor);

        if (!esConfirmada(cita)) {
            throw new ReglaNegocioException("Solo se pueden cancelar citas confirmadas");
        }
        if (esPaciente(actor)) {
            Duration faltan = Duration.between(OffsetDateTime.now(), cita.getInicio());
            if (faltan.compareTo(ANTICIPACION_MINIMA) < 0) {
                throw new ReglaNegocioException(
                        "Solo puedes cancelar con al menos 2 horas de anticipacion");
            }
        }

        cambiarEstado(cita, EstadoCita.cancelada, actor);
        notificacionService.notificarCitaCancelada(cita);
        return CitaResponse.desde(cita);
    }

    /** ADMIN y MEDICO marcan la cita como completada o no_asistio, solo si ya termino. */
    @Transactional
    public CitaResponse cambiarEstado(Long id, EstadoCita nuevo, UsuarioAutenticado actor) {
        if (nuevo != EstadoCita.completada && nuevo != EstadoCita.no_asistio) {
            throw new ReglaNegocioException(
                    "estado", "Solo se puede marcar como completada o no_asistio");
        }

        Cita cita = buscarVisible(id, actor);

        if (!esConfirmada(cita)) {
            throw new ReglaNegocioException("Solo se puede cambiar el estado de citas confirmadas");
        }
        if (OffsetDateTime.now().isBefore(cita.getFin())) {
            throw new ReglaNegocioException("La cita aun no termina");
        }

        cambiarEstado(cita, nuevo, actor);
        return CitaResponse.desde(cita);
    }

    // ================================================================ ayudas

    /** Todo cambio de estado deja una fila en historial_estados. */
    private void cambiarEstado(Cita cita, EstadoCita nuevo, UsuarioAutenticado actor) {
        String anterior = cita.getEstado();
        cita.setEstado(nuevo.name());
        registrarHistorial(cita, anterior, nuevo, actor);
    }

    private void registrarHistorial(
            Cita cita, String anterior, EstadoCita nuevo, UsuarioAutenticado actor) {

        Usuario quien = usuarioRepository.getReferenceById(actor.getId());
        historialRepository.save(new HistorialEstado(cita, anterior, nuevo.name(), quien));
    }

    /** 404 si no existe o si es de otro paciente (para un paciente, una cita ajena "no existe"). */
    private Cita buscarVisible(Long id, UsuarioAutenticado actor) {
        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cita no encontrada"));

        if (esPaciente(actor) && !cita.getPaciente().getId().equals(actor.getId())) {
            throw new RecursoNoEncontradoException("Cita no encontrada");
        }
        return cita;
    }

    private boolean esConfirmada(Cita cita) {
        return EstadoCita.confirmada.name().equals(cita.getEstado());
    }

    private boolean esPaciente(UsuarioAutenticado actor) {
        return "PACIENTE".equals(actor.getRol());
    }

    /** Busca los caracteres literalmente, incluso % y _. */
    private String escaparBusqueda(String texto) {
        return texto
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
