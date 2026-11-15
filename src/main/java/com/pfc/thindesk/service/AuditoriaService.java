package com.pfc.thindesk.service;

import com.pfc.thindesk.entity.RegistroAuditoria;
import com.pfc.thindesk.repository.RegistroAuditoriaRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuditoriaService {

    @Autowired
    private RegistroAuditoriaRepository repository;

    public void registrar(String usuario, RegistroAuditoria.Acao acao,
                          RegistroAuditoria.Resultado resultado, String ip) {
        registrar(usuario, acao, resultado, null, null, ip);
    }

    public void registrar(String usuario, RegistroAuditoria.Acao acao,
                          RegistroAuditoria.Resultado resultado,
                          String entidade, String entidadeId, String ip) {
        RegistroAuditoria registro = new RegistroAuditoria();
        registro.setQuando(Instant.now());
        registro.setUsuario(usuario);
        registro.setAcao(acao);
        registro.setResultado(resultado);
        registro.setEntidade(entidade);
        registro.setEntidadeId(entidadeId);
        registro.setIp(ip);
        repository.save(registro);
    }

    public List<RegistroAuditoria> listar() {
        return repository.findAllByOrderByQuandoDesc();
    }

    public static String ip(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
