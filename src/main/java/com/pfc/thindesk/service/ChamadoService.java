package com.pfc.thindesk.service;

import com.pfc.thindesk.dto.AbrirChamadoRequest;
import com.pfc.thindesk.entity.Chamado;
import com.pfc.thindesk.repository.ChamadoRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class ChamadoService {

    private final ChamadoRepository chamadoRepository;

    public ChamadoService(ChamadoRepository chamadoRepository) {
        this.chamadoRepository = chamadoRepository;
    }

    public Chamado criarChamado(AbrirChamadoRequest pedido, String solicitante) {
        Chamado chamado = new Chamado();
        chamado.setTitulo(ValidacaoTexto.obrigatorio(pedido.titulo(), "Título", 150));
        chamado.setDescricao(ValidacaoTexto.obrigatorio(pedido.descricao(), "Descrição", 5000));
        chamado.setUsuario(solicitante);
        chamado.setDataAbertura(Instant.now());
        chamado.setStatus("ABERTO");
        return chamadoRepository.insert(chamado);
    }
}
