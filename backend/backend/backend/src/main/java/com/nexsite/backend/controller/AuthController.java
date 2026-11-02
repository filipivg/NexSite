package com.nexsite.backend.controller;

import com.nexsite.backend.dto.CadastroDTO;
import com.nexsite.backend.service.CadastroService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*") // Permite que o seu arquivo HTML local faça requisições para a API
public class AuthController {

    private final CadastroService cadastroService;

    public AuthController(CadastroService cadastroService) {
        this.cadastroService = cadastroService;
    }

    @PostMapping("/cadastro")
    public ResponseEntity<?> cadastrar(@Valid @RequestBody CadastroDTO dto) {
        try {
            // Tenta registrar chamando as regras de negócio
            cadastroService.registrarConta(dto);
            return ResponseEntity.ok().body("{\"mensagem\": \"Conta criada com sucesso!\"}");
        } catch (IllegalArgumentException e) {
            // Captura os erros de negócio (e-mail duplicado, CNPJ repetido)
            return ResponseEntity.badRequest().body("{\"erro\": \"" + e.getMessage() + "\"}");
        }
    }
}