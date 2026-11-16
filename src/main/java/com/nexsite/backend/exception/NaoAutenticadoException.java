package com.nexsite.backend.exception;

/** Falta o login, ou o token é inválido/expirou. Vira HTTP 401. */
public class NaoAutenticadoException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public NaoAutenticadoException() {
        super("Faça login para continuar.");
    }
}
