package com.nexsite.backend.dto;

import com.nexsite.backend.model.Categoria;
import com.nexsite.backend.model.Estado;
import com.nexsite.backend.model.TipoEmpresa;

import java.math.BigDecimal;
import java.util.List;

/** Filtros da busca de empresas. Qualquer campo nulo/vazio significa "sem esse filtro". */
public record FiltroEmpresa(
        String q,
        List<Categoria> categorias,
        List<TipoEmpresa> tipos,
        Estado estado,
        String cidade,
        BigDecimal precoMin,
        BigDecimal precoMax,
        BigDecimal notaMin // média mínima de avaliações (1 a 5, no máximo 1 casa decimal); null = sem filtro
) {}
