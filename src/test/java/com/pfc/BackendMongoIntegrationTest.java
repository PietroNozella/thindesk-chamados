package com.pfc;

import static org.assertj.core.api.Assertions.assertThat;

import com.pfc.thindesk.entity.Chamado;
import com.pfc.thindesk.entity.Cliente;
import com.pfc.thindesk.entity.HorarioAtendimento;
import com.pfc.thindesk.service.ChamadoService;
import com.pfc.thindesk.service.ClienteService;
import com.pfc.thindesk.service.HorarioAtendimentoService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfEnvironmentVariable(named = "MONGO_E2E", matches = "true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BackendMongoIntegrationTest {

    private static final String DATABASE = "thindesk_preparacao_test_" + UUID.randomUUID().toString().replace("-", "");

    @LocalServerPort
    private int port;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private ChamadoService chamadoService;

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private HorarioAtendimentoService horarioService;

    @DynamicPropertySource
    static void configurarAmbiente(DynamicPropertyRegistry properties) {
        properties.add("spring.data.mongodb.uri", () -> System.getenv().getOrDefault(
                "MONGO_E2E_URI", "mongodb://localhost:27030"));
        properties.add("spring.data.mongodb.database", () -> DATABASE);
        properties.add("spring.security.user.name", () -> "teste-preparacao");
        properties.add("spring.security.user.password", () -> UUID.randomUUID().toString());
        properties.add("thindesk.security.tecnico.password", () -> UUID.randomUUID().toString());
    }

    @AfterAll
    void removerApenasBancoDesteTeste() {
        assertThat(mongoTemplate.getDb().getName()).isEqualTo(DATABASE);
        assertThat(DATABASE).startsWith("thindesk_preparacao_test_");
        mongoTemplate.getDb().drop();
    }

    @Test
    void devePersistirOsDocumentosDaBaseNoMongoDB() {
        Chamado chamado = new Chamado();
        chamado.setDescricao("Verificação de persistência da base de aula.");
        chamado.setStatus("ABERTO");
        Chamado chamadoSalvo = chamadoService.criarChamado(chamado);
        assertThat(chamadoSalvo.getId()).isNotBlank();
        assertThat(mongoTemplate.findById(chamadoSalvo.getId(), Chamado.class).getDescricao())
                .isEqualTo(chamado.getDescricao());

        Cliente cliente = new Cliente();
        cliente.setNome("Cliente de teste");
        Cliente clienteSalvo = clienteService.novoCliente(cliente);
        assertThat(clienteSalvo.getId()).isNotBlank();
        assertThat(mongoTemplate.findById(clienteSalvo.getId(), Cliente.class).getNome())
                .isEqualTo(cliente.getNome());

        HorarioAtendimento horario = new HorarioAtendimento();
        horario.setSetor("Suporte");
        horario.setDiaSemana("Segunda-feira");
        horario.setHorarioInicio("08:00");
        horario.setHorarioFim("18:00");
        HorarioAtendimento horarioSalvo = horarioService.salvar(horario);
        assertThat(horarioSalvo.getId()).isNotBlank();
        assertThat(mongoTemplate.findById(horarioSalvo.getId(), HorarioAtendimento.class).getSetor())
                .isEqualTo("Suporte");
        assertThat(mongoTemplate.collectionExists("usuarios")).isTrue();
    }

    @Test
    void deveIniciarServidorSemExporRotasLegadas() throws Exception {
        try (HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
            var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/chamados"))
                    .timeout(Duration.ofSeconds(10)).GET().build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(401);
            assertThat(response.headers().firstValue("Location")).isEmpty();
        }
    }
}
