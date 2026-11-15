package com.pfc.thindesk.controller;

import com.pfc.thindesk.entity.Cliente;
import com.pfc.thindesk.service.ClienteService;
import jakarta.validation.Valid;
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
@RequestMapping("/clientes")
public class ClientePageController {
    private final ClienteService clientes;

    public ClientePageController(ClienteService clientes) { this.clientes = clientes; }

    @InitBinder("cliente")
    public void configurarFormulario(WebDataBinder binder) { binder.setAllowedFields("nome", "telefone", "setor"); }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("clientes", clientes.listarClientes());
        model.addAttribute("content", "clientes");
        return "layout";
    }

    @GetMapping("/novo")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public String novo(Model model) {
        model.addAttribute("cliente", new Cliente());
        return formulario(model);
    }

    @PostMapping("/novo")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public String criar(@Valid @ModelAttribute("cliente") Cliente cliente, BindingResult resultado,
            Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) return formulario(model);
        clientes.novoCliente(cliente);
        redirect.addFlashAttribute("ok", "Cliente cadastrado com sucesso.");
        return "redirect:/clientes";
    }

    @GetMapping("/editar/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public String editar(@PathVariable String id, Model model) {
        model.addAttribute("cliente", clientes.buscar(id));
        return formulario(model);
    }

    @PostMapping("/editar/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public String atualizar(@PathVariable String id, @Valid @ModelAttribute("cliente") Cliente cliente,
            BindingResult resultado, Model model, RedirectAttributes redirect) {
        clientes.buscar(id);
        cliente.setId(id);
        if (resultado.hasErrors()) return formulario(model);
        clientes.atualizar(id, cliente);
        redirect.addFlashAttribute("ok", "Cliente atualizado com sucesso.");
        return "redirect:/clientes";
    }

    @PostMapping("/deletar/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public String excluir(@PathVariable String id, RedirectAttributes redirect) {
        clientes.excluir(id);
        redirect.addFlashAttribute("ok", "Cliente excluído.");
        return "redirect:/clientes";
    }

    private String formulario(Model model) {
        model.addAttribute("content", "cliente-form");
        return "layout";
    }
}