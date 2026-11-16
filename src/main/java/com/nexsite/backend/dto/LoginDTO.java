package com.nexsite.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginDTO {

    // Limites de tamanho: o e-mail vira chave do controle de tentativas (memória) e a senha
    // não pode passar de 72 (limite do BCrypt, o mesmo do cadastro)
    @NotBlank(message = "O e-mail é obrigatório")
    @Size(max = 254, message = "E-mail muito longo")
    private String email;

    @NotBlank(message = "A senha é obrigatória")
    @Size(max = 72, message = "A senha deve ter no máximo 72 caracteres")
    private String senha;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
}
