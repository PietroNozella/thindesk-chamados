package com.pfc.thindesk.service;

import com.pfc.thindesk.entity.Perfil;
import com.pfc.thindesk.entity.Usuario;
import com.pfc.thindesk.repository.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UsuarioService {
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private MongoTemplate mongoTemplate;

    @Value("${app.login.max-tentativas:5}") private int maxTentativas;
    @Value("${app.login.bloqueio-minutos:15}") private long bloqueioMinutos;

    public Usuario cadastrar(String nome, String sobrenome, String email,
            String username, String senhaInicial, Perfil perfil) {
        validarSenha(senhaInicial);
        if (usuarioRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Nome de usuário já existe.");
        }
        if (usuarioRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("E-mail já cadastrado.");
        }
        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setSobrenome(sobrenome);
        usuario.setEmail(email);
        usuario.setUsername(username);
        usuario.setSenha(passwordEncoder.encode(senhaInicial));
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        usuario.setCriadoEm(Instant.now());
        try {
            return usuarioRepository.save(usuario);
        } catch (DuplicateKeyException e) {
            throw new IllegalArgumentException("Nome de usuário ou e-mail já cadastrado.");
        }
    }

    public List<Usuario> listar() {
        return usuarioRepository.findAll();
    }

    public Usuario buscarPorId(String id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
    }

    public void trocarSenha(String username, String senhaAtual, String senhaNova) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
        if (!passwordEncoder.matches(senhaAtual, usuario.getSenha())) {
            throw new IllegalArgumentException("Senha atual incorreta.");
        }
        atualizarSenha(usuario, senhaNova);
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public void redefinirSenhaPorAdministrador(String administrador, String usuarioId,
            String senhaAtual, String senhaNova) {
        Usuario admin = usuarioRepository.findByUsername(administrador)
                .orElseThrow(() -> new IllegalArgumentException("Administrador não encontrado."));
        if (!passwordEncoder.matches(senhaAtual, admin.getSenha())) {
            throw new IllegalArgumentException("Senha do administrador incorreta.");
        }
        Usuario alvo = buscarPorId(usuarioId);
        if (admin.getId().equals(alvo.getId())) {
            throw new IllegalArgumentException("Use a opção Trocar senha para alterar sua própria senha.");
        }
        atualizarSenha(alvo, senhaNova);
    }

    private void atualizarSenha(Usuario usuario, String senhaNova) {
        validarSenha(senhaNova);
        var resultado = mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(usuario.getId()).and("senha").is(usuario.getSenha())),
                new Update().set("senha", passwordEncoder.encode(senhaNova))
                        .inc("versaoCredencial", 1).set("falhasLogin", 0).unset("bloqueadoAte"), Usuario.class);
        if (resultado.getModifiedCount() != 1) {
            throw new IllegalArgumentException("A conta foi alterada durante a operação. Recarregue e tente novamente.");
        }
    }

    private void validarSenha(String senha) {
        if (senha == null || senha.isBlank() || senha.length() < 8) {
            throw new IllegalArgumentException("A senha deve ter ao menos 8 caracteres.");
        }
        if (senha.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("A senha é muito longa. Use uma senha menor.");
        }
    }

    public boolean registrarFalha(String username) {
        Usuario usuario = mongoTemplate.findAndModify(Query.query(Criteria.where("username").is(username)),
                new Update().inc("falhasLogin", 1), FindAndModifyOptions.options().returnNew(true), Usuario.class);
        if (usuario == null || usuario.getFalhasLogin() < maxTentativas) return false;
        var resultado = mongoTemplate.updateFirst(
                Query.query(Criteria.where("username").is(username).and("falhasLogin").gte(maxTentativas)),
                new Update().set("bloqueadoAte", Instant.now().plus(Duration.ofMinutes(bloqueioMinutos)))
                        .set("falhasLogin", 0), Usuario.class);
        return resultado.getModifiedCount() == 1;
    }

    public void registrarSucesso(String username) {
        mongoTemplate.updateFirst(Query.query(Criteria.where("username").is(username)),
                new Update().set("falhasLogin", 0).unset("bloqueadoAte"), Usuario.class);
    }

    public void registrarAceite(String username, String termosVersao, String privacidadeVersao) {
        mongoTemplate.updateFirst(Query.query(Criteria.where("username").is(username)),
                new Update().set("aceiteTermosVersao", termosVersao)
                        .set("aceitePrivacidadeVersao", privacidadeVersao).set("aceiteEm", Instant.now()), Usuario.class);
    }
}
