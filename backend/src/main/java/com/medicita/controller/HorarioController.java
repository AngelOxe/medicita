package com.medicita.controller;

import com.medicita.dto.request.HorarioRequest;
import com.medicita.dto.response.HorarioResponse;
import com.medicita.dto.response.PaginaResponse;
import com.medicita.service.HorarioService;
import com.medicita.util.Paginacion;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Horarios de atencion. Solo ADMIN y MEDICO (regla en SecurityConfig). */
@RestController
@RequestMapping("/api/horarios")
public class HorarioController {

    private final HorarioService horarioService;

    public HorarioController(HorarioService horarioService) {
        this.horarioService = horarioService;
    }

    /** Ejemplo: GET /api/horarios?page=0&size=10&diaSemana=1&activo=true */
    @GetMapping
    public PaginaResponse<HorarioResponse> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer diaSemana,
            @RequestParam(required = false) Boolean activo) {

        var orden = Sort.by("diaSemana").ascending().and(Sort.by("horaInicio").ascending());
        var pagina = horarioService.listar(diaSemana, activo, Paginacion.de(page, size, orden));
        return PaginaResponse.desde(pagina);
    }

    @GetMapping("/{id}")
    public HorarioResponse obtener(@PathVariable Long id) {
        return horarioService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HorarioResponse crear(@Valid @RequestBody HorarioRequest request) {
        return horarioService.crear(request);
    }

    @PutMapping("/{id}")
    public HorarioResponse actualizar(
            @PathVariable Long id,
            @Valid @RequestBody HorarioRequest request) {

        return horarioService.actualizar(id, request);
    }

    /** Sin parametros desactiva el horario (borrado logico); con ?definitivo=true lo borra. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean definitivo) {
        if (definitivo) {
            horarioService.eliminar(id);
        } else {
            horarioService.desactivar(id);
        }
    }
}
