package com.nexsite.backend.dto;

import java.util.List;

/** Uma página de resultados. A numeração de páginas começa em 0. */
public record PaginaDTO<T>(
        List<T> conteudo,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas
) {}
