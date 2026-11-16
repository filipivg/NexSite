package com.nexsite.backend.dto;

/** Média (uma casa decimal) e quantidade de avaliações. Sem avaliações: média nula e total 0. */
public record NotaResumo(Double media, long total) {

    public static final NotaResumo VAZIO = new NotaResumo(null, 0);

    public static NotaResumo de(Double mediaBruta, long total) {
        if (total == 0 || mediaBruta == null) return VAZIO;
        return new NotaResumo(Math.round(mediaBruta * 10) / 10.0, total);
    }
}
