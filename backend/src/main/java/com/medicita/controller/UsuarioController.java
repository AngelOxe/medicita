package com.medicita.controller;

import com.medicita.dto.request.UsuarioRequest;
import com.medicita.dto.response.PaginaResponse;
import com.medicita.dto.response.UsuarioResponse;
import com.medicita.security.UsuarioAutenticado;
import com.medicita.service.UsuarioService;
import com.medicita.util.Paginacion;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

/** Usuarios: todo el controlador es solo para ADMIN (lo decide SecurityConfig). */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /** Ejemplo: GET /api/usuarios?page=0&size=10&search=ana&rol=PACIENTE&activo=true */
    @GetMapping
    public PaginaResponse<UsuarioResponse> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) Boolean activo) {

        return PaginaResponse.desde(usuarioService.listar(
                search, rol, activo, Paginacion.de(page, size, Sort.by("nombre").ascending())));
    }

    @GetMapping("/{id}")
    public UsuarioResponse obtener(@PathVariable Long id) {
        return usuarioService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse crear(@Valid @RequestBody UsuarioRequest request) {
        return usuarioService.crear(request);
    }

    @PutMapping("/{id}")
    public UsuarioResponse actualizar(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioRequest.Actualizar request,
            @AuthenticationPrincipal UsuarioAutenticado actor) {

        return usuarioService.actualizar(id, request, actor.getId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado actor) {
        usuarioService.desactivar(id, actor.getId());
    }
}
