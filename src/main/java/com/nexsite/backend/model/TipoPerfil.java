package com.nexsite.backend.model;

/**
 * Tipo de conta. O valor (USUARIO / EMPRESA) é o que trafega entre front e back
 * e o que fica salvo no banco, independente do texto exibido na tela.
 */
public enum TipoPerfil {
    USUARIO,
    EMPRESA
}
