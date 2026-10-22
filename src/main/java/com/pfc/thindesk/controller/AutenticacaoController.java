package com.pfc.thindesk.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AutenticacaoController {

    @GetMapping("/api/autenticacao")
    public UsuarioAutenticado consultar(Authentication autenticacao) {
        String perfil = autenticacao.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_TECNICO"))
                ? "TECNICO" : "SOLICITANTE";
        return new UsuarioAutenticado(autenticacao.getName(), perfil);
    }

    public record UsuarioAutenticado(String usuario, String perfil) {
    }
}
