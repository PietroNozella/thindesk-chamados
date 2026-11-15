package com.pfc;

import com.pfc.thindesk.entity.Perfil;
import com.pfc.thindesk.entity.Usuario;
import com.pfc.thindesk.repository.UsuarioRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Cria a conta administradora inicial no primeiro boot, se ainda não existir.
// Equivale ao "superuser técnico" do SIGEE que bootstrapa o primeiro admin.
@Component
@Order(2)
public class AdminSeed implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeed.class);

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:thindesk123}")
    private String adminPassword;

    @Value("${app.admin.email:admin@thindesk.local}")
    private String adminEmail;

    @Value("${app.admin.nome:Administrador}")
    private String adminNome;

    @Override
    public void run(String... args) {
        if (usuarioRepository.findByUsername(adminUsername).isPresent()) {
            return;
        }
        Usuario admin = new Usuario();
        admin.setUsername(adminUsername);
        admin.setSenha(passwordEncoder.encode(adminPassword));
        admin.setEmail(adminEmail);
        admin.setNome(adminNome);
        admin.setSobrenome("Sistema");
        admin.setPerfil(Perfil.ADMINISTRADOR);
        admin.setAtivo(true);
        admin.setCriadoEm(Instant.now());
        usuarioRepository.save(admin);
        log.info("Conta administradora inicial criada: {}", adminUsername);
    }
}
