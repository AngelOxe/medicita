package com.medicita.service;

import com.medicita.dto.request.UsuarioRequest;
import com.medicita.dto.response.UsuarioResponse;
import com.medicita.entity.Usuario;
import com.medicita.exception.CorreoDuplicadoException;
import com.medicita.exception.RecursoNoEncontradoException;
import com.medicita.exception.ReglaNegocioException;
import com.medicita.repository.RolRepository;
import com.medicita.repository.UsuarioRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Administracion de cuentas (solo ADMIN). Reglas: un unico MEDICO activo, nunca quedarse sin
 * ADMIN activo y nadie puede desactivar su propia cuenta. Desactivar es un borrado logico.
 */
@Service
@Transactional(readOnly = true)
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Page<UsuarioResponse> listar(String search, String rol, Boolean activo, Pageable paginacion) {
        Specification<Usuario> filtros = (root, query, cb) -> {
            var condiciones = new ArrayList<Predicate>();

            if (rol != null && !rol.isBlank()) {
                condiciones.add(cb.equal(root.get("rol").get("nombre"), rol.strip().toUpperCase(Locale.ROOT)));
            }
            if (activo != null) {
                condiciones.add(cb.equal(root.get("activo"), activo));
            }
            if (search != null && !search.isBlank()) {
                String patron = "%" + escapar(search.strip().toLowerCase(Locale.ROOT)) + "%";
                condiciones.add(cb.or(
                        cb.like(cb.lower(root.<String>get("nombre")), patron, '\\'),
                        cb.like(cb.lower(root.<String>get("email")), patron, '\\')));
            }
            return cb.and(condiciones.toArray(new Predicate[0]));
        };
        return usuarioRepository.findAll(filtros, paginacion).map(UsuarioResponse::de);
    }

    public UsuarioResponse obtener(Long id) {
        return UsuarioResponse.de(buscar(id));
    }

    @Transactional
    public UsuarioResponse crear(UsuarioRequest request) {
        if (usuarioRepository.existsByEmailIgnoreCase(request.email())) {
            throw new CorreoDuplicadoException();
        }
        if ("MEDICO".equals(request.rol())) {
            validarUnicoMedico();
        }

        var rol = rolRepository.findByNombre(request.rol())
                .orElseThrow(() -> new ReglaNegocioException("rol", "El rol indicado no existe"));

        var usuario = new Usuario();
        usuario.setNombre(request.nombre());
        usuario.setEmail(request.email());
        usuario.setTelefono(request.telefono());
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setRol(rol);
        usuario.setActivo(true);

        try {
            usuarioRepository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException exception) {
            throw new CorreoDuplicadoException();
        }
        return UsuarioResponse.de(usuario);
    }

    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioRequest.Actualizar request, Long actorId) {
        var usuario = buscar(id);

        if (usuario.isActivo() && !request.activo()) {
            validarPuedeDesactivar(usuario, actorId);
        }
        if (!usuario.isActivo() && request.activo() && "MEDICO".equals(usuario.getRol().getNombre())) {
            validarUnicoMedico();
        }

        usuario.setNombre(request.nombre());
        usuario.setTelefono(request.telefono());
        usuario.setActivo(request.activo());
        return UsuarioResponse.de(usuario);
    }

    /** DELETE: desactiva la cuenta y conserva sus citas e historial. */
    @Transactional
    public void desactivar(Long id, Long actorId) {
        var usuario = buscar(id);
        if (!usuario.isActivo()) {
            return;
        }
        validarPuedeDesactivar(usuario, actorId);
        usuario.setActivo(false);
    }

    /** Borrado definitivo: solo cuentas sin citas ni historial; las demas se desactivan. */
    @Transactional
    public void eliminar(Long id, Long actorId) {
        var usuario = buscar(id);
        if (usuario.getId().equals(actorId)) {
            throw new ReglaNegocioException("No puedes eliminar tu propia cuenta");
        }
        if (usuario.isActivo()) {
            validarPuedeDesactivar(usuario, actorId);
        }
        if (usuarioRepository.tieneActividad(id)) {
            throw new ReglaNegocioException(
                    "Esta cuenta tiene citas o historial registrados y no se puede eliminar. Desactivala para conservarlos");
        }
        usuarioRepository.borrarTokensDeRecuperacion(id);
        usuarioRepository.delete(usuario);
        usuarioRepository.flush();
    }

    private void validarPuedeDesactivar(Usuario usuario, Long actorId) {
        if (usuario.getId().equals(actorId)) {
            throw new ReglaNegocioException("No puedes desactivar tu propia cuenta");
        }
        if ("ADMIN".equals(usuario.getRol().getNombre())
                && usuarioRepository.countByRolNombreAndActivoTrue("ADMIN") <= 1) {
            throw new ReglaNegocioException("Debe quedar al menos un administrador activo");
        }
    }

    private void validarUnicoMedico() {
        if (usuarioRepository.countByRolNombreAndActivoTrue("MEDICO") >= 1) {
            throw new ReglaNegocioException("rol", "Ya existe un medico activo. Solo puede haber uno");
        }
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }

    private static String escapar(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
