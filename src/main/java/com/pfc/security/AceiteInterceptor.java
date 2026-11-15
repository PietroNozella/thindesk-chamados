package com.pfc.security;

import com.pfc.thindesk.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

// Equivale ao middleware ExigirAceiteDocumentosLegais do SIGEE:
// usuário logado sem aceite vigente é redirecionado para /aceite.
@Component
public class AceiteInterceptor implements HandlerInterceptor {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Value("${app.legal.termos-versao:1.0}")
    private String termosVersao;

    @Value("${app.legal.privacidade-versao:1.0}")
    private String privacidadeVersao;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return true;
        }
        boolean emDia = usuarioRepository.findByUsername(auth.getName())
                .map(u -> u.aceiteEmDia(termosVersao, privacidadeVersao))
                .orElse(true);
        if (!emDia) {
            response.sendRedirect(request.getContextPath() + "/aceite");
            return false;
        }
        return true;
    }
}
