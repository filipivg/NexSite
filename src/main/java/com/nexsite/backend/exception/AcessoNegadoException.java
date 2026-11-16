package com.nexsite.backend.exception;

/** A pessoa está logada, mas não pode fazer isso (ex.: conta de empresa avaliando empresas). Vira HTTP 403. */
public class AcessoNegadoException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
