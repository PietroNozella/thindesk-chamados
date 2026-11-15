package com.pfc.thindesk.entity;

// Perfis funcionais — mesma ideia dos grupos do SIGEE
// (Administrador cadastra, Operador e Professor usam conforme permissão).
public enum Perfil {
    ADMINISTRADOR,
    OPERADOR,
    PROFESSOR;

    public String autoridade() {
        return "ROLE_" + name();
    }
}
