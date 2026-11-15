package com.pfc.thindesk.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


@Document(collection = "chamados")
public class Chamado{
    
    @Id
    private String id;
    @PositiveOrZero(message = "A revisão do chamado é inválida.")
    private Long revisao;
    @NotBlank(message = "Informe a descrição do chamado.")
    @Size(max = 2000, message = "A descrição deve ter até 2000 caracteres.")
    private String descricao;
    @NotBlank(message = "Selecione o status.")
    @Pattern(regexp = "ABERTO|EM_ANDAMENTO|CONCLUIDO|CANCELADO", message = "Selecione um status válido.")
    private String status;
    @NotBlank(message = "Informe o tipo do chamado.")
    @Size(max = 100, message = "O tipo deve ter até 100 caracteres.")
    private String tipo;
    @Size(max = 100, message = "O técnico deve ter até 100 caracteres.")
    private String tecnico;
    @NotBlank(message = "Informe o solicitante.")
    @Size(max = 100, message = "O solicitante deve ter até 100 caracteres.")
    private String usuario;

    public String getId() {
        return id;
    }

    public Long getRevisao() {
        return revisao;
    }

    public void setRevisao(Long revisao) {
        this.revisao = revisao;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getTecnico() {
        return tecnico;
    }

    public void setTecnico(String tecnico) {
        this.tecnico = tecnico;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }
    
    
    
}
