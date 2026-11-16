package com.nexsite.backend.util;

/**
 * Pontuação usada em "Mais bem avaliadas": média ponderada (bayesiana).
 *
 *   pontuação = (soma das notas + PESO_PRIOR x média geral do site) / (nº de avaliações + PESO_PRIOR)
 *
 * Na prática, toda empresa começa com PESO_PRIOR avaliações "imaginárias" iguais à média geral do site,
 * e as avaliações reais vão empurrando a pontuação para a nota verdadeira dela.
 *  - Poucas avaliações: a pontuação fica perto da média geral (uma nota 5 sozinha não garante o topo).
 *  - Muitas avaliações: a pontuação converge para a média real da empresa.
 *
 * Exemplo (média geral 4,0 e PESO_PRIOR 5): duas empresas com média 5,0,
 * uma com 10 avaliações e outra com 2 ->  4,67 contra 4,29: a de 10 fica na frente.
 *
 * ATENÇÃO: a mesma conta é feita em SQL por EmpresaSpecs.ordenarPorNota. Se mudar aqui, mude lá.
 */
public final class RankingNotas {

    /** Quanto o "palpite inicial" pesa, medido em avaliações. Maior = exige mais avaliações para sair da média geral. */
    public static final int PESO_PRIOR = 5;

    private RankingNotas() {}

    public static double pontuacao(long somaNotas, long totalAvaliacoes, double mediaGeral) {
        return (somaNotas + PESO_PRIOR * mediaGeral) / (totalAvaliacoes + PESO_PRIOR);
    }
}
