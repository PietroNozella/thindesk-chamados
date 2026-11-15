package com.pfc;

import com.pfc.security.AutenticacaoHandlers;
import com.pfc.security.ValidarCredencialFilter;
import com.pfc.thindesk.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private AutenticacaoHandlers autenticacaoHandlers;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, UsuarioRepository usuarios) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/esqueci-senha", "/redefinir-senha/**",
                        "/termos", "/privacidade", "/403",
                        "/css/**", "/js/**", "/images/**", "/error").permitAll()
                // Cadastro de usuários e auditoria: só Administrador (igual ao SIGEE).
                .requestMatchers("/usuarios/**", "/auditoria/**").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.GET, "/api/**").authenticated()
                .requestMatchers("/api/**").hasAnyRole("ADMINISTRADOR", "OPERADOR")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(autenticacaoHandlers.loginSucesso())
                .failureHandler(autenticacaoHandlers.loginFalha())
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessHandler(autenticacaoHandlers.logout())
            )
            .exceptionHandling(e -> e
                .accessDeniedHandler(autenticacaoHandlers.acessoNegado())
            )
            .addFilterAfter(new ValidarCredencialFilter(usuarios), AnonymousAuthenticationFilter.class);
        return http.build();
    }
}
