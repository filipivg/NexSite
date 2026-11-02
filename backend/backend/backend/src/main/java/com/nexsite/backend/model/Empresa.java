package com.nexsite.backend.model;

public class Empresa {
    private String cnpj;
    private Usuario responsavel;

    public Empresa(String cnpj, Usuario responsavel) {
        this.cnpj = cnpj;
        this.responsavel = responsavel;
    }

    // Getters e Setters
    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }

    public Usuario getResponsavel() { return responsavel; }
    public void setResponsavel(Usuario responsavel) { this.responsavel = responsavel; }
}