package com.medicita.controller;

import com.medicita.dto.request.UsuarioRequest;
import com.medicita.dto.response.UsuarioResponse;
import com.medicita.security.UsuarioAutenticado;
import com.medicita.service.PerfilService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/perfil")
public class PerfilController {

    private final PerfilService perfilService;

    public PerfilController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    @GetMapping
    public UsuarioResponse obtenerPerfil(
            @AuthenticationPrincipal UsuarioAutenticado usuario) {

        return perfilService.obtenerPerfil(usuario.getId());
    }

    /** La persona edita solo su nombre y telefono; rol, correo y estado no se cambian aqui. */
    @PutMapping
    public UsuarioResponse actualizarPerfil(
            @Valid @RequestBody UsuarioRequest.Perfil request,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {

        return perfilService.actualizar(usuario.getId(), request);
    }
}