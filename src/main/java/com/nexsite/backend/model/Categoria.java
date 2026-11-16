package com.nexsite.backend.model;

/** Categorias de empresa. O nome (TECNOLOGIA...) trafega na API e fica no banco; o rótulo é o texto exibido. */
public enum Categoria {
    TECNOLOGIA("Tecnologia"),
    MARKETING("Marketing"),
    DESIGN("Design"),
    EDUCACAO("Educação"),
    ALIMENTACAO("Alimentação"),
    SAUDE("Saúde"),
    SERVICOS("Serviços"),
    ECOMMERCE("E-commerce"),
    ENTRETENIMENTO("Entretenimento"),
    OUTROS("Outros");

    private final String rotulo;

    Categoria(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
