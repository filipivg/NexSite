package com.nexsite.backend.model;

import jakarta.persistence.*;

import java.time.Instant;

/** Nota (1 a 5) e análise opcional que um usuário deu a uma empresa. Cada usuário avalia cada empresa uma única vez. */
@Entity
@Table(name = "avaliacoes",
        uniqueConstraints = @UniqueConstraint(name = "uk_avaliacao_empresa_autor", columnNames = {"empresa_id", "autor_id"}))
public class Avaliacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @ManyToOne(optional = false)
    @JoinColumn(name = "autor_id")
    private Usuario autor;

    @Column(nullable = false)
    private int nota;

    @Column(length = 1000)
    private String comentario;

    @Column(nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(nullable = false)
    private Instant atualizadoEm;

    public Avaliacao() {}

    public Avaliacao(Empresa empresa, Usuario autor, int nota, String comentario) {
        this.empresa = empresa;
        this.autor = autor;
        this.nota = nota;
        this.comentario = comentario;
        this.criadoEm = Instant.now();
        this.atualizadoEm = this.criadoEm;
    }

    /** O autor mudou de ideia: troca nota e texto e marca como editada. */
    public void atualizar(int nota, String comentario) {
        this.nota = nota;
        this.comentario = comentario;
        this.atualizadoEm = Instant.now();
    }

    public Long getId() { return id; }
    public Empresa getEmpresa() { return empresa; }
    public Usuario getAutor() { return autor; }
    public int getNota() { return nota; }
    public String getComentario() { return comentario; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
}
