package com.nexsite.backend.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "empresas")
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String cnpj;

    // Relacionamento 1 para 1 com a tabela usuarios (Chave Estrangeira).
    // O nome da empresa é o nome do usuário responsável (Usuario.nome).
    @OneToOne
    @JoinColumn(name = "usuario_id", referencedColumnName = "id")
    private Usuario responsavel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Categoria categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoEmpresa tipo;

    @Column(nullable = false, length = 100)
    private String cidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 2)
    private Estado estado;

    // Faixa de preço dos produtos/serviços (em reais)
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precoMin;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precoMax;

    @Column(length = 300)
    private String descricao;

    // Contatos opcionais. O site já fica com http(s):// e o telefone só com dígitos (DDD + número).
    @Column(length = 200)
    private String site;

    @Column(length = 11)
    private String telefone;

    @Column(nullable = false, updatable = false)
    private Instant criadoEm = Instant.now();

    public Empresa() {}

    public Empresa(String cnpj, Usuario responsavel) {
        this.cnpj = cnpj;
        this.responsavel = responsavel;
    }

    // Getters e Setters
    public Long getId() { return id; }

    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }

    public Usuario getResponsavel() { return responsavel; }
    public void setResponsavel(Usuario responsavel) { this.responsavel = responsavel; }

    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }

    public TipoEmpresa getTipo() { return tipo; }
    public void setTipo(TipoEmpresa tipo) { this.tipo = tipo; }

    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }

    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }

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

    public Instant getCriadoEm() { return criadoEm; }
}
