package com.pfc.security;

import com.pfc.thindesk.entity.Usuario;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

public class UsuarioAutenticado extends User {

    private static final long serialVersionUID = 1L;
    private final long versaoCredencial;

    public UsuarioAutenticado(Usuario usuario) {
        super(usuario.getUsername(), usuario.getSenha(), usuario.isAtivo(), true, true,
                !usuario.bloqueado(), List.of(new SimpleGrantedAuthority(usuario.getPerfil().autoridade())));
        this.versaoCredencial = usuario.getVersaoCredencial();
    }

    public long getVersaoCredencial() {
        return versaoCredencial;
    }
}
