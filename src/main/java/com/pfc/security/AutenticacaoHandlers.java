package com.pfc.security;

import com.pfc.thindesk.entity.RegistroAuditoria;
import com.pfc.thindesk.service.AuditoriaService;
import com.pfc.thindesk.service.UsuarioService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

// Centraliza os efeitos colaterais da autenticação:
// bloqueio progressivo após tentativas inválidas e trilha de auditoria.
@Component
public class AutenticacaoHandlers {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private AuditoriaService auditoriaService;

    public AuthenticationSuccessHandler loginSucesso() {
        return (request, response, authentication) -> {
            String username = authentication.getName();
            usuarioService.registrarSucesso(username);
            auditoriaService.registrar(username, RegistroAuditoria.Acao.LOGIN,
                    RegistroAuditoria.Resultado.SUCESSO, AuditoriaService.ip(request));
            response.sendRedirect(request.getContextPath() + "/");
        };
    }

    public AuthenticationFailureHandler loginFalha() {
        return new AuthenticationFailureHandler() {
            @Override
            public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                                AuthenticationException exception)
                    throws IOException, ServletException {
                String username = request.getParameter("username");
                String destino = "/login?error";
                if (exception instanceof LockedException) {
                    auditoriaService.registrar(username, RegistroAuditoria.Acao.LOGIN,
                            RegistroAuditoria.Resultado.FALHA, AuditoriaService.ip(request));
                    destino = "/login?bloqueado";
                } else {
                    boolean bloqueouAgora = usuarioService.registrarFalha(username);
                    if (bloqueouAgora) {
                        auditoriaService.registrar(username, RegistroAuditoria.Acao.CONTA_BLOQUEADA,
                                RegistroAuditoria.Resultado.SUCESSO, AuditoriaService.ip(request));
                        destino = "/login?bloqueado";
                    } else {
                        auditoriaService.registrar(username, RegistroAuditoria.Acao.LOGIN,
                                RegistroAuditoria.Resultado.FALHA, AuditoriaService.ip(request));
                    }
                }
                response.sendRedirect(request.getContextPath() + destino);
            }
        };
    }

    public LogoutSuccessHandler logout() {
        return (request, response, authentication) -> {
            if (authentication != null) {
                auditoriaService.registrar(authentication.getName(), RegistroAuditoria.Acao.LOGOUT,
                        RegistroAuditoria.Resultado.SUCESSO, AuditoriaService.ip(request));
            }
            response.sendRedirect(request.getContextPath() + "/login?logout");
        };
    }

    public AccessDeniedHandler acessoNegado() {
        return (request, response, denied) -> {
            String username = null;
            if (request.getUserPrincipal() != null) {
                username = request.getUserPrincipal().getName();
            }
            auditoriaService.registrar(username, RegistroAuditoria.Acao.ACESSO_NEGADO,
                    RegistroAuditoria.Resultado.FALHA, AuditoriaService.ip(request));
            response.sendRedirect(request.getContextPath() + "/403");
        };
    }
}
