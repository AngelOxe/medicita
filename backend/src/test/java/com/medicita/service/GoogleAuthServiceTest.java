package com.medicita.service;

import com.medicita.entity.Rol;
import com.medicita.entity.Usuario;
import com.medicita.exception.ReglaNegocioException;
import com.medicita.repository.RolRepository;
import com.medicita.repository.UsuarioRepository;
import com.medicita.security.GoogleIdentidad;
import com.medicita.security.GoogleTokenVerificador;
import com.medicita.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Inicio de sesion con Google: cuenta nueva (siempre PACIENTE), vinculacion y rechazos. */
class GoogleAuthServiceTest {

    private GoogleTokenVerificador verificador;
    private UsuarioRepository usuarios;
    private RolRepository roles;
    private PasswordEncoder encoder;
    private JwtService jwt;
    private GoogleAuthService servicio;

    @BeforeEach
    void preparar() {
        verificador = mock(GoogleTokenVerificador.class);
        usuarios = mock(UsuarioRepository.class);
        roles = mock(RolRepository.class);
        encoder = mock(PasswordEncoder.class);
        jwt = mock(JwtService.class);

        when(verificador.estaConfigurado()).thenReturn(true);
        when(encoder.encode(anyString())).thenReturn("$2a$12$hashAleatorio");
        when(roles.findByNombre("PACIENTE")).thenReturn(Optional.of(rol("PACIENTE")));
        when(jwt.generar(anyLong(), anyString()))
                .thenReturn(new JwtService.TokenGenerado("token-de-la-app", Instant.now().plusSeconds(3600)));
        when(usuarios.saveAndFlush(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            ReflectionTestUtils.setField(u, "id", 99L);
            return u;
        });

        servicio = new GoogleAuthService(verificador, usuarios, roles, encoder, jwt);
    }

    @Test
    void siGoogleNoEstaConfiguradoSeRechaza() {
        when(verificador.estaConfigurado()).thenReturn(false);

        assertThrows(ReglaNegocioException.class, () -> servicio.iniciarSesion("token"));
        verify(verificador, never()).verificar(anyString());
    }

    @Test
    void unTokenInvalidoSeRechazaConUnauthorized() {
        when(verificador.verificar("falso")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> servicio.iniciarSesion("falso"));
        verify(usuarios, never()).saveAndFlush(any());
    }

    @Test
    void sinCorreoVerificadoPorGoogleSeRechaza() {
        when(verificador.verificar("t")).thenReturn(Optional.of(identidad("ana@gmail.com", false)));

        assertThrows(BadCredentialsException.class, () -> servicio.iniciarSesion("t"));
        verify(usuarios, never()).saveAndFlush(any());
    }

    @Test
    void conUnCorreoNuevoSeCreaUnPacienteSinTelefonoNiContrasenaConocida() {
        when(verificador.verificar("t")).thenReturn(Optional.of(identidad("nueva@gmail.com", true)));
        when(usuarios.findByEmailIgnoreCase("nueva@gmail.com")).thenReturn(Optional.empty());

        var respuesta = servicio.iniciarSesion("t");

        var captura = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).saveAndFlush(captura.capture());
        Usuario creado = captura.getValue();
        assertEquals("PACIENTE", creado.getRol().getNombre());
        assertEquals("sub-123", creado.getGoogleSub());
        assertNull(creado.getTelefono());
        assertEquals("$2a$12$hashAleatorio", creado.getPasswordHash());
        assertEquals("Ana Google", creado.getNombre());
        assertTrue(creado.isActivo());
        assertEquals("token-de-la-app", respuesta.token());
        assertEquals("PACIENTE", respuesta.usuario().rol());
    }

    @Test
    void unaCuentaExistenteSeVinculaConGoogleSinCambiarSuRol() {
        Usuario existente = usuario("medico@gmail.com", "MEDICO", null, true);
        when(verificador.verificar("t")).thenReturn(Optional.of(identidad("medico@gmail.com", true)));
        when(usuarios.findByEmailIgnoreCase("medico@gmail.com")).thenReturn(Optional.of(existente));

        var respuesta = servicio.iniciarSesion("t");

        assertEquals("sub-123", existente.getGoogleSub());
        assertEquals("MEDICO", respuesta.usuario().rol());
        verify(usuarios, never()).saveAndFlush(any());
    }

    @Test
    void siElCorreoYaEstaVinculadoAOtraCuentaDeGoogleSeRechaza() {
        Usuario existente = usuario("ana@gmail.com", "PACIENTE", "otro-sub", true);
        when(verificador.verificar("t")).thenReturn(Optional.of(identidad("ana@gmail.com", true)));
        when(usuarios.findByEmailIgnoreCase("ana@gmail.com")).thenReturn(Optional.of(existente));

        assertThrows(BadCredentialsException.class, () -> servicio.iniciarSesion("t"));
        assertEquals("otro-sub", existente.getGoogleSub());
    }

    @Test
    void unaCuentaDesactivadaNoPuedeEntrarConGoogle() {
        Usuario existente = usuario("ana@gmail.com", "PACIENTE", null, false);
        when(verificador.verificar("t")).thenReturn(Optional.of(identidad("ana@gmail.com", true)));
        when(usuarios.findByEmailIgnoreCase("ana@gmail.com")).thenReturn(Optional.of(existente));

        assertThrows(BadCredentialsException.class, () -> servicio.iniciarSesion("t"));
        assertFalse(existente.isActivo());
        verify(jwt, never()).generar(anyLong(), anyString());
    }

    @Test
    void laConfiguracionPublicaSoloEntregaElIdSiEstaHabilitado() {
        when(verificador.clientId()).thenReturn("mi-id.apps.googleusercontent.com");
        assertEquals("mi-id.apps.googleusercontent.com", servicio.configuracion().clientId());
        assertTrue(servicio.configuracion().habilitado());

        when(verificador.estaConfigurado()).thenReturn(false);
        assertFalse(servicio.configuracion().habilitado());
        assertEquals("", servicio.configuracion().clientId());
    }

    // ---------------------------------------------------------------- apoyo

    private static GoogleIdentidad identidad(String email, boolean verificado) {
        return new GoogleIdentidad("sub-123", email, verificado, "Ana Google", "https://foto.example/a.jpg");
    }

    private static Rol rol(String nombre) {
        Rol rol = new Rol();
        rol.setNombre(nombre);
        return rol;
    }

    private static Usuario usuario(String email, String rol, String googleSub, boolean activo) {
        Usuario u = new Usuario();
        ReflectionTestUtils.setField(u, "id", 7L);
        u.setNombre("Usuario");
        u.setEmail(email);
        u.setRol(rol(rol));
        u.setGoogleSub(googleSub);
        u.setActivo(activo);
        return u;
    }
}
