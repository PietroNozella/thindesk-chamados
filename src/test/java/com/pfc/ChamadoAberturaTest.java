package com.pfc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ChamadoController.class, properties = {"THINDESK_USERNAME=teste",
        "THINDESK_PASSWORD=senha-apenas-teste", "THINDESK_TECNICO_PASSWORD=senha-tecnico-teste",
        "spring.jackson.deserialization.fail-on-unknown-properties=true"})
@Import({SecurityConfig.class, ChamadoService.class})
class ChamadoAberturaTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChamadoRepository repository;

    @MockitoBean
    private MongoTemplate mongoTemplate;

    @Test
    void devePreencherDadosAutomaticos() throws Exception {
        when(repository.insert(any(Chamado.class))).thenAnswer(invocation -> {
            Chamado chamado = invocation.getArgument(0);
            assertThat(chamado.getId()).isNull();
            assertThat(chamado.getDataAbertura()).isNotNull();
            chamado.setId("id-gerado");
            return chamado;
        });
        mockMvc.perform(post("/api/chamados")
                .header(HttpHeaders.AUTHORIZATION, SecurityConfigTest.basic("teste", "senha-apenas-teste"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"titulo\":\" Impressora parada \",\"descricao\":\" Não imprime. \"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/chamados/id-gerado"))
                .andExpect(jsonPath("$.titulo").value("Impressora parada"))
                .andExpect(jsonPath("$.usuario").value("teste"))
                .andExpect(jsonPath("$.status").value("ABERTO"));
    }

    @Test
    void deveRejeitarCamposInvalidosOuControladosPeloServidor() throws Exception {
        String[] pedidos = {"{}", "{\"titulo\":\" \",\"descricao\":\"Teste\"}",
                "{\"titulo\":\"Teste\",\"descricao\":null}",
                "{\"titulo\":\"Teste\",\"descricao\":\"Teste\",\"usuario\":\"outro\"}",
                "{\"titulo\":\"Teste\",\"descricao\":\"Teste\",\"id\":\"existente\"}",
                "{\"titulo\":\"" + "a".repeat(151) + "\",\"descricao\":\"Teste\"}",
                "{\"titulo\":\"Teste\",\"descricao\":\"" + "a".repeat(5001) + "\"}"};
        for (String pedido : pedidos) {
            mockMvc.perform(post("/api/chamados")
                    .header(HttpHeaders.AUTHORIZATION, SecurityConfigTest.basic("teste", "senha-apenas-teste"))
                    .contentType(MediaType.APPLICATION_JSON).content(pedido))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(repository);
    }

    @Test
    void tecnicoNaoDeveAbrirChamadoComoSolicitante() throws Exception {
        mockMvc.perform(post("/api/chamados")
                .header(HttpHeaders.AUTHORIZATION, SecurityConfigTest.basic("tecnico", "senha-tecnico-teste"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"titulo\":\"Teste\",\"descricao\":\"Teste\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(repository);
    }
}
