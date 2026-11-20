package com.medicita.security;

import com.medicita.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RolesSecurityTest.ControladorPrueba.class)
@Import({
        SecurityConfig.class,
        RolesSecurityTest.ControladorPrueba.class
})
class RolesSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AutenticacionJwtService autenticacionJwtService;

    @Test
    void sinAutenticacionDevuelve401() throws Exception {
        mockMvc.perform(get("/api/usuarios/prueba"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path")
                        .value("/api/usuarios/prueba"));
    }

    @Test
    @WithMockUser(roles = "PACIENTE")
    void pacienteNoPuedeAdministrarUsuarios() throws Exception {
        mockMvc.perform(get("/api/usuarios/prueba"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(roles = "MEDICO")
    void medicoNoPuedeAdministrarUsuarios() throws Exception {
        mockMvc.perform(get("/api/usuarios/prueba"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void administradorPuedeAcceder() throws Exception {
        mockMvc.perform(get("/api/usuarios/prueba"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("OK"));
    }

    @Test
    @WithMockUser(roles = "PACIENTE")
    void pacienteNoPuedeModificarServiciosNiAgenda() throws Exception {
        mockMvc.perform(post("/api/servicios")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/horarios")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminAdministraServiciosYAgendaPeroNoMarcaElEstadoDeLasCitas() throws Exception {
        mockMvc.perform(post("/api/servicios")).andExpect(status().isOk());
        mockMvc.perform(post("/api/horarios")).andExpect(status().isOk());
        mockMvc.perform(patch("/api/citas/1/estado")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MEDICO")
    void medicoAdministraServiciosYAgendaYMarcaElEstadoDeLasCitas() throws Exception {
        mockMvc.perform(post("/api/servicios")).andExpect(status().isOk());
        mockMvc.perform(post("/api/horarios")).andExpect(status().isOk());
        mockMvc.perform(patch("/api/citas/1/estado")).andExpect(status().isOk());
    }

    @RestController
    static class ControladorPrueba {

        @PostMapping({"/api/servicios", "/api/horarios"})
        public Map<String, String> escribir() {
            return Map.of("estado", "OK");
        }

        @GetMapping("/api/horarios")
        public Map<String, String> leerHorarios() {
            return Map.of("estado", "OK");
        }

        @PatchMapping("/api/citas/{id}/estado")
        public Map<String, String> cambiarEstado(@org.springframework.web.bind.annotation.PathVariable Long id) {
            return Map.of("estado", "OK");
        }

        @GetMapping("/api/usuarios/prueba")
        public Map<String, String> comprobarAcceso() {
            return Map.of("estado", "OK");
        }
    }
}