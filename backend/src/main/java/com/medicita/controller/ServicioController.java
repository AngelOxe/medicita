package com.medicita.controller;

import com.medicita.dto.request.ServicioRequest;
import com.medicita.dto.response.PaginaResponse;
import com.medicita.dto.response.ServicioResponse;
import com.medicita.security.UsuarioAutenticado;
import com.medicita.service.ServicioService;
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

/**
 * Catalogo de servicios. Las reglas por rol viven en SecurityConfig
 * (GET: autenticados; POST, PUT y DELETE: ADMIN y MEDICO).
 */
@RestController
@RequestMapping("/api/servicios")
public class ServicioController {

    private final ServicioService servicioService;

    public ServicioController(ServicioService servicioService) {
        this.servicioService = servicioService;
    }

    /** Ejemplo: GET /api/servicios?page=0&size=10&search=consulta&activo=true */
    @GetMapping
    public PaginaResponse<ServicioResponse> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean activo,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {

        var pagina = servicioService.listar(
                search,
                activo,
                esPaciente(usuario),
                Paginacion.de(page, size, Sort.by("nombre").ascending())
        );
        return PaginaResponse.desde(pagina);
    }

    @GetMapping("/{id}")
    public ServicioResponse obtener(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {

        return servicioService.obtenerPorId(id, esPaciente(usuario));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServicioResponse crear(@Valid @RequestBody ServicioRequest request) {
        return servicioService.crear(request);
    }

    @PutMapping("/{id}")
    public ServicioResponse actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ServicioRequest request) {

        return servicioService.actualizar(id, request);
    }

    /**
     * Sin parametros: borrado logico (desactiva; las citas historicas se conservan).
     * Con ?definitivo=true: borra el servicio, solo si nunca tuvo citas.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean definitivo) {
        if (definitivo) {
            servicioService.eliminar(id);
        } else {
            servicioService.desactivar(id);
        }
    }

    private boolean esPaciente(UsuarioAutenticado usuario) {
        return "PACIENTE".equals(usuario.getRol());
    }
}
