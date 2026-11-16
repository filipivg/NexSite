package com.nexsite.backend.dto;

import com.nexsite.backend.model.Empresa;

import java.math.BigDecimal;

/** O que a API devolve de cada empresa nas listagens. Não expõe e-mail, CNPJ nem dados do usuário. */
public record EmpresaResumoDTO(
        Long id,
        String nome,
        String categoria,
        String categoriaRotulo,
        String tipo,
        String tipoRotulo,
        String cidade,
        String estado,
        BigDecimal precoMin,
        BigDecimal precoMax,
        String descricao,
        String criadoEm, // data ISO-8601 em texto (ex.: 2026-11-11T12:00:00Z), igual em qualquer configuração
        Double notaMedia, // média das avaliações (1 casa decimal); null se ninguém avaliou ainda
        long totalAvaliacoes
) {
    public static EmpresaResumoDTO de(Empresa e, NotaResumo nota) {
        return new EmpresaResumoDTO(
                e.getId(),
                e.getResponsavel().getNome(),
                e.getCategoria().name(),
                e.getCategoria().getRotulo(),
                e.getTipo().name(),
                e.getTipo().getRotulo(),
                e.getCidade(),
                e.getEstado().name(),
                e.getPrecoMin(),
                e.getPrecoMax(),
                e.getDescricao(),
                e.getCriadoEm().toString(),
                nota.media(),
                nota.total()
        );
    }
}
