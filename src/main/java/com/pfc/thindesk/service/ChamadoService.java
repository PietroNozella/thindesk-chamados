package com.pfc.thindesk.service;

import com.pfc.thindesk.dto.AbrirChamadoRequest;
import com.pfc.thindesk.dto.ResolverChamadoRequest;
import com.pfc.thindesk.entity.Chamado;
import com.pfc.thindesk.repository.ChamadoRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ChamadoService {

    private final ChamadoRepository chamadoRepository;
    private final MongoTemplate mongoTemplate;

    public ChamadoService(ChamadoRepository chamadoRepository, MongoTemplate mongoTemplate) {
        this.chamadoRepository = chamadoRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public Chamado criarChamado(AbrirChamadoRequest pedido, String solicitante) {
        Chamado chamado = new Chamado();
        chamado.setTitulo(ValidacaoTexto.obrigatorio(pedido.titulo(), "Título", 150));
        chamado.setDescricao(ValidacaoTexto.obrigatorio(pedido.descricao(), "Descrição", 5000));
        chamado.setUsuario(solicitante);
        chamado.setDataAbertura(Instant.now().truncatedTo(ChronoUnit.MILLIS));
        chamado.setStatus("ABERTO");
        return chamadoRepository.insert(chamado);
    }

    public List<Chamado> listarPorUsuario(String usuario, Authentication autenticacao) {
        verificarPermissaoConsulta(usuario, autenticacao);
        return chamadoRepository.findByUsuarioOrderByDataAberturaDesc(usuario);
    }

    public Chamado consultar(String id, Authentication autenticacao) {
        Chamado chamado = chamadoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chamado não encontrado."));
        verificarPermissaoConsulta(chamado.getUsuario(), autenticacao);
        return chamado;
    }

    public Chamado resolver(String id, ResolverChamadoRequest pedido, String tecnico) {
        String solucao = ValidacaoTexto.obrigatorio(pedido.solucao(), "Solução", 5000);
        // A condição e a gravação são atômicas: somente a primeira resolução pode vencer.
        Query query = Query.query(Criteria.where("id").is(id).and("status").is("ABERTO"));
        Update update = new Update().set("solucao", solucao).set("tecnico", tecnico)
                .set("dataResolucao", Instant.now().truncatedTo(ChronoUnit.MILLIS)).set("status", "RESOLVIDO");
        Chamado resolvido = mongoTemplate.findAndModify(query, update,
                FindAndModifyOptions.options().returnNew(true), Chamado.class);
        if (resolvido != null) {
            return resolvido;
        }
        if (!chamadoRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chamado não encontrado.");
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Chamado não está aberto e não pode ser alterado.");
    }

    private void verificarPermissaoConsulta(String usuario, Authentication autenticacao) {
        boolean tecnico = autenticacao.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_TECNICO"));
        if (!tecnico && !autenticacao.getName().equals(usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso não permitido a chamados de outro usuário.");
        }
    }
}
