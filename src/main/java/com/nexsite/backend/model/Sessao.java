package com.nexsite.backend.model;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Uma sessão de login. O token em si NUNCA é salvo: só o hash SHA-256 dele.
 * Assim, mesmo que alguém leia o banco, não consegue usar as sessões.
 */
@Entity
@Table(name = "sessoes")
public class Sessao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false, updatable = false)
    private Instant criadaEm;

    @Column(nullable = false)
    private Instant expiraEm;

    public Sessao() {}

    public Sessao(String tokenHash, Usuario usuario, Instant criadaEm, Instant expiraEm) {
        this.tokenHash = tokenHash;
        this.usuario = usuario;
        this.criadaEm = criadaEm;
        this.expiraEm = expiraEm;
    }

    public Long getId() { return id; }
    public String getTokenHash() { return tokenHash; }
    public Usuario getUsuario() { return usuario; }
    public Instant getCriadaEm() { return criadaEm; }
    public Instant getExpiraEm() { return expiraEm; }
}
