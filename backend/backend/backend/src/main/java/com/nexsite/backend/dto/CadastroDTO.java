package com.nexsite.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public class CadastroDTO {
    
    @NotBlank(message = "O nome é obrigatório")
    private String nome;

    @NotBlank(message = "O e-mail é obrigatório")
    @Email(message = "Forneça um e-mail válido")
    private String email;

    @NotBlank(message = "A senha é obrigatória")
    @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres")
    private String senha;

    @NotBlank(message = "O tipo de perfil deve ser informado")
    private String tipoPerfil;

    @Pattern(regexp = "^(\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2})?$", message = "Formato de CNPJ inválido")
    private String cnpj;

    // Getters e Setters
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    public String getTipoPerfil() { return tipoPerfil; }
    public void setTipoPerfil(String tipoPerfil) { this.tipoPerfil = tipoPerfil; }

    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }
}