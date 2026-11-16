package com.nexsite.backend.service;

import com.nexsite.backend.dto.LoginDTO;
import com.nexsite.backend.exception.CredenciaisInvalidasException;
import com.nexsite.backend.model.Usuario;
import com.nexsite.backend.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService tentativas;

    // Hash de uma senha que ninguém conhece. Serve para gastar o mesmo tempo de BCrypt
    // quando o e-mail não existe (senão a resposta mais rápida entregaria quais e-mails têm conta).
    private final String hashFalso;

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       LoginAttemptService tentativas) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tentativas = tentativas;
        this.hashFalso = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public Usuario autenticar(LoginDTO dto, String ip) {
        String email = dto.getEmail().trim().toLowerCase();

        // 1. Bloqueado por excesso de erros? Nem confere a senha.
        tentativas.verificarBloqueio(ip, email);

        // 2. Confere e-mail e senha (sempre faz a comparação BCrypt, mesmo se o e-mail não existir)
        Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);
        String hash = usuario != null ? usuario.getSenha() : hashFalso;
        boolean senhaConfere = passwordEncoder.matches(dto.getSenha(), hash);

        if (usuario == null || !senhaConfere) {
            tentativas.registrarFalha(ip, email);
            throw new CredenciaisInvalidasException();
        }

        tentativas.registrarSucesso(ip, email);
        return usuario;
    }
}
