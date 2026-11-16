package com.nexsite.backend.dto;

import java.util.List;

/** Resumo (média e total) mais uma página de avaliações. A numeração de páginas começa em 0. */
public record AvaliacoesDTO(
        Double notaMedia,
        long total,
        List<AvaliacaoDTO> conteudo,
        int pagina,
        int tamanho,
        int totalPaginas
) {}
