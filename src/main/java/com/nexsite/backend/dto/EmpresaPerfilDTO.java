package com.nexsite.backend.dto;

import com.nexsite.backend.model.Empresa;

import java.math.BigDecimal;

/**
 * Tudo o que a página de perfil mostra. Não expõe e-mail, senha nem CNPJ:
 * o CNPJ é informado pela própria pessoa e ainda não é verificado, então não é exibido como se fosse um fato.
 */
public record EmpresaPerfilDTO(
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
        String site,
        String telefone,
        String criadoEm,
        Double notaMedia,
        long totalAvaliacoes
) {
    public static EmpresaPerfilDTO de(Empresa e, NotaResumo nota) {
        return new EmpresaPerfilDTO(
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
                e.getSite(),
                e.getTelefone(),
                e.getCriadoEm().toString(),
                nota.media(),
                nota.total()
        );
    }
}
