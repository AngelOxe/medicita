package com.medicita.controller;

import com.medicita.dto.response.UsuarioResponse;
import com.medicita.security.UsuarioAutenticado;
import com.medicita.service.PerfilService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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
}