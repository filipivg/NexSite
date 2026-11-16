package com.nexsite.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** O que o usuário envia ao avaliar: nota obrigatória e análise opcional. */
public class AvaliacaoRequestDTO {

    @NotNull(message = "Escolha uma nota de 1 a 5")
    @Min(value = 1, message = "A nota deve ser de 1 a 5")
    @Max(value = 5, message = "A nota deve ser de 1 a 5")
    private Integer nota;

    @Size(max = 1000, message = "A análise deve ter no máximo 1000 caracteres")
    private String comentario;

    public Integer getNota() { return nota; }
    public void setNota(Integer nota) { this.nota = nota; }

    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }
}
