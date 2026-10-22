package com.pfc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pfc.thindesk.controller.ChamadoController;
import com.pfc.thindesk.entity.Chamado;
import com.pfc.thindesk.repository.ChamadoRepository;
import com.pfc.thindesk.service.ChamadoService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ChamadoController.class, properties = {"THINDESK_USERNAME=teste",
        "THINDESK_PASSWORD=senha-apenas-teste", "THINDESK_TECNICO_PASSWORD=senha-tecnico-teste"})
@Import({SecurityConfig.class, ChamadoService.class})
class ChamadoConsultaTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChamadoRepository repository;

    private Chamado chamado(String usuario) {
        Chamado chamado = new Chamado();
        chamado.setId("chamado-teste");
        chamado.setUsuario(usuario);
        chamado.setTitulo("Impressora");
        chamado.setStatus("ABERTO");
        return chamado;
    }

    @Test
    void deveListarSomenteChamadosDoSolicitante() throws Exception {
        when(repository.findByUsuarioOrderByDataAberturaDesc("teste")).thenReturn(List.of(chamado("teste")));
        mockMvc.perform(get("/api/chamados/usuario/teste")
                .header(HttpHeaders.AUTHORIZATION, SecurityConfigTest.basic("teste", "senha-apenas-teste")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].usuario").value("teste"));
    }

    @Test
    void deveRetornarListaVazia() throws Exception {
        when(repository.findByUsuarioOrderByDataAberturaDesc("teste")).thenReturn(List.of());
        mockMvc.perform(get("/api/chamados/usuario/teste")
                .header(HttpHeaders.AUTHORIZATION, SecurityConfigTest.basic("teste", "senha-apenas-teste")))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void naoDeveListarChamadosDeOutroSolicitante() throws Exception {
        mockMvc.perform(get("/api/chamados/usuario/outro")
                .header(HttpHeaders.AUTHORIZATION, SecurityConfigTest.basic("teste", "senha-apenas-teste")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(repository);
    }

    @Test
    void deveConsultarDetalhesEStatus() throws Exception {
        when(repository.findById("chamado-teste")).thenReturn(Optional.of(chamado("teste")));
        mockMvc.perform(get("/api/chamados/chamado-teste")
                .header(HttpHeaders.AUTHORIZATION, SecurityConfigTest.basic("teste", "senha-apenas-teste")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Impressora"))
                .andExpect(jsonPath("$.status").value("ABERTO"));
    }

    @Test
    void deveRetornar404ParaChamadoInexistente() throws Exception {
        when(repository.findById("inexistente")).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/chamados/inexistente")
                .header(HttpHeaders.AUTHORIZATION, SecurityConfigTest.basic("teste", "senha-apenas-teste")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void deveRestringirConsultaIndividualPorDonoEPermitirTecnico() throws Exception {
        when(repository.findById("chamado-teste")).thenReturn(Optional.of(chamado("outro")));
        mockMvc.perform(get("/api/chamados/chamado-teste")
                .header(HttpHeaders.AUTHORIZATION, SecurityConfigTest.basic("teste", "senha-apenas-teste")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/chamados/chamado-teste")
                .header(HttpHeaders.AUTHORIZATION, SecurityConfigTest.basic("tecnico", "senha-tecnico-teste")))
                .andExpect(status().isOk());
        when(repository.findByUsuarioOrderByDataAberturaDesc("outro")).thenReturn(List.of(chamado("outro")));
        mockMvc.perform(get("/api/chamados/usuario/outro")
                .header(HttpHeaders.AUTHORIZATION, SecurityConfigTest.basic("tecnico", "senha-tecnico-teste")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].usuario").value("outro"));
    }
}
