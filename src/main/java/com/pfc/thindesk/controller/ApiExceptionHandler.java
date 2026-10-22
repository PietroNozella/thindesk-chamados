package com.pfc.thindesk.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErroApi> tratarRegra(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode())
                .body(new ErroApi(exception.getStatusCode().value(), exception.getReason()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroApi> tratarJsonInvalido() {
        return ResponseEntity.badRequest().body(new ErroApi(400, "JSON inválido ou campos não permitidos."));
    }
}
