package com.nexsite.backend.exception;

/** Lançada quando o login está temporariamente bloqueado por excesso de tentativas erradas. Vira HTTP 429. */
public class MuitasTentativasException extends RuntimeException {

    private final long segundos;

    public MuitasTentativasException(long segundos) {
        super(montarMensagem(segundos));
        this.segundos = segundos;
    }

    /** Tempo que falta para o bloqueio acabar (vai no cabeçalho Retry-After). */
    public long getSegundos() {
        return segundos;
    }

    private static String montarMensagem(long segundos) {
        long minutos = Math.max(1, (segundos + 59) / 60); // arredonda para cima
        return "Muitas tentativas de login. Tente novamente em "
                + minutos + (minutos == 1 ? " minuto." : " minutos.");
    }
}
