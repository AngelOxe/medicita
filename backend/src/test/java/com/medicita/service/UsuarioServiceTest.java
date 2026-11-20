package com.medicita.service;

import com.medicita.dto.request.UsuarioRequest;
import com.medicita.entity.Rol;
import com.medicita.entity.Usuario;
import com.medicita.exception.CorreoDuplicadoException;
import com.medicita.exception.ReglaNegocioException;
import com.medicita.repository.RolRepository;
import com.medicita.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Reglas de administracion de cuentas: un solo medico, ultimo admin, auto-desactivacion y correo unico. */
class UsuarioServiceTest {

    private UsuarioRepository usuarios;
    private RolRepository roles;
    private PasswordEncoder encoder;
    private UsuarioService servicio;

    @BeforeEach
    void preparar() {
        usuarios = mock(UsuarioRepository.class);
        roles = mock(RolRepository.class);
        encoder = mock(PasswordEncoder.class);
        when(encoder.encode(any())).thenReturn("$2a$12$hash");
        servicio = new UsuarioService(usuarios, roles, encoder);
    }

    @Test
    void crearGuardaElHashYNuncaLaContrasenaEnClaro() {
        when(usuarios.existsByEmailIgnoreCase("nuevo@example.com")).thenReturn(false);
        when(roles.findByNombre("PACIENTE")).thenReturn(Optional.of(rol("PACIENTE")));

        var respuesta = servicio.crear(peticion("nuevo@example.com", "PACIENTE"));

        assertEquals("PACIENTE", respuesta.rol());
        verify(usuarios).saveAndFlush(any(Usuario.class));
        verify(encoder).encode("Clave#2026");
    }

    @Test
    void crearConCorreoRepetidoSeRechaza() {
        when(usuarios.existsByEmailIgnoreCase("ana@example.com")).thenReturn(true);

        assertThrows(CorreoDuplicadoException.class,
                () -> servicio.crear(peticion("ana@example.com", "PACIENTE")));
        verify(usuarios, never()).saveAndFlush(any());
    }

    @Test
    void noSePuedeCrearUnSegundoMedicoActivo() {
        when(usuarios.countByRolNombreAndActivoTrue("MEDICO")).thenReturn(1L);

        var error = assertThrows(ReglaNegocioException.class,
                () -> servicio.crear(peticion("otro@example.com", "MEDICO")));
        assertEquals("rol", error.getCampo());
        verify(usuarios, never()).saveAndFlush(any());
    }

    @Test
    void noSePuedeReactivarUnMedicoSiYaHayOtroActivo() {
        var medicoInactivo = usuario(5L, "MEDICO", false);
        when(usuarios.findById(5L)).thenReturn(Optional.of(medicoInactivo));
        when(usuarios.countByRolNombreAndActivoTrue("MEDICO")).thenReturn(1L);

        assertThrows(ReglaNegocioException.class,
                () -> servicio.actualizar(5L, new UsuarioRequest.Actualizar("Dr", "+525512345678", true), 1L));
        assertFalse(medicoInactivo.isActivo());
    }

    @Test
    void nadiePuedeDesactivarSuPropiaCuenta() {
        var admin = usuario(1L, "ADMIN", true);
        when(usuarios.findById(1L)).thenReturn(Optional.of(admin));
        when(usuarios.countByRolNombreAndActivoTrue("ADMIN")).thenReturn(2L);

        assertThrows(ReglaNegocioException.class, () -> servicio.desactivar(1L, 1L));
        assertTrue(admin.isActivo());
    }

    @Test
    void noSeDesactivaAlUltimoAdministradorActivo() {
        var admin = usuario(1L, "ADMIN", true);
        when(usuarios.findById(1L)).thenReturn(Optional.of(admin));
        when(usuarios.countByRolNombreAndActivoTrue("ADMIN")).thenReturn(1L);

        var error = assertThrows(ReglaNegocioException.class, () -> servicio.desactivar(1L, 99L));
        assertNotEquals("", error.getMessage());
        assertTrue(admin.isActivo());
    }

    @Test
    void desactivarUnPacienteConservaLaCuentaPeroLaMarcaInactiva() {
        var paciente = usuario(7L, "PACIENTE", true);
        when(usuarios.findById(7L)).thenReturn(Optional.of(paciente));

        servicio.desactivar(7L, 1L);

        assertFalse(paciente.isActivo());
        verify(usuarios, never()).delete(any(Usuario.class));
    }

    @Test
    void actualizarCambiaNombreYTelefonoPeroNoElCorreoNiElRol() {
        var paciente = usuario(7L, "PACIENTE", true);
        when(usuarios.findById(7L)).thenReturn(Optional.of(paciente));

        var respuesta = servicio.actualizar(7L, new UsuarioRequest.Actualizar("Nombre Nuevo", "+525598765432", true), 1L);

        assertEquals("Nombre Nuevo", respuesta.nombre());
        assertEquals("+525598765432", respuesta.telefono());
        assertEquals("usuario7@example.com", respuesta.email());
        assertEquals("PACIENTE", respuesta.rol());
    }

    @Test
    void eliminarBorraLaCuentaSoloSiNoTieneHistorial() {
        var paciente = usuario(7L, "PACIENTE", true);
        when(usuarios.findById(7L)).thenReturn(Optional.of(paciente));
        when(usuarios.tieneActividad(7L)).thenReturn(false);

        servicio.eliminar(7L, 1L);

        verify(usuarios).borrarTokensDeRecuperacion(7L);
        verify(usuarios).delete(paciente);
    }

    @Test
    void noSeEliminaUnaCuentaConCitasNiLaPropia() {
        var paciente = usuario(7L, "PACIENTE", true);
        when(usuarios.findById(7L)).thenReturn(Optional.of(paciente));
        when(usuarios.tieneActividad(7L)).thenReturn(true);

        assertThrows(ReglaNegocioException.class, () -> servicio.eliminar(7L, 1L));
        assertThrows(ReglaNegocioException.class, () -> servicio.eliminar(7L, 7L));
        verify(usuarios, never()).delete(any(Usuario.class));
    }

    private UsuarioRequest peticion(String email, String rol) {
        return new UsuarioRequest("Persona Nueva", email, "+525512345678", "Clave#2026", rol);
    }

    private Rol rol(String nombre) {
        Rol r = new Rol();
        r.setNombre(nombre);
        return r;
    }

    private Usuario usuario(Long id, String rol, boolean activo) {
        Usuario u = new Usuario();
        ReflectionTestUtils.setField(u, "id", id);
        u.setNombre("Usuario " + id);
        u.setEmail("usuario" + id + "@example.com");
        u.setTelefono("+525500000000");
        u.setActivo(activo);
        u.setRol(rol(rol));
        return u;
    }
}
