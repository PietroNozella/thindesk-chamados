package com.pfc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pfc.thindesk.controller.ChamadoController;
import com.pfc.thindesk.entity.Chamado;
import com.pfc.thindesk.repository.ChamadoRepository;
import com.pfc.thindesk.service.ChamadoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ChamadoController.class, properties = {"THINDESK_USERNAME=teste",
        "THINDESK_PASSWORD=senha-apenas-teste", "THINDESK_TECNICO_PASSWORD=senha-tecnico-teste"})
@Import({SecurityConfig.class, ChamadoService.class})
class ChamadoResolucaoTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChamadoRepository repository;

    @MockitoBean
    private MongoTemplate mongoTemplate;

    @Test
    void deveRetornarChamadoResolvido() throws Exception {
        Chamado chamado = new Chamado();
        chamado.setStatus("RESOLVIDO");
        chamado.setSolucao("Driver reinstalado.");
        chamado.setTecnico("tecnico");
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class),
                any(FindAndModifyOptions.class), eq(Chamado.class))).thenReturn(chamado);
        mockMvc.perform(put("/api/chamados/id/resolver")
                .header(HttpHeaders.AUTHORIZATION, SecurityConfigTest.basic("tecnico", "senha-tecnico-teste"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"solucao\":\"Driver reinstalado.\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RESOLVIDO"));
    }

    @Test
    void deveRejeitarSolucaoInvalida() throws Exception {
        for (String json : new String[]{"{}", "{\"solucao\":\" \"}",
                "{\"solucao\":\"" + "a".repeat(5001) + "\"}"}) {
            mockMvc.perform(put("/api/chamados/id/resolver")
                    .header(HttpHeaders.AUTHORIZATION, SecurityConfigTest.basic("tecnico", "senha-tecnico-teste"))
                    .contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(mongoTemplate, repository);
    }

    @Test
    void deveDistinguirInexistenteDeChamadoJaResolvido() throws Exception {
        when(repository.existsById("resolvido")).thenReturn(true);
        mockMvc.perform(put("/api/chamados/inexistente/resolver")
                .header(HttpHeaders.AUTHORIZATION, SecurityConfigTest.basic("tecnico", "senha-tecnico-teste"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"solucao\":\"Teste\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/chamados/resolvido/resolver")
                .header(HttpHeaders.AUTHORIZATION, SecurityConfigTest.basic("tecnico", "senha-tecnico-teste"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"solucao\":\"Teste\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void deveBloquearSolicitanteEAtualizacaoGenerica() throws Exception {
        mockMvc.perform(put("/api/chamados/id/resolver")
                .header(HttpHeaders.AUTHORIZATION, SecurityConfigTest.basic("teste", "senha-apenas-teste"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"solucao\":\"Teste\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/chamados/id")
                .header(HttpHeaders.AUTHORIZATION, SecurityConfigTest.basic("tecnico", "senha-tecnico-teste"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ABERTO\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(mongoTemplate, repository);
    }
}
