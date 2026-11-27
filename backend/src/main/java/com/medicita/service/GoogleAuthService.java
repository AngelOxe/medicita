package com.medicita.service;

import com.medicita.dto.response.AuthResponse;
import com.medicita.dto.response.GoogleConfigResponse;
import com.medicita.dto.response.UsuarioResponse;
import com.medicita.entity.Usuario;
import com.medicita.exception.ReglaNegocioException;
import com.medicita.repository.RolRepository;
import com.medicita.repository.UsuarioRepository;
import com.medicita.security.GoogleIdentidad;
import com.medicita.security.GoogleTokenVerificador;
import com.medicita.security.JwtService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Inicio de sesion y registro con Google.
 * Se verifica el token de Google y se busca la cuenta por correo:
 * - Si existe y esta activa, se vincula con Google (solo si Google confirma el correo) y se inicia sesion.
 * - Si no existe, se crea un PACIENTE (el rol nunca lo decide quien se registra).
 * En los dos casos se entrega el MISMO JWT de la aplicacion, asi que roles, rutas y logout funcionan igual.
 */
@Service
public class GoogleAuthService {

    private static final String MENSAJE_CREDENCIALES = "No se pudo iniciar sesion con Google";

    private final GoogleTokenVerificador verificador;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public GoogleAuthService(
            GoogleTokenVerificador verificador,
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.verificador = verificador;
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public GoogleConfigResponse configuracion() {
        boolean habilitado = verificador.estaConfigurado();
        return new GoogleConfigResponse(habilitado, habilitado ? verificador.clientId() : "");
    }

    @Transactional
    public AuthResponse iniciarSesion(String credential) {
        if (!verificador.estaConfigurado()) {
            throw new ReglaNegocioException("El inicio de sesion con Google no esta disponible");
        }

        GoogleIdentidad identidad = verificador.verificar(credential)
                .orElseThrow(() -> new BadCredentialsException(MENSAJE_CREDENCIALES));

        // Sin correo verificado por Google no se puede confiar en que la cuenta sea de esa persona
        if (!identidad.emailVerificado() || identidad.email() == null || identidad.email().isBlank()) {
            throw new BadCredentialsException(MENSAJE_CREDENCIALES);
        }

        String email = identidad.email().trim();
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .map(existente -> vincular(existente, identidad))
                .orElseGet(() -> crearPaciente(identidad, email));

        var token = jwtService.generar(usuario.getId(), usuario.getRol().getNombre());
        return new AuthResponse(token.token(), "Bearer", token.expiraEn(), UsuarioResponse.de(usuario));
    }

    private Usuario vincular(Usuario usuario, GoogleIdentidad identidad) {
        if (!usuario.isActivo()) {
            throw new BadCredentialsException(MENSAJE_CREDENCIALES);
        }
        if (usuario.getGoogleSub() == null) {
            usuario.setGoogleSub(identidad.sub());
        } else if (!usuario.getGoogleSub().equals(identidad.sub())) {
            // El correo ya esta vinculado a OTRA cuenta de Google
            throw new BadCredentialsException(MENSAJE_CREDENCIALES);
        }
        if (usuario.getAvatarUrl() == null && identidad.foto() != null) {
            usuario.setAvatarUrl(recortar(identidad.foto(), 500));
        }
        return usuario;
    }

    private Usuario crearPaciente(GoogleIdentidad identidad, String email) {
        var rolPaciente = rolRepository.findByNombre("PACIENTE")
                .orElseThrow(() -> new IllegalStateException("No existe el rol PACIENTE en la base de datos"));

        var usuario = new Usuario();
        usuario.setNombre(nombreDe(identidad, email));
        usuario.setEmail(email);
        // La cuenta de Google no tiene contrasena propia: se guarda un hash de un valor aleatorio
        // que nadie conoce, asi que por contrasena no se puede entrar (si la quiere, usa "Olvide mi contrasena")
        usuario.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        usuario.setTelefono(null);
        usuario.setGoogleSub(identidad.sub());
        usuario.setAvatarUrl(identidad.foto() == null ? null : recortar(identidad.foto(), 500));
        usuario.setRol(rolPaciente);
        usuario.setActivo(true);

        try {
            return usuarioRepository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException error) {
            // Otra peticion creo la cuenta al mismo tiempo: se usa esa
            return usuarioRepository.findByEmailIgnoreCase(email).orElseThrow(() -> error);
        }
    }

    private String nombreDe(GoogleIdentidad identidad, String email) {
        String nombre = identidad.nombre();
        if (nombre == null || nombre.isBlank()) {
            nombre = email.substring(0, email.indexOf('@') > 0 ? email.indexOf('@') : email.length());
        }
        return recortar(nombre.trim(), 120);
    }

    private String recortar(String texto, int maximo) {
        return texto.length() <= maximo ? texto : texto.substring(0, maximo);
    }
}
