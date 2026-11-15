package com.pfc.thindesk.entity;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

// Registro imutável de auditoria: autenticação, acessos negados e ações relevantes.
// Somente leitura pela tela /auditoria (restrita ao Administrador).
@Document(collection = "auditoria")
public class RegistroAuditoria {

    public enum Acao {
        LOGIN,
        LOGOUT,
        CONTA_BLOQUEADA,
        USUARIO_CADASTRADO,
        SENHA_TROCADA,
        RECOVERY_SOLICITADO,
        SENHA_REDEFINIDA,
        ACEITE_REGISTRADO,
        ACESSO_NEGADO
    }

    public enum Resultado {
        SUCESSO,
        FALHA
    }

    @Id
    private String id;
    private Instant quando = Instant.now();
    private String usuario;
    private String ip;
    private Acao acao;
    private Resultado resultado;
    private String entidade;
    private String entidadeId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Instant getQuando() {
        return quando;
    }

    public void setQuando(Instant quando) {
        this.quando = quando;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Acao getAcao() {
        return acao;
    }

    public void setAcao(Acao acao) {
        this.acao = acao;
    }

    public Resultado getResultado() {
        return resultado;
    }

    public void setResultado(Resultado resultado) {
        this.resultado = resultado;
    }

    public String getEntidade() {
        return entidade;
    }

    public void setEntidade(String entidade) {
        this.entidade = entidade;
    }

    public String getEntidadeId() {
        return entidadeId;
    }

    public void setEntidadeId(String entidadeId) {
        this.entidadeId = entidadeId;
    }
}
