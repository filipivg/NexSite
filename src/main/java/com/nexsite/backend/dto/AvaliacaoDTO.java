package com.nexsite.backend.dto;

import com.nexsite.backend.model.Avaliacao;
import com.nexsite.backend.util.NomeUtil;

/** Uma avaliação como aparece para todos. O autor sai abreviado ("Maria S."); e-mail nunca é exposto. */
public record AvaliacaoDTO(
        Long id,
        String autor,
        int nota,
        String comentario,
        String criadoEm,
        boolean editada
) {
    public static AvaliacaoDTO de(Avaliacao a) {
        return new AvaliacaoDTO(
                a.getId(),
                NomeUtil.abreviar(a.getAutor().getNome()),
                a.getNota(),
                a.getComentario(),
                a.getCriadoEm().toString(),
                !a.getAtualizadoEm().equals(a.getCriadoEm())
        );
    }
}
