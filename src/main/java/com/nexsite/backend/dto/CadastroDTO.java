package com.nexsite.backend.dto;

import com.nexsite.backend.model.Categoria;
import com.nexsite.backend.model.Estado;
import com.nexsite.backend.model.TipoEmpresa;
import com.nexsite.backend.model.TipoPerfil;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class CadastroDTO {

    @NotBlank(message = "O nome é obrigatório")
    private String nome;

    @NotBlank(message = "O e-mail é obrigatório")
    @Email(message = "Forneça um e-mail válido")
    private String email;

    // Máximo de 72: é o limite do BCrypt
    @NotBlank(message = "A senha é obrigatória")
    @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres")
    private String senha;

    @NotNull(message = "O tipo de perfil deve ser informado")
    private TipoPerfil tipoPerfil;

    // Opcional aqui. A obrigatoriedade para EMPRESA é checada no CadastroService.
    // Aceita o CNPJ numérico e o alfanumérico (letras maiúsculas nas 12 primeiras posições; DV sempre numérico)
    @Pattern(regexp = "^([0-9A-Z]{2}\\.[0-9A-Z]{3}\\.[0-9A-Z]{3}/[0-9A-Z]{4}-[0-9]{2})?$", message = "Formato de CNPJ inválido")
    private String cnpj;

    // ---- Dados do anúncio da empresa ----
    // Opcionais aqui: a obrigatoriedade para EMPRESA é checada no CadastroService.
    // Para perfil de usuário comum esses campos são ignorados.
    private Categoria categoria;

    private TipoEmpresa tipoEmpresa;

    private Estado estado;

    @Size(max = 100, message = "O nome da cidade deve ter no máximo 100 caracteres")
    private String cidade;

    @DecimalMin(value = "0.00", message = "O preço mínimo não pode ser negativo")
    @Digits(integer = 8, fraction = 2, message = "Preço mínimo inválido")
    private BigDecimal precoMin;

    @DecimalMin(value = "0.00", message = "O preço máximo não pode ser negativo")
    @Digits(integer = 8, fraction = 2, message = "Preço máximo inválido")
    private BigDecimal precoMax;

    @Size(max = 300, message = "A descrição deve ter no máximo 300 caracteres")
    private String descricao;

    // Contatos opcionais (o formato é checado no CadastroService)
    @Size(max = 200, message = "O site deve ter no máximo 200 caracteres")
    private String site;

    @Size(max = 30, message = "Telefone inválido")
    private String telefone;

    // Getters e Setters
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    public TipoPerfil getTipoPerfil() { return tipoPerfil; }
    public void setTipoPerfil(TipoPerfil tipoPerfil) { this.tipoPerfil = tipoPerfil; }

    public String getCnpj() { return cnpj; }
    // Padroniza em maiúsculas já na entrada (CNPJ alfanumérico), antes da validação do @Pattern
    public void setCnpj(String cnpj) { this.cnpj = cnpj == null ? null : cnpj.trim().toUpperCase(); }
    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }

    public TipoEmpresa getTipoEmpresa() { return tipoEmpresa; }
    public void setTipoEmpresa(TipoEmpresa tipoEmpresa) { this.tipoEmpresa = tipoEmpresa; }

    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }

    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }

    public BigDecimal getPrecoMin() { return precoMin; }
    public void setPrecoMin(BigDecimal precoMin) { this.precoMin = precoMin; }

    public BigDecimal getPrecoMax() { return precoMax; }
    public void setPrecoMax(BigDecimal precoMax) { this.precoMax = precoMax; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getSite() { return site; }
    public void setSite(String site) { this.site = site; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
}
