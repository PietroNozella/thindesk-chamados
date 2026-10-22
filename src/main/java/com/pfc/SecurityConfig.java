package com.pfc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfc.thindesk.controller.ErroApi;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(
            @Value("${spring.security.user.name}") String usuario,
            @Value("${spring.security.user.password}") String senha,
            @Value("${thindesk.security.tecnico.username}") String tecnico,
            @Value("${thindesk.security.tecnico.password}") String senhaTecnico,
            PasswordEncoder encoder) {
        validarCredenciais(usuario, senha);
        validarCredenciais(tecnico, senhaTecnico);
        if (usuario.equalsIgnoreCase(tecnico)) {
            throw new IllegalArgumentException("Solicitante e técnico devem ter nomes diferentes.");
        }
        return new InMemoryUserDetailsManager(
                User.withUsername(usuario).password(encoder.encode(senha)).roles("SOLICITANTE").build(),
                User.withUsername(tecnico).password(encoder.encode(senhaTecnico)).roles("TECNICO").build());
    }

    private void validarCredenciais(String usuario, String senha) {
        if (!usuario.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,63}")) {
            throw new IllegalArgumentException("Nome de login deve ter de 1 a 64 letras, números, pontos, hífens ou sublinhados.");
        }
        if (senha.isBlank() || senha.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Senha deve ser preenchida e ter no máximo 72 bytes em UTF-8.");
        }
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper mapper) throws Exception {
        http
            // API para Postman/Insomnia: credenciais por requisição, sem cookies ou formulários.
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/api/autenticacao").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/chamados").hasRole("SOLICITANTE")
                .requestMatchers(HttpMethod.GET, "/api/chamados/usuario/{id}", "/api/chamados/{id}")
                    .hasAnyRole("SOLICITANTE", "TECNICO")
                .requestMatchers(HttpMethod.PUT, "/api/chamados/{id}/resolver").hasRole("TECNICO")
                .anyRequest().denyAll())
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(basic -> basic.authenticationEntryPoint((request, response, exception) -> {
                response.setHeader("WWW-Authenticate", "Basic realm=\"ThinDesk\"");
                escreverErro(response, mapper, 401, "Autenticação necessária ou credenciais inválidas.");
            }))
            .logout(AbstractHttpConfigurer::disable)
            .requestCache(AbstractHttpConfigurer::disable)
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, exception) -> {
                    response.setHeader("WWW-Authenticate", "Basic realm=\"ThinDesk\"");
                    escreverErro(response, mapper, 401, "Autenticação necessária ou credenciais inválidas.");
                })
                .accessDeniedHandler((request, response, exception) ->
                    escreverErro(response, mapper, 403, "Acesso não permitido.")));
        return http.build();
    }

    private void escreverErro(HttpServletResponse response, ObjectMapper mapper, int status, String mensagem)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        mapper.writeValue(response.getWriter(), new ErroApi(status, mensagem));
    }
}
