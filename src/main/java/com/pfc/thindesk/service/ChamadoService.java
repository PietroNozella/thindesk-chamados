package com.pfc.thindesk.service;

import com.pfc.thindesk.entity.Chamado;
import com.pfc.thindesk.repository.ChamadoRepository;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ChamadoService {
    @Autowired private ChamadoRepository chamadoRepository;
    @Autowired private MongoTemplate mongoTemplate;

    public Chamado criarChamado(Chamado chamado) {
        chamado.setId(null);
        chamado.setRevisao(0L);
        return chamadoRepository.save(chamado);
    }

    public List<Chamado> listarChamados() {
        List<Chamado> chamados = chamadoRepository.findAll();
        chamados.forEach(this::normalizarLeitura);
        return chamados;
    }

    public Chamado buscarChamado(String id) {
        Chamado chamado = chamadoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chamado não encontrado."));
        normalizarLeitura(chamado);
        return chamado;
    }

    public void cancelarChamado(String id, Long revisao) {
        alterar(id, revisao, new Update().set("status", "CANCELADO"));
    }

    public Chamado atualizarChamado(String id, Chamado chamado) {
        return alterar(id, chamado.getRevisao(), new Update()
                .set("descricao", chamado.getDescricao()).set("status", chamado.getStatus())
                .set("tipo", chamado.getTipo()).set("tecnico", chamado.getTecnico()).set("usuario", chamado.getUsuario()));
    }

    private Chamado alterar(String id, Long revisao, Update update) {
        if (revisao == null || revisao < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recarregue o chamado antes de alterar.");
        }
        Criteria criterioRevisao = revisao == 0
                ? new Criteria().orOperator(Criteria.where("revisao").is(0), Criteria.where("revisao").is(null))
                : Criteria.where("revisao").is(revisao);
        Chamado alterado = mongoTemplate.findAndModify(
                Query.query(new Criteria().andOperator(Criteria.where("id").is(id), criterioRevisao)),
                update.set("revisao", revisao + 1), FindAndModifyOptions.options().returnNew(true), Chamado.class);
        if (alterado == null) {
            if (!chamadoRepository.existsById(id)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chamado não encontrado.");
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Este chamado foi alterado por outra operação. Recarregue e confira os dados antes de salvar.");
        }
        return alterado;
    }

    private void normalizarLeitura(Chamado chamado) {
        if (chamado.getRevisao() == null) chamado.setRevisao(0L);
        if (chamado.getStatus() == null) {
            chamado.setStatus("NÃO INFORMADO");
            return;
        }
        String status = Normalizer.normalize(chamado.getStatus(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").strip().toUpperCase(Locale.ROOT).replace(' ', '_');
        if (List.of("ABERTO", "EM_ANDAMENTO", "CONCLUIDO", "CANCELADO").contains(status)) chamado.setStatus(status);
    }
}