package com.nexsite.backend.util;

public final class NomeUtil {

    private NomeUtil() {}

    /** "Maria da Silva" -> "Maria S."; "Maria" -> "Maria". Evita expor o nome completo de quem avalia. */
    public static String abreviar(String nome) {
        if (nome == null || nome.isBlank()) return "Usuário";
        String[] partes = nome.trim().split("\\s+");
        if (partes.length == 1) return partes[0];
        String ultimo = partes[partes.length - 1];
        return partes[0] + " " + ultimo.substring(0, ultimo.offsetByCodePoints(0, 1)).toUpperCase() + ".";
    }
}
