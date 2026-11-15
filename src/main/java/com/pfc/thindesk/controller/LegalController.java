package com.pfc.thindesk.controller;

import com.pfc.thindesk.entity.RegistroAuditoria;
import com.pfc.thindesk.service.AuditoriaService;
import com.pfc.thindesk.service.UsuarioService;
import java.security.Principal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import jakarta.servlet.http.HttpServletRequest;

// Documentos legais públicos + aceite versionado obrigatório após o login.
@Controller
public class LegalController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private AuditoriaService auditoriaService;

    @Value("${app.legal.termos-versao:1.0}")
    private String termosVersao;

    @Value("${app.legal.privacidade-versao:1.0}")
    private String privacidadeVersao;

    @GetMapping("/termos")
    public String termos(Model model) {
        model.addAttribute("versao", termosVersao);
        return "termos";
    }

    @GetMapping("/privacidade")
    public String privacidade(Model model) {
        model.addAttribute("versao", privacidadeVersao);
        return "privacidade";
    }

    @GetMapping("/aceite")
    public String aceiteForm(Model model) {
        model.addAttribute("termosVersao", termosVersao);
        model.addAttribute("privacidadeVersao", privacidadeVersao);
        return "aceite";
    }

    @PostMapping("/aceite")
    public String aceite(Principal principal, HttpServletRequest request) {
        usuarioService.registrarAceite(principal.getName(), termosVersao, privacidadeVersao);
        auditoriaService.registrar(principal.getName(), RegistroAuditoria.Acao.ACEITE_REGISTRADO,
                RegistroAuditoria.Resultado.SUCESSO, AuditoriaService.ip(request));
        return "redirect:/";
    }
}
