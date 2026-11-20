package com.medicita.service;

import com.medicita.dto.request.HorarioRequest;
import com.medicita.dto.response.HorarioResponse;
import com.medicita.entity.Cita;
import com.medicita.entity.Horario;
import com.medicita.entity.Servicio;
import com.medicita.exception.RecursoNoEncontradoException;
import com.medicita.exception.ReglaNegocioException;
import com.medicita.repository.CitaRepository;
import com.medicita.repository.HorarioRepository;
import com.medicita.repository.ServicioRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Horarios de atencion semanales y los servicios que se ofrecen en cada uno
 * (relacion muchos a muchos mediante horario_servicio).
 */
@Service
@Transactional(readOnly = true)
public class HorarioService {

    private final HorarioRepository horarioRepository;
    private final ServicioRepository servicioRepository;
    private final CitaRepository citaRepository;
    private final ZoneId zona;

    public HorarioService(
            HorarioRepository horarioRepository,
            ServicioRepository servicioRepository,
            CitaRepository citaRepository,
            ZoneId zona) {
        this.horarioRepository = horarioRepository;
        this.servicioRepository = servicioRepository;
        this.citaRepository = citaRepository;
        this.zona = zona;
    }

    public Page<HorarioResponse> listar(
            Integer diaSemana, Boolean activo, Pageable pageable) {

        Specification<Horario> filtros = (root, query, cb) -> {
            var condiciones = new ArrayList<Predicate>();
            if (diaSemana != null) {
                condiciones.add(cb.equal(root.get("diaSemana"), diaSemana.shortValue()));
            }
            if (activo != null) {
                condiciones.add(cb.equal(root.get("activo"), activo));
            }
            return cb.and(condiciones.toArray(new Predicate[0]));
        };

        // Los servicios de cada horario se cargan por lotes (@BatchSize), no uno por uno
        return horarioRepository.findAll(filtros, pageable).map(HorarioResponse::desde);
    }

    public HorarioResponse obtener(Long id) {
        return HorarioResponse.desde(buscar(id));
    }

    @Transactional
    public HorarioResponse crear(HorarioRequest request) {
        validarDatos(request, null);
        Set<Long> sinCoberturaAntes = citasSinCobertura();

        Horario horario = new Horario();
        aplicar(horario, request);
        horarioRepository.saveAndFlush(horario);

        verificarCobertura(sinCoberturaAntes);
        return HorarioResponse.desde(horario);
    }

    @Transactional
    public HorarioResponse actualizar(Long id, HorarioRequest request) {
        Horario horario = buscar(id);
        validarDatos(request, id);
        Set<Long> sinCoberturaAntes = citasSinCobertura();

        aplicar(horario, request);
        horarioRepository.saveAndFlush(horario);

        verificarCobertura(sinCoberturaAntes);
        return HorarioResponse.desde(horario);
    }

    /** Borrado logico: el horario queda inactivo y deja de ofrecerse. */
    @Transactional
    public void desactivar(Long id) {
        Horario horario = buscar(id);
        if (!horario.isActivo()) {
            return;
        }
        Set<Long> sinCoberturaAntes = citasSinCobertura();

        horario.setActivo(false);
        horarioRepository.saveAndFlush(horario);

        verificarCobertura(sinCoberturaAntes);
    }

    /** Borrado definitivo: se aplica la misma regla de cobertura que al desactivar. */
    @Transactional
    public void eliminar(Long id) {
        Horario horario = buscar(id);
        Set<Long> sinCoberturaAntes = citasSinCobertura();

        horarioRepository.delete(horario);
        horarioRepository.flush();

        verificarCobertura(sinCoberturaAntes);
    }

    // ---------------------------------------------------------------- validaciones

    private void validarDatos(HorarioRequest request, Long idActual) {
        if (!request.horaFin().isAfter(request.horaInicio())) {
            throw new ReglaNegocioException(
                    "horaFin", "La hora de fin debe ser posterior a la de inicio");
        }

        Short dia = request.diaSemana().shortValue();
        boolean repetido = idActual == null
                ? horarioRepository.existsByDiaSemanaAndHoraInicioAndHoraFin(
                        dia, request.horaInicio(), request.horaFin())
                : horarioRepository.existsByDiaSemanaAndHoraInicioAndHoraFinAndIdNot(
                        dia, request.horaInicio(), request.horaFin(), idActual);
        if (repetido) {
            throw new ReglaNegocioException(
                    "horaInicio", "Ya existe un horario con ese dia y esas horas");
        }

        if (Boolean.TRUE.equals(request.activo())
                && horarioRepository.existeCruceActivo(
                        dia,
                        request.horaInicio(),
                        request.horaFin(),
                        idActual == null ? -1L : idActual)) {
            throw new ReglaNegocioException(
                    "horaInicio", "Se cruza con otro horario activo del mismo dia");
        }
    }

    private void aplicar(Horario horario, HorarioRequest request) {
        List<Servicio> servicios = servicioRepository.findAllById(request.servicioIds());
        if (servicios.size() != request.servicioIds().size()) {
            throw new RecursoNoEncontradoException(
                    "Alguno de los servicios seleccionados no existe");
        }

        horario.setDiaSemana(request.diaSemana().shortValue());
        horario.setHoraInicio(request.horaInicio());
        horario.setHoraFin(request.horaFin());
        horario.setActivo(request.activo());
        // Aqui se llena la tabla puente horario_servicio
        horario.setServicios(new HashSet<>(servicios));
    }

    // ---------------------------------------------------------------- cobertura de citas futuras

    /**
     * Regla del contrato: un cambio de agenda no puede dejar citas futuras confirmadas
     * sin horario o servicio que las cubra. Se compara el antes y el despues, asi un dato
     * viejo inconsistente no bloquea cambios que no tienen relacion.
     */
    private void verificarCobertura(Set<Long> sinCoberturaAntes) {
        Set<Long> nuevas = citasSinCobertura();
        nuevas.removeAll(sinCoberturaAntes);

        if (!nuevas.isEmpty()) {
            throw new ReglaNegocioException(
                    "Hay " + nuevas.size() + " cita(s) futura(s) confirmada(s) que quedarian "
                            + "sin horario. Cancelalas primero");
        }
    }

    private Set<Long> citasSinCobertura() {
        List<Horario> activos = horarioRepository.findByActivoTrue();
        List<Cita> futuras = citaRepository.buscarConfirmadasFuturas(OffsetDateTime.now());

        return futuras.stream()
                .filter(cita -> !estaCubierta(cita, activos))
                .map(Cita::getId)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private boolean estaCubierta(Cita cita, List<Horario> activos) {
        ZonedDateTime inicio = cita.getInicio().atZoneSameInstant(zona);
        ZonedDateTime fin = cita.getFin().atZoneSameInstant(zona);
        if (!inicio.toLocalDate().equals(fin.toLocalDate())) {
            return false;
        }

        short dia = (short) inicio.getDayOfWeek().getValue();
        LocalTime desde = inicio.toLocalTime();
        LocalTime hasta = fin.toLocalTime();
        Long servicioId = cita.getServicio().getId();

        return activos.stream().anyMatch(h ->
                h.getDiaSemana() == dia
                        && !desde.isBefore(h.getHoraInicio())
                        && !hasta.isAfter(h.getHoraFin())
                        && h.getServicios().stream()
                                .anyMatch(s -> s.getId().equals(servicioId)));
    }

    private Horario buscar(Long id) {
        return horarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Horario no encontrado"));
    }
}
