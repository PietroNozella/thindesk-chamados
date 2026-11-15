package com.pfc.thindesk.controller;

import com.pfc.thindesk.entity.HorarioAtendimento;
import com.pfc.thindesk.service.HorarioAtendimentoService;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ajustes-horarios")
public class HorarioAtendimentoController {

    @Autowired
    private HorarioAtendimentoService horarioAtendimentoService;

    @GetMapping
    public List<HorarioAtendimento> listarHorarios() {
        return horarioAtendimentoService.listarTodos();
    }

    @PostMapping
    public HorarioAtendimento salvarHorario(@Valid @RequestBody HorarioAtendimento horarioAtendimento) {
        return horarioAtendimentoService.salvar(horarioAtendimento);
    }

    @DeleteMapping("/{id}")
    public void deletarHorario(@PathVariable String id) {
        horarioAtendimentoService.deletar(id);
    }
}
