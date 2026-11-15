package com.pfc.security;

import com.pfc.thindesk.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class ValidarCredencialFilter extends OncePerRequestFilter {

    private final UsuarioRepository usuarios;

    public ValidarCredencialFilter(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            boolean vigente = auth.getPrincipal() instanceof UsuarioAutenticado principal
                    && usuarios.findByUsername(principal.getUsername())
                            .map(usuario -> usuario.isAtivo()
                                    && usuario.getVersaoCredencial() == principal.getVersaoCredencial())
                            .orElse(false);
            if (!vigente) {
                var sessao = request.getSession(false);
                if (sessao != null) sessao.invalidate();
                SecurityContextHolder.clearContext();
                response.sendRedirect(request.getContextPath() + "/login?expirada");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
