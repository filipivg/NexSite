package com.nexsite.backend.util;

import com.nexsite.backend.exception.RegraNegocioException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

/**
 * Valida e padroniza os contatos da empresa (site e telefone).
 * Os dois são opcionais: vazio vira null. Valor preenchido porém inválido lança RegraNegocioException.
 */
public final class ContatoUtil {

    private static final int MAX_SITE = 200;

    // Qualquer "algo:" no começo é um esquema (javascript:, mailto:, data:, ftp:...)
    private static final Pattern TEM_ESQUEMA = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*:.*");
    private static final Pattern HTTP_OU_HTTPS = Pattern.compile("^https?://.*", Pattern.CASE_INSENSITIVE);

    private ContatoUtil() {}

    /**
     * Aceita "exemplo.com.br" ou "https://exemplo.com.br/loja" e devolve sempre com http(s)://.
     * Só http e https passam: esse valor vira um link na página, então javascript:, data: etc. são barrados.
     */
    public static String normalizarSite(String entrada) {
        if (entrada == null || entrada.isBlank()) return null;

        String site = entrada.trim();
        String erro = "Site inválido. Use um endereço como https://www.suaempresa.com.br.";

        if (site.length() > MAX_SITE) throw new RegraNegocioException(erro);

        if (!HTTP_OU_HTTPS.matcher(site).matches()) {
            if (TEM_ESQUEMA.matcher(site).matches()) {
                throw new RegraNegocioException(erro); // outro esquema (javascript:, ftp:...)
            }
            site = "https://" + site; // a pessoa digitou só o domínio
        }

        URI uri;
        try {
            uri = new URI(site);
        } catch (URISyntaxException e) {
            throw new RegraNegocioException(erro); // espaços, caracteres proibidos etc.
        }

        String host = uri.getHost();
        boolean valido = host != null
                && host.contains(".")            // "https://localhost" ou "https://x" não servem
                && !host.startsWith(".") && !host.endsWith(".")
                && uri.getUserInfo() == null;    // barra "https://banco.com@site-falso.com"
        if (!valido) throw new RegraNegocioException(erro);

        // esquema em minúsculas (HTTPS:// -> https://)
        return uri.getScheme().toLowerCase() + site.substring(uri.getScheme().length());
    }

    /**
     * Aceita "(62) 99999-9999", "62 3333-4444", "+55 62 99999-9999"...
     * Devolve só os dígitos com DDD (10 ou 11 dígitos): "62999999999".
     */
    public static String normalizarTelefone(String entrada) {
        if (entrada == null || entrada.isBlank()) return null;

        String erro = "Telefone inválido. Informe o DDD e o número, ex.: (62) 99999-9999.";
        String digitos = entrada.replaceAll("\\D", "");

        // código do país (+55) é opcional
        if ((digitos.length() == 12 || digitos.length() == 13) && digitos.startsWith("55")) {
            digitos = digitos.substring(2);
        }

        if (digitos.length() != 10 && digitos.length() != 11) throw new RegraNegocioException(erro);
        if (digitos.chars().distinct().count() == 1) throw new RegraNegocioException(erro); // 00000000000
        if (digitos.charAt(0) == '0' || digitos.charAt(1) == '0') throw new RegraNegocioException(erro); // DDD 11 a 99
        if (digitos.length() == 11 && digitos.charAt(2) != '9') throw new RegraNegocioException(erro);  // celular começa com 9
        if (digitos.length() == 10 && (digitos.charAt(2) == '0' || digitos.charAt(2) == '1')) throw new RegraNegocioException(erro);

        return digitos;
    }
}
