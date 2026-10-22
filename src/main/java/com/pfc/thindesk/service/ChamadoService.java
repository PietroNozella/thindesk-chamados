package com.pfc.thindesk.service;

import com.pfc.thindesk.dto.AbrirChamadoRequest;
import com.pfc.thindesk.entity.Chamado;
import com.pfc.thindesk.repository.ChamadoRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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

    private void verificarPermissaoConsulta(String usuario, Authentication autenticacao) {
        boolean tecnico = autenticacao.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_TECNICO"));
        if (!tecnico && !autenticacao.getName().equals(usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso não permitido a chamados de outro usuário.");
        }
    }
}
