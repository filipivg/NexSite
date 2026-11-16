package com.nexsite.backend.exception;

/** Erro de regra de negócio (e-mail em uso, CNPJ inválido etc.). Vira HTTP 400. */
public class RegraNegocioException extends RuntimeException {
    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
