package com.nexsite.backend.controller;

import com.nexsite.backend.dto.CadastroDTO;
import com.nexsite.backend.dto.LoginDTO;
import com.nexsite.backend.model.Usuario;
import com.nexsite.backend.service.AuthService;
import com.nexsite.backend.service.CadastroService;
import com.nexsite.backend.service.SessaoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*") // Permite que o seu arquivo HTML local faça requisições para a API
public class AuthController {

    private final CadastroService cadastroService;
    private final AuthService authService;
    private final SessaoService sessaoService;

    public AuthController(CadastroService cadastroService, AuthService authService, SessaoService sessaoService) {
        this.cadastroService = cadastroService;
        this.authService = authService;
        this.sessaoService = sessaoService;
    }

    // Sem try/catch: os erros (validação, e-mail duplicado, CNPJ inválido, login errado)
    // são tratados em um lugar só, no GlobalExceptionHandler.

    @PostMapping("/cadastro")
    public ResponseEntity<?> cadastrar(@Valid @RequestBody CadastroDTO dto) {
        cadastroService.registrarConta(dto);
        return ResponseEntity.ok(Map.of("mensagem", "Conta criada com sucesso!"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest request) {
        // O IP de quem está tentando entra no controle de tentativas (limite de erros por IP)
        Usuario usuario = authService.autenticar(dto, request.getRemoteAddr());
        // Cria a sessão: o token vai para o front e é enviado nos pedidos que exigem login
        String token = sessaoService.criar(usuario);
        // Devolve só dados públicos e o token. Nunca a senha nem o hash.
        return ResponseEntity.ok(Map.of(
                "id", usuario.getId(),
                "nome", usuario.getNome(),
                "email", usuario.getEmail(),
                "tipoPerfil", usuario.getTipoPerfil().name(),
                "token", token
        ));
    }

    /** Encerra a sessão. Sempre responde 204, mesmo com token inválido (sair nunca deve dar erro). */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        sessaoService.encerrar(SessaoService.extrairToken(authorization));
        return ResponseEntity.noContent().build();
    }
}
