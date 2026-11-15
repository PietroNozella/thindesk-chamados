package com.pfc.thindesk.entity;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "horariosAtendimento")
public class HorarioAtendimento {

    @Id
    private String id;
    @NotBlank(message = "Informe o setor.")
    @Size(max = 100, message = "O setor deve ter até 100 caracteres.")
    private String setor;
    @NotBlank(message = "Selecione o dia da semana.")
    @Pattern(regexp = "Segunda-feira|Terça-feira|Quarta-feira|Quinta-feira|Sexta-feira|Sábado|Domingo", message = "Selecione um dia válido.")
    private String diaSemana; // Exemplo: "Segunda-feira"
    @NotBlank(message = "Informe o horário de início.")
    @Pattern(regexp = "([01][0-9]|2[0-3]):[0-5][0-9]", message = "Informe um horário de início válido.")
    private String horarioInicio; // Exemplo: "08:00"
    @NotBlank(message = "Informe o horário de fim.")
    @Pattern(regexp = "([01][0-9]|2[0-3]):[0-5][0-9]", message = "Informe um horário de fim válido.")
    private String horarioFim; // Exemplo: "18:00"

    @AssertTrue(message = "O horário de fim deve ser posterior ao início no mesmo dia.")
    public boolean isIntervaloValido() {
        return horarioInicio == null || horarioFim == null || horarioFim.compareTo(horarioInicio) > 0;
    }
    public HorarioAtendimento() {}

    public HorarioAtendimento(String id, String setor, String diaSemana, String horarioInicio, String horarioFim) {
        this.id = id;
        this.setor = setor;
        this.diaSemana = diaSemana;
        this.horarioInicio = horarioInicio;
        this.horarioFim = horarioFim;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSetor() { return setor; }
    public void setSetor(String setor) { this.setor = setor; }

    public String getDiaSemana() { return diaSemana; }
    public void setDiaSemana(String diaSemana) { this.diaSemana = diaSemana; }

    public String getHorarioInicio() { return horarioInicio; }
    public void setHorarioInicio(String horarioInicio) { this.horarioInicio = horarioInicio; }

    public String getHorarioFim() { return horarioFim; }
    public void setHorarioFim(String horarioFim) { this.horarioFim = horarioFim; }
}