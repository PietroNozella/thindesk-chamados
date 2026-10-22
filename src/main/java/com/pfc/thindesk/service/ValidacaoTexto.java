package com.pfc.thindesk.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

final class ValidacaoTexto {

    private ValidacaoTexto() {
    }

    static String obrigatorio(String valor, String campo, int limite) {
        if (valor == null || valor.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, campo + " deve ser preenchido.");
        }
        String texto = valor.strip();
        if (texto.length() > limite) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    campo + " deve ter no máximo " + limite + " caracteres.");
        }
        return texto;
    }
}
