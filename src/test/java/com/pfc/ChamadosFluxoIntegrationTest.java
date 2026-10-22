package com.pfc;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfc.thindesk.entity.Chamado;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfEnvironmentVariable(named = "MONGO_E2E", matches = "true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChamadosFluxoIntegrationTest {

    private static final String DATABASE = "thindesk_fluxo_test_" + UUID.randomUUID().toString().replace("-", "");
    private static final String SENHA = "senha-exclusiva-dos-testes";
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private UserDetailsService usuarios;

    @Autowired
    private PasswordEncoder encoder;

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry properties) {
        properties.add("spring.data.mongodb.uri", () -> System.getenv().getOrDefault(
                "MONGO_E2E_URI", "mongodb://localhost:27030"));
        properties.add("spring.data.mongodb.database", () -> DATABASE);
        properties.add("spring.security.user.name", () -> "solicitante-e2e");
        properties.add("spring.security.user.password", () -> SENHA);
        properties.add("thindesk.security.tecnico.username", () -> "tecnico-e2e");
        properties.add("thindesk.security.tecnico.password", () -> SENHA);
    }

    @BeforeAll
    void cadastrarSegundoSolicitanteApenasNoTeste() {
        ((InMemoryUserDetailsManager) usuarios).createUser(User.withUsername("outro-e2e")
                .password(encoder.encode(SENHA)).roles("SOLICITANTE").build());
    }

    @AfterAll
    void limparApenasBancoDesteTeste() {
        http.close();
        assertThat(mongoTemplate.getDb().getName()).isEqualTo(DATABASE);
        assertThat(DATABASE).startsWith("thindesk_fluxo_test_");
        mongoTemplate.getDb().drop();
    }

    private HttpResponse<String> requisitar(String metodo, String rota, String usuario, Object corpo) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + rota))
                .timeout(Duration.ofSeconds(15)).header("Accept", "application/json");
        if (usuario != null) {
            builder.header("Authorization", SecurityConfigTest.basic(usuario, SENHA));
        }
        if (corpo != null) {
            builder.header("Content-Type", "application/json");
        }
        builder.method(metodo, corpo == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(corpo)));
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode abrir(String usuario) throws Exception {
        var resposta = requisitar("POST", "/api/chamados", usuario,
                Map.of("titulo", "Impressora sem conexão", "descricao", "A impressora não aparece na rede."));
        assertThat(resposta.statusCode()).isEqualTo(201);
        JsonNode chamado = mapper.readTree(resposta.body());
        assertThat(resposta.headers().firstValue("Location")).contains("/api/chamados/" + chamado.get("id").asText());
        return chamado;
    }

    @Test
    void deveExecutarFluxoCompletoEPreservarChamadoResolvido() throws Exception {
        JsonNode aberto = abrir("solicitante-e2e");
        String id = aberto.get("id").asText();
        String rota = "/api/chamados/" + id;
        assertThat(aberto.get("usuario").asText()).isEqualTo("solicitante-e2e");
        assertThat(aberto.get("status").asText()).isEqualTo("ABERTO");
        assertThat(aberto.get("solucao").isNull()).isTrue();
        assertThat(aberto.get("dataResolucao").isNull()).isTrue();
        Chamado persistido = mongoTemplate.findById(id, Chamado.class);
        assertThat(persistido).isNotNull();
        assertThat(persistido.getDataAbertura()).isEqualTo(Instant.parse(aberto.get("dataAbertura").asText()));

        var lista = requisitar("GET", "/api/chamados/usuario/solicitante-e2e", "solicitante-e2e", null);
        assertThat(lista.statusCode()).isEqualTo(200);
        List<String> ids = new ArrayList<>();
        mapper.readTree(lista.body()).forEach(item -> ids.add(item.get("id").asText()));
        assertThat(ids).contains(id);

        var consulta = requisitar("GET", rota, "solicitante-e2e", null);
        assertThat(consulta.statusCode()).isEqualTo(200);
        assertThat(mapper.readTree(consulta.body())).isEqualTo(aberto);
        var resposta = requisitar("PUT", rota + "/resolver", "tecnico-e2e",
                Map.of("solucao", " Configuração de rede corrigida. "));
        assertThat(resposta.statusCode()).isEqualTo(200);
        JsonNode resolvido = mapper.readTree(resposta.body());
        assertThat(resolvido.get("status").asText()).isEqualTo("RESOLVIDO");
        assertThat(resolvido.get("solucao").asText()).isEqualTo("Configuração de rede corrigida.");
        assertThat(resolvido.get("tecnico").asText()).isEqualTo("tecnico-e2e");
        assertThat(resolvido.get("dataAbertura")).isEqualTo(aberto.get("dataAbertura"));
        assertThat(Instant.parse(resolvido.get("dataResolucao").asText()))
                .isAfterOrEqualTo(persistido.getDataAbertura());

        assertThat(requisitar("PUT", rota + "/resolver", "tecnico-e2e",
                Map.of("solucao", "Tentativa de sobrescrita")).statusCode()).isEqualTo(409);
        assertThat(requisitar("PUT", rota, "tecnico-e2e", Map.of("status", "ABERTO")).statusCode()).isEqualTo(403);
        assertThat(requisitar("POST", "/api/chamados", "solicitante-e2e",
                Map.of("id", id, "titulo", "Sobrescrita", "descricao", "Tentativa")).statusCode()).isEqualTo(400);
        assertThat(mapper.readTree(requisitar("GET", rota, "solicitante-e2e", null).body())).isEqualTo(resolvido);
        Chamado finalizado = mongoTemplate.findById(id, Chamado.class);
        assertThat(finalizado.getSolucao()).isEqualTo("Configuração de rede corrigida.");
        assertThat(finalizado.getUsuario()).isEqualTo("solicitante-e2e");
        assertThat(finalizado.getTitulo()).isEqualTo(aberto.get("titulo").asText());
    }

    @Test
    void deveIsolarSolicitantesEPermitirAcompanhamentoDoTecnico() throws Exception {
        JsonNode chamado = abrir("outro-e2e");
        String rota = "/api/chamados/" + chamado.get("id").asText();
        assertThat(requisitar("GET", rota, "solicitante-e2e", null).statusCode()).isEqualTo(403);
        assertThat(requisitar("GET", "/api/chamados/usuario/outro-e2e", "solicitante-e2e", null).statusCode())
                .isEqualTo(403);
        assertThat(requisitar("GET", rota, "tecnico-e2e", null).statusCode()).isEqualTo(200);
        assertThat(requisitar("GET", "/api/chamados/usuario/outro-e2e", "tecnico-e2e", null).statusCode())
                .isEqualTo(200);
        assertThat(requisitar("PUT", rota + "/resolver", "outro-e2e", Map.of("solucao", "Tentativa"))
                .statusCode()).isEqualTo(403);
        assertThat(mongoTemplate.findById(chamado.get("id").asText(), Chamado.class).getStatus()).isEqualTo("ABERTO");
    }

    @Test
    void deveRejeitarDadosInvalidosSemPersistir() throws Exception {
        long quantidade = mongoTemplate.getCollection("chamados").countDocuments();
        List<Map<String, Object>> pedidos = List.of(Map.of(), Map.of("titulo", " ", "descricao", "Teste"),
                Map.of("titulo", "Teste", "descricao", " "),
                Map.of("titulo", "Teste", "descricao", "Teste", "status", "RESOLVIDO"),
                Map.of("titulo", "Teste", "descricao", "Teste", "usuario", "outro-e2e"),
                Map.of("titulo", "a".repeat(151), "descricao", "Teste"));
        for (var pedido : pedidos) {
            assertThat(requisitar("POST", "/api/chamados", "solicitante-e2e", pedido).statusCode()).isEqualTo(400);
        }
        assertThat(mongoTemplate.getCollection("chamados").countDocuments()).isEqualTo(quantidade);
        JsonNode chamado = abrir("solicitante-e2e");
        String id = chamado.get("id").asText();
        assertThat(requisitar("PUT", "/api/chamados/" + id + "/resolver", "tecnico-e2e",
                Map.of("solucao", " ")).statusCode()).isEqualTo(400);
        assertThat(mongoTemplate.findById(id, Chamado.class).getStatus()).isEqualTo("ABERTO");
    }

    @Test
    void deveExigirAutenticacaoETratarInexistentes() throws Exception {
        for (String rota : List.of("/api/autenticacao", "/api/chamados/usuario/solicitante-e2e", "/api/chamados/inexistente")) {
            assertThat(requisitar("GET", rota, null, null).statusCode()).isEqualTo(401);
        }
        assertThat(requisitar("POST", "/api/chamados", null, Map.of()).statusCode()).isEqualTo(401);
        assertThat(requisitar("PUT", "/api/chamados/inexistente/resolver", null, Map.of()).statusCode()).isEqualTo(401);
        assertThat(requisitar("GET", "/api/autenticacao", "usuario-invalido", null).statusCode()).isEqualTo(401);
        assertThat(requisitar("GET", "/api/chamados/inexistente", "solicitante-e2e", null).statusCode()).isEqualTo(404);
        assertThat(requisitar("PUT", "/api/chamados/inexistente/resolver", "tecnico-e2e",
                Map.of("solucao", "Teste")).statusCode()).isEqualTo(404);
        var listaVazia = requisitar("GET", "/api/chamados/usuario/sem-chamados", "tecnico-e2e", null);
        assertThat(listaVazia.statusCode()).isEqualTo(200);
        assertThat(mapper.readTree(listaVazia.body()).isEmpty()).isTrue();
    }

    @Test
    void apenasUmaResolucaoSimultaneaDeveVencer() throws Exception {
        String id = abrir("solicitante-e2e").get("id").asText();
        CountDownLatch prontas = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var primeira = executor.submit(() -> {
                prontas.countDown();
                assertThat(iniciar.await(5, TimeUnit.SECONDS)).isTrue();
                return requisitar("PUT", "/api/chamados/" + id + "/resolver", "tecnico-e2e", Map.of("solucao", "Solução A"));
            });
            var segunda = executor.submit(() -> {
                prontas.countDown();
                assertThat(iniciar.await(5, TimeUnit.SECONDS)).isTrue();
                return requisitar("PUT", "/api/chamados/" + id + "/resolver", "tecnico-e2e", Map.of("solucao", "Solução B"));
            });
            assertThat(prontas.await(5, TimeUnit.SECONDS)).isTrue();
            iniciar.countDown();
            var respostaA = primeira.get(20, TimeUnit.SECONDS);
            var respostaB = segunda.get(20, TimeUnit.SECONDS);
            assertThat(List.of(respostaA.statusCode(), respostaB.statusCode())).containsExactlyInAnyOrder(200, 409);
            var vencedora = mapper.readTree((respostaA.statusCode() == 200 ? respostaA : respostaB).body());
            Chamado persistido = mongoTemplate.findById(id, Chamado.class);
            assertThat(persistido.getStatus()).isEqualTo("RESOLVIDO");
            assertThat(persistido.getSolucao()).isEqualTo(vencedora.get("solucao").asText());
            assertThat(persistido.getDataResolucao()).isEqualTo(Instant.parse(vencedora.get("dataResolucao").asText()));
        }
    }
}
