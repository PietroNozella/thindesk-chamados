package com.pfc.thindesk.controller;

import com.pfc.thindesk.dto.AbrirChamadoRequest;
import com.pfc.thindesk.entity.Chamado;
import com.pfc.thindesk.service.ChamadoService;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chamados")
public class ChamadoController {

    private final ChamadoService chamadoService;

    public ChamadoController(ChamadoService chamadoService) {
        this.chamadoService = chamadoService;
    }

    @PostMapping
    public ResponseEntity<Chamado> criarChamado(@RequestBody AbrirChamadoRequest pedido,
            Authentication autenticacao) {
        Chamado chamado = chamadoService.criarChamado(pedido, autenticacao.getName());
        return ResponseEntity.created(URI.create("/api/chamados/" + chamado.getId())).body(chamado);
    }
}
