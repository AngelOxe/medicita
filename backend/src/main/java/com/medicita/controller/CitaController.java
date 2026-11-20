package com.medicita.controller;

import com.medicita.dto.request.CitaRequest;
import com.medicita.dto.response.CitaResponse;
import com.medicita.dto.response.PaginaResponse;
import com.medicita.entity.EstadoCita;
import com.medicita.security.UsuarioAutenticado;
import com.medicita.service.CitaService;
import com.medicita.util.Paginacion;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Citas. Que rol entra a cada ruta lo decide SecurityConfig; si la cita es del usuario
 * (o de otro paciente) lo decide CitaService.
 */
@RestController
@RequestMapping("/api/citas")
public class CitaController {

    private final CitaService citaService;

    public CitaController(CitaService citaService) {
        this.citaService = citaService;
    }

    /** PACIENTE reserva para si mismo; ADMIN y MEDICO agregan pacienteId al cuerpo. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CitaResponse crear(
            @Valid @RequestBody CitaRequest request,
            @AuthenticationPrincipal UsuarioAutenticado actor) {

        return citaService.crear(request, actor);
    }

    /** Ejemplo: GET /api/citas/mias?page=0&size=10&tipo=proximas&estado=confirmada */
    @GetMapping("/mias")
    public PaginaResponse<CitaResponse> mias(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) CitaRequest.Tipo tipo,
            @RequestParam(required = false) EstadoCita estado,
            @AuthenticationPrincipal UsuarioAutenticado actor) {

        // Las proximas se ordenan de la mas cercana a la mas lejana; el historial, al reves
        Sort orden = tipo == CitaRequest.Tipo.proximas
                ? Sort.by("inicio").ascending()
                : Sort.by("inicio").descending();

        var pagina = citaService.listarMias(
                actor.getId(), tipo, estado, Paginacion.de(page, size, orden));
        return PaginaResponse.desde(pagina);
    }

    /** Ejemplo: GET /api/citas?page=0&size=10&search=ana&estado=confirmada&fecha=2026-11-23 */
    @GetMapping
    public PaginaResponse<CitaResponse> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) EstadoCita estado,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "desc") String orden) {

        // desde/hasta (ambos inclusive) alimentan las vistas de calendario; "asc" ordena la agenda del dia
        Sort sort = "asc".equalsIgnoreCase(orden)
                ? Sort.by("inicio").ascending()
                : Sort.by("inicio").descending();
        var pagina = citaService.listar(
                search, estado, fecha, desde, hasta,
                Paginacion.de(page, size, sort));
        return PaginaResponse.desde(pagina);
    }

    @GetMapping("/{id}")
    public CitaResponse obtener(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioAutenticado actor) {

        return citaService.obtener(id, actor);
    }

    /** Sin cuerpo. Paciente: solo propias y con 2 h o mas. ADMIN y MEDICO: sin limite. */
    @PatchMapping("/{id}/cancelar")
    public CitaResponse cancelar(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioAutenticado actor) {

        return citaService.cancelar(id, actor);
    }

    /** Cuerpo: {"estado":"completada"} o {"estado":"no_asistio"}. Solo MEDICO. */
    @PatchMapping("/{id}/estado")
    public CitaResponse cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody CitaRequest.CambioEstado request,
            @AuthenticationPrincipal UsuarioAutenticado actor) {

        return citaService.cambiarEstado(id, request.estado(), actor);
    }
}
