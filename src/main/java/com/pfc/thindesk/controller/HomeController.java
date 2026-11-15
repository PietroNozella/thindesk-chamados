package com.pfc.thindesk.controller;

import com.pfc.thindesk.entity.Chamado;
import com.pfc.thindesk.service.ChamadoService;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class HomeController {
    @Autowired private ChamadoService chamadoService;

    @GetMapping("/")
    public ModelAndView home() {
        ModelAndView model = new ModelAndView("layout");
        List<Chamado> chamados = chamadoService.listarChamados();
        model.addObject("totalChamados", chamados.size());
        model.addObject("resumoStatus", chamados.stream().collect(
                Collectors.groupingBy(Chamado::getStatus, Collectors.counting())));
        model.addObject("chamados", chamados.stream().limit(5).toList());
        model.addObject("content", "home");
        return model;
    }
}