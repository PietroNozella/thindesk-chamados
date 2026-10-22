package com.pfc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pfc.thindesk.service.ChamadoService;
import com.pfc.thindesk.service.ClienteService;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(properties = {"THINDESK_USERNAME=teste", "THINDESK_PASSWORD=senha-apenas-teste",
        "THINDESK_TECNICO_PASSWORD=senha-tecnico-teste"})
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChamadoService chamadoService;

    @MockitoBean
    private ClienteService clienteService;

    static String basic(String usuario, String senha) {
        return "Basic " + Base64.getEncoder().encodeToString(
                (usuario + ":" + senha).getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void deveExigirCredenciaisSemRedirecionar() throws Exception {
        mockMvc.perform(get("/api/autenticacao"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists(HttpHeaders.WWW_AUTHENTICATE))
                .andExpect(header().doesNotExist(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void deveRejeitarSenhaInvalida() throws Exception {
        mockMvc.perform(get("/api/autenticacao").header(HttpHeaders.AUTHORIZATION, basic("teste", "errada")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveIdentificarSolicitanteSemCriarSessao() throws Exception {
        mockMvc.perform(get("/api/autenticacao")
                .header(HttpHeaders.AUTHORIZATION, basic("teste", "senha-apenas-teste")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario").value("teste"))
                .andExpect(jsonPath("$.perfil").value("SOLICITANTE"))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    @Test
    void deveIdentificarTecnico() throws Exception {
        mockMvc.perform(get("/api/autenticacao")
                .header(HttpHeaders.AUTHORIZATION, basic("tecnico", "senha-tecnico-teste")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value("TECNICO"));
    }

    @Test
    void deveBloquearRotasLegadasMesmoAutenticado() throws Exception {
        mockMvc.perform(get("/api/clientes")
                .header(HttpHeaders.AUTHORIZATION, basic("teste", "senha-apenas-teste")))
                .andExpect(status().isForbidden());
    }

    @Test
    void deveExigirAutenticacaoNaEscrita() throws Exception {
        mockMvc.perform(post("/api/chamados"))
                .andExpect(status().isUnauthorized());
    }
}
