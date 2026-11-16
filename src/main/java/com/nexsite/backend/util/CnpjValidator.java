package com.nexsite.backend.util;

/**
 * Valida os dígitos verificadores do CNPJ, nos dois formatos aceitos pela Receita Federal:
 *  - numérico (o formato antigo);
 *  - alfanumérico (novas inscrições desde jul/2026): as 12 primeiras posições podem ter
 *    letras maiúsculas A-Z e números, e os 2 últimos dígitos (DV) são sempre números.
 *
 * O cálculo é o módulo 11 de sempre, só que cada caractere vale (código ASCII - 48):
 * '0'..'9' continuam valendo 0..9 e 'A' passa a valer 17, 'B' 18 e assim por diante.
 */
public final class CnpjValidator {

    private static final int[] PESOS_DV1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_DV2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private CnpjValidator() {}

    public static boolean isValido(String cnpj) {
        if (cnpj == null) return false;

        // Tira a máscara (pontos, barra e traço) e padroniza em maiúsculas
        String c = cnpj.replaceAll("[./-]", "").toUpperCase();

        // 12 caracteres alfanuméricos + 2 dígitos verificadores numéricos
        if (!c.matches("[0-9A-Z]{12}[0-9]{2}")) return false;

        // Sequência com todos os caracteres iguais (00000000000000 etc.) passa na conta, mas não existe
        if (c.chars().distinct().count() == 1) return false;

        int dv1 = calcularDigito(c, PESOS_DV1);
        int dv2 = calcularDigito(c, PESOS_DV2);

        return dv1 == c.charAt(12) - '0' && dv2 == c.charAt(13) - '0';
    }

    private static int calcularDigito(String cnpj, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += (cnpj.charAt(i) - '0') * pesos[i]; // '0' == 48, então isso é ASCII - 48
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
