package com.nexsite.backend.dto;

import java.util.Map;

/** Totais para a página inicial: quantas empresas existem no total e em cada categoria. */
public record ResumoDTO(long total, Map<String, Long> porCategoria) {}
