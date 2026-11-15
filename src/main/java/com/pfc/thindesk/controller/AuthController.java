package com.pfc.thindesk.controller;

import com.pfc.thindesk.entity.Perfil;
import com.pfc.thindesk.entity.RegistroAuditoria;
import com.pfc.thindesk.form.UsuarioCadastroForm;
import com.pfc.thindesk.service.AuditoriaService;
import com.pfc.thindesk.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {
    @Autowired private UsuarioService usuarioService;
    @Autowired private AuditoriaService auditoriaService;

    @InitBinder("cadastro")
    public void configurarCadastro(WebDataBinder binder) {
        binder.setAllowedFields("nome", "sobrenome", "email", "username", "senha", "perfil");
    }

    @GetMapping("/login")
    public String login() { return "login"; }

    @GetMapping("/403")
    public String acessoNegado() { return "403"; }

    @GetMapping("/usuarios")
    public String usuarios(Model model) {
        model.addAttribute("usuarios", usuarioService.listar());
        return pagina("usuarios", model);
    }

    @GetMapping("/usuarios/novo")
    public String novoUsuarioForm(Model model) {
        model.addAttribute("cadastro", new UsuarioCadastroForm());
        model.addAttribute("perfis", Perfil.values());
        return pagina("usuarios-novo", model);
    }

    @PostMapping("/usuarios/novo")
    public String novoUsuario(@Valid @ModelAttribute("cadastro") UsuarioCadastroForm cadastro,
            BindingResult resultado, Model model, RedirectAttributes redirect, Principal principal,
            HttpServletRequest request) {
        model.addAttribute("perfis", Perfil.values());
        if (resultado.hasErrors()) return pagina("usuarios-novo", model);
        try {
            var usuario = usuarioService.cadastrar(cadastro.getNome(), cadastro.getSobrenome(), cadastro.getEmail(),
                    cadastro.getUsername(), cadastro.getSenha(), cadastro.getPerfil());
            auditoriaService.registrar(principal.getName(), RegistroAuditoria.Acao.USUARIO_CADASTRADO,
                    RegistroAuditoria.Resultado.SUCESSO, "Usuario", usuario.getId(), AuditoriaService.ip(request));
        } catch (IllegalArgumentException e) {
            resultado.reject("cadastro", e.getMessage());
            return pagina("usuarios-novo", model);
        }
        redirect.addFlashAttribute("ok", "Usuário cadastrado com sucesso.");
        return "redirect:/usuarios/novo";
    }

    @GetMapping("/usuarios/{id}/senha")
    public String redefinirForm(@PathVariable String id, Model model) {
        model.addAttribute("alvo", usuarioService.buscarPorId(id));
        return pagina("usuarios-senha", model);
    }

    @PostMapping("/usuarios/{id}/senha")
    public String redefinir(@PathVariable String id, @RequestParam String senhaAtual,
            @RequestParam String senhaNova, Principal principal, Model model,
            RedirectAttributes redirect, HttpServletRequest request) {
        model.addAttribute("alvo", usuarioService.buscarPorId(id));
        try {
            usuarioService.redefinirSenhaPorAdministrador(principal.getName(), id, senhaAtual, senhaNova);
        } catch (IllegalArgumentException e) {
            auditoriaService.registrar(principal.getName(), RegistroAuditoria.Acao.SENHA_REDEFINIDA,
                    RegistroAuditoria.Resultado.FALHA, "Usuario", id, AuditoriaService.ip(request));
            model.addAttribute("erro", e.getMessage());
            return pagina("usuarios-senha", model);
        }
        auditoriaService.registrar(principal.getName(), RegistroAuditoria.Acao.SENHA_REDEFINIDA,
                RegistroAuditoria.Resultado.SUCESSO, "Usuario", id, AuditoriaService.ip(request));
        redirect.addFlashAttribute("ok", "Senha redefinida. Informe a nova senha ao usuário por um canal privado.");
        return "redirect:/usuarios";
    }

    @GetMapping("/minha-senha")
    public String minhaSenhaForm(Model model) { return pagina("minha-senha", model); }

    @PostMapping("/minha-senha")
    public String minhaSenha(Principal principal, @RequestParam String senhaAtual,
            @RequestParam String senhaNova, Model model, HttpServletRequest request) {
        try {
            usuarioService.trocarSenha(principal.getName(), senhaAtual, senhaNova);
        } catch (IllegalArgumentException e) {
            model.addAttribute("erro", e.getMessage());
            return pagina("minha-senha", model);
        }
        auditoriaService.registrar(principal.getName(), RegistroAuditoria.Acao.SENHA_TROCADA,
                RegistroAuditoria.Resultado.SUCESSO, AuditoriaService.ip(request));
        var sessao = request.getSession(false);
        if (sessao != null) sessao.invalidate();
        SecurityContextHolder.clearContext();
        return "redirect:/login?redefinida";
    }

    @GetMapping("/esqueci-senha")
    public String esqueciForm() { return "esqueci-senha"; }

    @RequestMapping("/redefinir-senha/{token}")
    public String linkAntigo(@PathVariable String token) {
        throw new ResponseStatusException(HttpStatus.GONE, "A recuperação de acesso deve ser feita pelo administrador.");
    }

    private String pagina(String nome, Model model) {
        model.addAttribute("content", nome);
        return "layout";
    }
}