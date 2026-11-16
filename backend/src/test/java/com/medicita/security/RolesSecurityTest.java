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
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @RestController
    static class ControladorPrueba {

        @GetMapping("/api/usuarios/prueba")
        public Map<String, String> comprobarAcceso() {
            return Map.of("estado", "OK");
        }
    }
}