package com.nexsite.backend.exception;

/** Recurso que não existe (ex.: empresa com esse id). Vira HTTP 404. */
public class NaoEncontradoException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public NaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
