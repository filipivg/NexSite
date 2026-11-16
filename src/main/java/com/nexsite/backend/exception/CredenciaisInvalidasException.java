package com.nexsite.backend.exception;

/** Lançada quando e-mail ou senha não conferem no login. */
public class CredenciaisInvalidasException extends RuntimeException {
    public CredenciaisInvalidasException() {
        // Mensagem única de propósito: não revela se foi o e-mail ou a senha que errou
        super("E-mail ou senha incorretos.");
    }
}
