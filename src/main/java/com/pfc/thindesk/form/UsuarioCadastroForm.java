package com.pfc.thindesk.form;

import com.pfc.thindesk.entity.Perfil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UsuarioCadastroForm {
    @NotBlank(message = "Informe o nome.") @Size(max = 150)
    private String nome;
    @NotBlank(message = "Informe o sobrenome.") @Size(max = 150)
    private String sobrenome;
    @NotBlank(message = "Informe o e-mail.") @Email(message = "Informe um e-mail válido.") @Size(max = 254)
    private String email;
    @NotBlank(message = "Informe o nome de usuário.") @Size(max = 150)
    @Pattern(regexp = "[A-Za-z0-9._-]+", message = "Use apenas letras sem acento, números, ponto, hífen ou sublinhado no usuário.")
    private String username;
    @NotBlank(message = "Informe a senha inicial.") @Size(min = 8, max = 72, message = "A senha deve ter de 8 a 72 caracteres.")
    private String senha;
    @NotNull(message = "Selecione o perfil.")
    private Perfil perfil;
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getSobrenome() { return sobrenome; }
    public void setSobrenome(String sobrenome) { this.sobrenome = sobrenome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    public Perfil getPerfil() { return perfil; }
    public void setPerfil(Perfil perfil) { this.perfil = perfil; }
}