package com.pfc.thindesk.entity;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "usuarios")
public class Usuario {

    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    @Indexed(unique = true)
    private String email;

    private String nome;
    private String sobrenome;
    private String senha; // hash BCrypt, nunca texto puro
    private long versaoCredencial;
    private Perfil perfil;
    private boolean ativo = true;
    private Instant criadoEm = Instant.now();

    // Bloqueio após tentativas inválidas
    private int falhasLogin = 0;
    private Instant bloqueadoAte;

    // Aceite versionado de documentos legais
    private String aceiteTermosVersao;
    private String aceitePrivacidadeVersao;
    private Instant aceiteEm;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getSobrenome() {
        return sobrenome;
    }

    public void setSobrenome(String sobrenome) {
        this.sobrenome = sobrenome;
    }

    public String getSenha() {
        return senha;
    }

    public long getVersaoCredencial() {
        return versaoCredencial;
    }

    public void setVersaoCredencial(long versaoCredencial) {
        this.versaoCredencial = versaoCredencial;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public void setPerfil(Perfil perfil) {
        this.perfil = perfil;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(Instant criadoEm) {
        this.criadoEm = criadoEm;
    }

    public int getFalhasLogin() {
        return falhasLogin;
    }

    public void setFalhasLogin(int falhasLogin) {
        this.falhasLogin = falhasLogin;
    }

    public Instant getBloqueadoAte() {
        return bloqueadoAte;
    }

    public void setBloqueadoAte(Instant bloqueadoAte) {
        this.bloqueadoAte = bloqueadoAte;
    }

    public boolean bloqueado() {
        return bloqueadoAte != null && Instant.now().isBefore(bloqueadoAte);
    }

    public String getAceiteTermosVersao() {
        return aceiteTermosVersao;
    }

    public void setAceiteTermosVersao(String aceiteTermosVersao) {
        this.aceiteTermosVersao = aceiteTermosVersao;
    }

    public String getAceitePrivacidadeVersao() {
        return aceitePrivacidadeVersao;
    }

    public void setAceitePrivacidadeVersao(String aceitePrivacidadeVersao) {
        this.aceitePrivacidadeVersao = aceitePrivacidadeVersao;
    }

    public Instant getAceiteEm() {
        return aceiteEm;
    }

    public void setAceiteEm(Instant aceiteEm) {
        this.aceiteEm = aceiteEm;
    }

    public boolean aceiteEmDia(String termosVersao, String privacidadeVersao) {
        return termosVersao.equals(aceiteTermosVersao)
                && privacidadeVersao.equals(aceitePrivacidadeVersao);
    }
}
