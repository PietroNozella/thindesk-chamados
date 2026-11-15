package com.pfc.thindesk.controller;

import com.pfc.thindesk.entity.HorarioAtendimento;
import com.pfc.thindesk.service.HorarioAtendimentoService;
import jakarta.validation.Valid;
import java.util.List;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/ajustes-horarios")
public class HorarioAtendimentoPageController {
    private final HorarioAtendimentoService horarios;

    public HorarioAtendimentoPageController(HorarioAtendimentoService horarios) { this.horarios = horarios; }

    @InitBinder("novoHorario")
    public void configurarFormulario(WebDataBinder binder) {
        binder.setAllowedFields("setor", "diaSemana", "horarioInicio", "horarioFim");
    }

    @ModelAttribute("diasSemana")
    public List<String> diasSemana() {
        return List.of("Segunda-feira", "Terça-feira", "Quarta-feira", "Quinta-feira", "Sexta-feira", "Sábado", "Domingo");
    }

    @GetMapping
    public String listar(Model model) {
        if (!model.containsAttribute("novoHorario")) model.addAttribute("novoHorario", new HorarioAtendimento());
        model.addAttribute("horarios", horarios.listarTodos());
        model.addAttribute("content", "ajustes-horarios");
        return "layout";
    }

    @PostMapping("/salvar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public String salvar(@Valid @ModelAttribute("novoHorario") HorarioAtendimento horario,
            BindingResult resultado, Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) return listar(model);
        horarios.salvar(horario);
        redirect.addFlashAttribute("ok", "Horário cadastrado com sucesso.");
        return "redirect:/ajustes-horarios";
    }

    @PostMapping("/deletar/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public String excluir(@PathVariable String id, RedirectAttributes redirect) {
        horarios.deletar(id);
        redirect.addFlashAttribute("ok", "Horário excluído.");
        return "redirect:/ajustes-horarios";
    }
}