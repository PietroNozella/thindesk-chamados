package com.pfc.thindesk.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "clientes")
public class Cliente {
    
    @Id
    private String id;
    @NotBlank(message = "Informe o nome do cliente.")
    @Size(max = 150, message = "O nome deve ter até 150 caracteres.")
    private String nome;
    @Size(max = 30, message = "O telefone deve ter até 30 caracteres.")
    private String telefone;
    @NotBlank(message = "Informe o setor.")
    @Size(max = 100, message = "O setor deve ter até 100 caracteres.")
    private String setor;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getSetor() {
        return setor;
    }

    public void setSetor(String setor) {
        this.setor = setor;
    }
    
    

}
