package com.pfc.thindesk.controller;

import com.pfc.thindesk.entity.Chamado;
import com.pfc.thindesk.service.ChamadoService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ChamadoPageController {

    private final ChamadoService chamadoService;

    public ChamadoPageController(ChamadoService chamadoService) {
        this.chamadoService = chamadoService;
    }

    @InitBinder("chamado")
    public void configurarFormulario(WebDataBinder binder) {
        binder.setAllowedFields("descricao", "status", "tipo", "tecnico", "usuario", "revisao");
    }

    @ModelAttribute("statusDisponiveis")
    public Map<String, String> statusDisponiveis() {
        Map<String, String> status = new LinkedHashMap<>();
        status.put("ABERTO", "Aberto");
        status.put("EM_ANDAMENTO", "Em andamento");
        status.put("CONCLUIDO", "Concluído");
        status.put("CANCELADO", "Cancelado");
        return status;
    }

    @GetMapping("/chamados")
    public String listar(Model model) {
        model.addAttribute("chamados", chamadoService.listarChamados());
        model.addAttribute("content", "chamados");
        return "layout";
    }

    @GetMapping("/chamados/novo")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public String novo(Model model) {
        Chamado chamado = new Chamado();
        chamado.setStatus("ABERTO");
        model.addAttribute("chamado", chamado);
        return formulario(model);
    }

    @PostMapping("/chamados/novo")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public String criar(@Valid @ModelAttribute("chamado") Chamado chamado, BindingResult resultado,
            Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return formulario(model);
        }
        chamadoService.criarChamado(chamado);
        redirect.addFlashAttribute("ok", "Chamado cadastrado com sucesso.");
        return "redirect:/chamados";
    }

    @GetMapping("/chamados/atualizar/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public String editar(@PathVariable String id, Model model) {
        model.addAttribute("chamado", chamadoService.buscarChamado(id));
        return formulario(model);
    }

    @PostMapping("/chamados/atualizar/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public String atualizar(@PathVariable String id, @Valid @ModelAttribute("chamado") Chamado chamado,
            BindingResult resultado, Model model, RedirectAttributes redirect, HttpServletResponse response) {
        chamadoService.buscarChamado(id);
        chamado.setId(id);
        if (resultado.hasErrors()) {
            return formulario(model);
        }
        try {
            chamadoService.atualizarChamado(id, chamado);
        } catch (ResponseStatusException e) {
            if (e.getStatusCode() != HttpStatus.CONFLICT) throw e;
            response.setStatus(HttpStatus.CONFLICT.value());
            model.addAttribute("erro", e.getReason());
            model.addAttribute("conflito", true);
            return formulario(model);
        }
        redirect.addFlashAttribute("ok", "Chamado atualizado com sucesso.");
        return "redirect:/chamados";
    }

    @PostMapping("/chamados/cancelar/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public String cancelar(@PathVariable String id, @RequestParam Long revisao,
            RedirectAttributes redirect, Model model, HttpServletResponse response) {
        try {
            chamadoService.cancelarChamado(id, revisao);
        } catch (ResponseStatusException e) {
            if (e.getStatusCode() != HttpStatus.CONFLICT) throw e;
            response.setStatus(HttpStatus.CONFLICT.value());
            model.addAttribute("erro", e.getReason());
            return listar(model);
        }
        redirect.addFlashAttribute("ok", "Chamado cancelado. O registro foi mantido para consulta.");
        return "redirect:/chamados";
    }

    private String formulario(Model model) {
        model.addAttribute("content", "chamados/atualizar");
        return "layout";
    }
}
