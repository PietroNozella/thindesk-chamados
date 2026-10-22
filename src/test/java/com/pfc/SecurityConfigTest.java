package com.pfc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pfc.thindesk.service.ChamadoService;
import com.pfc.thindesk.service.ClienteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(properties = {"THINDESK_USERNAME=teste", "THINDESK_PASSWORD=senha-apenas-teste"})
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChamadoService chamadoService;

    @MockitoBean
    private ClienteService clienteService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void deveBloquearApiLegadaSemRedirecionar() throws Exception {
        mockMvc.perform(get("/api/chamados").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist(HttpHeaders.LOCATION));
    }

    @Test
    void naoDeveServirFormularioDeLogin() throws Exception {
        mockMvc.perform(get("/login").accept(MediaType.TEXT_HTML))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
    }

    @Test
    void deveManterProtecaoCsrfNasRequisicoesDeEscrita() throws Exception {
        mockMvc.perform(post("/api/chamados").contentType(MediaType.APPLICATION_JSON)
                .content("{\"descricao\":\"Tentativa sem CSRF\"}"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(""));
    }
}
