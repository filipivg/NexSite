package com.nexsite.backend.model;

/** Como a empresa atua: prestando serviços, vendendo em loja ou sendo um site. */
public enum TipoEmpresa {
    PRESTADORA_SERVICOS("Prestadora de serviços"),
    LOJA("Loja"),
    SITE("Site");

    private final String rotulo;

    TipoEmpresa(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
