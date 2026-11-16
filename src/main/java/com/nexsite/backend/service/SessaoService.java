package com.nexsite.backend.service;

import com.nexsite.backend.exception.NaoAutenticadoException;
import com.nexsite.backend.model.Sessao;
import com.nexsite.backend.model.Usuario;
import com.nexsite.backend.repository.SessaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Sessões de login com token opaco: no login nasce um código aleatório de 256 bits,
 * o servidor guarda só o hash dele, e o front manda o código em "Authorization: Bearer ...".
 * Sair (logout) apaga a sessão de verdade, coisa que um JWT sozinho não permite.
 */
@Service
public class SessaoService {

    static final Duration VALIDADE = Duration.ofDays(30);
    private static final int MAX_TAMANHO_TOKEN = 200; // descarta lixo gigante sem nem calcular hash

    private final SessaoRepository sessaoRepository;
    private final Clock clock;
    private final SecureRandom aleatorio = new SecureRandom();

    public SessaoService(SessaoRepository sessaoRepository, Clock clock) {
        this.sessaoRepository = sessaoRepository;
        this.clock = clock;
    }

    /** Cria a sessão e devolve o token (esta é a única vez em que ele existe por inteiro no servidor). */
    @Transactional
    public String criar(Usuario usuario) {
        Instant agora = clock.instant();
        sessaoRepository.apagarExpiradas(agora); // faxina aproveitando o login

        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        sessaoRepository.save(new Sessao(hash(token), usuario, agora, agora.plus(VALIDADE)));
        return token;
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> usuarioDoToken(String token) {
        if (token == null || token.isBlank() || token.length() > MAX_TAMANHO_TOKEN) return Optional.empty();
        Instant agora = clock.instant();
        return sessaoRepository.buscarPorHash(hash(token))
                .filter(s -> agora.isBefore(s.getExpiraEm()))
                .map(Sessao::getUsuario);
    }

    @Transactional
    public void encerrar(String token) {
        if (token == null || token.isBlank() || token.length() > MAX_TAMANHO_TOKEN) return;
        sessaoRepository.apagarPorHash(hash(token));
    }

    /** Lê o cabeçalho "Authorization: Bearer <token>" e devolve o usuário, ou lança 401. */
    public Usuario exigirUsuario(String authorization) {
        return usuarioDoToken(extrairToken(authorization)).orElseThrow(NaoAutenticadoException::new);
    }

    /** Devolve o token do cabeçalho, ou null se o cabeçalho não for "Bearer ...". */
    public static String extrairToken(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) return null;
        String token = authorization.substring(7).trim();
        return token.isEmpty() ? null : token;
    }

    static String hash(String token) {
        try {
            byte[] resumo = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumo);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
