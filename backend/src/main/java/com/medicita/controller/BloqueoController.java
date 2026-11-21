package com.medicita.controller;

import com.medicita.dto.request.BloqueoRequest;
import com.medicita.dto.response.BloqueoResponse;
import com.medicita.dto.response.PaginaResponse;
import com.medicita.service.BloqueoService;
import com.medicita.util.Paginacion;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;

/** Bloqueos de agenda. Solo ADMIN y MEDICO (regla en SecurityConfig). */
@RestController
@RequestMapping("/api/bloqueos")
public class BloqueoController {

    private final BloqueoService bloqueoService;

    public BloqueoController(BloqueoService bloqueoService) {
        this.bloqueoService = bloqueoService;
    }

    /** Ejemplo: GET /api/bloqueos?page=0&size=10&desde=2026-11-23&hasta=2026-11-30 */
    @GetMapping
    public PaginaResponse<BloqueoResponse> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {

        var pagina = bloqueoService.listar(
                desde, hasta, Paginacion.de(page, size, Sort.by("inicio").descending()));
        return PaginaResponse.desde(pagina);
    }

    @GetMapping("/{id}")
    public BloqueoResponse obtener(@PathVariable Long id) {
        return bloqueoService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BloqueoResponse crear(@Valid @RequestBody BloqueoRequest request) {
        return bloqueoService.crear(request);
    }

    @PutMapping("/{id}")
    public BloqueoResponse actualizar(
            @PathVariable Long id,
            @Valid @RequestBody BloqueoRequest request) {

        return bloqueoService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        bloqueoService.eliminar(id);
    }
}
