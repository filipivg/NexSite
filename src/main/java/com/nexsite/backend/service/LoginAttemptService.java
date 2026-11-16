package com.nexsite.backend.service;

import com.nexsite.backend.exception.MuitasTentativasException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita as tentativas de login erradas. Dois limites, os dois por janela de 15 minutos:
 *  - 5 erros para o mesmo e-mail vindo do mesmo IP  -> segura quem tenta adivinhar a senha de uma conta;
 *  - 20 erros vindos do mesmo IP (e-mails variados) -> segura quem testa uma lista de e-mails.
 *
 * Por que a chave inclui o IP e não só o e-mail? Se bloqueasse só pelo e-mail, qualquer pessoa
 * poderia travar a conta de outra apenas errando a senha de propósito.
 *
 * Limitações (aceitáveis em desenvolvimento, a revisar antes de publicar):
 *  - os contadores ficam na memória: zeram quando o servidor reinicia e não são
 *    compartilhados entre várias instâncias (nesse caso, usar Redis ou o banco);
 *  - atrás de proxy/balanceador (Nginx, Cloudflare...) o IP visto é o do proxy, e todos os usuários
 *    dividiriam o mesmo limite de 20 erros. Nesse caso, ligar server.forward-headers-strategy=native
 *    no application.properties, mas só se o proxy for de confiança (senão o IP pode ser forjado).
 */
@Service
public class LoginAttemptService {

    static final int MAX_FALHAS_POR_EMAIL = 5;
    static final int MAX_FALHAS_POR_IP = 20;
    static final Duration JANELA = Duration.ofMinutes(15);

    // Se o mapa passar disso, apaga os registros vencidos (evita crescer sem limite)
    private static final int LIMITE_PARA_LIMPEZA = 10_000;

    private record Registro(int falhas, Instant inicio, Instant bloqueadoAte) {}

    private final ConcurrentHashMap<String, Registro> registros = new ConcurrentHashMap<>();
    private final Clock clock;

    public LoginAttemptService(Clock clock) {
        this.clock = clock;
    }

    /** Chamar ANTES de conferir a senha. Lança MuitasTentativasException se estiver bloqueado. */
    public void verificarBloqueio(String ip, String email) {
        Instant agora = clock.instant();
        long espera = Math.max(
                segundosRestantes(registros.get(chaveEmail(ip, email)), agora),
                segundosRestantes(registros.get(chaveIp(ip)), agora));
        if (espera > 0) {
            throw new MuitasTentativasException(espera);
        }
    }

    /** Login errado (inclusive e-mail que não existe: assim não dá para descobrir quais e-mails existem). */
    public void registrarFalha(String ip, String email) {
        Instant agora = clock.instant();
        somarFalha(chaveEmail(ip, email), MAX_FALHAS_POR_EMAIL, agora);
        somarFalha(chaveIp(ip), MAX_FALHAS_POR_IP, agora);

        if (registros.size() > LIMITE_PARA_LIMPEZA) {
            registros.entrySet().removeIf(e -> expirou(e.getValue(), agora));
        }
    }

    /**
     * Login certo: zera só o contador do par IP+e-mail.
     * O contador do IP NÃO zera, senão um atacante poderia "limpar" o limite logando na própria conta.
     */
    public void registrarSucesso(String ip, String email) {
        registros.remove(chaveEmail(ip, email));
    }

    // ---------- internos ----------

    private void somarFalha(String chave, int maximo, Instant agora) {
        registros.compute(chave, (k, atual) -> {
            if (atual == null || expirou(atual, agora)) {
                atual = new Registro(0, agora, null); // começa uma janela nova
            }
            int falhas = atual.falhas() + 1;
            Instant bloqueadoAte = falhas >= maximo ? agora.plus(JANELA) : atual.bloqueadoAte();
            return new Registro(falhas, atual.inicio(), bloqueadoAte);
        });
    }

    private static boolean expirou(Registro r, Instant agora) {
        Instant fim = r.bloqueadoAte() != null ? r.bloqueadoAte() : r.inicio().plus(JANELA);
        return !agora.isBefore(fim);
    }

    private static long segundosRestantes(Registro r, Instant agora) {
        if (r == null || r.bloqueadoAte() == null || !agora.isBefore(r.bloqueadoAte())) {
            return 0;
        }
        long ms = Duration.between(agora, r.bloqueadoAte()).toMillis();
        return (ms + 999) / 1000; // arredonda para cima
    }

    private static String chaveEmail(String ip, String email) {
        return "ip-email:" + ip + "|" + email;
    }

    private static String chaveIp(String ip) {
        return "ip:" + ip;
    }
}
