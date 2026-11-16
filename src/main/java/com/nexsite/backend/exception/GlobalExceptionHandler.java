package com.nexsite.backend.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Todo erro da API sai neste mesmo formato: {"erro": "mensagem"}.
 * É o campo que o script.js lê para mostrar o aviso ao usuário.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Ordem dos campos no formulário: assim a mensagem mostrada é sempre a do primeiro campo com problema
    private static final List<String> ORDEM_CAMPOS = List.of("nome", "email", "senha", "tipoPerfil", "cnpj", "nota", "comentario");

    /** Falha no @Valid (senha curta, e-mail inválido, formato de CNPJ etc.) */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidacao(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();

        ex.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator
                        .comparingInt((FieldError e) -> posicao(e.getField()))
                        // "obrigatório" vem antes de "tamanho inválido" no mesmo campo
                        .thenComparing(e -> isObrigatorio(e) ? 0 : 1))
                .forEach(e -> campos.putIfAbsent(e.getField(), e.getDefaultMessage()));

        String erro = campos.values().stream().findFirst().orElse("Dados inválidos.");

        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("erro", erro);
        corpo.put("campos", campos); // mensagem de cada campo, caso o front queira destacar um por um
        return ResponseEntity.badRequest().body(corpo);
    }

    /** JSON quebrado ou valor impossível (ex.: tipoPerfil = "QUALQUER_COISA") */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleJsonInvalido(HttpMessageNotReadableException ex) {
        return erro(HttpStatus.BAD_REQUEST, "Requisição inválida. Verifique os dados enviados.");
    }

    /** Parâmetro de URL com valor impossível (ex.: ?estado=XX, ?precoMin=abc ou /empresas/abc). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleParametroInvalido(MethodArgumentTypeMismatchException ex) {
        return erro(HttpStatus.BAD_REQUEST, "Valor inválido para o parâmetro '" + ex.getName() + "'.");
    }

    /** Pedido que exige login, sem token ou com token inválido/expirado. */
    @ExceptionHandler(NaoAutenticadoException.class)
    public ResponseEntity<Map<String, String>> handleNaoAutenticado(NaoAutenticadoException ex) {
        return erro(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    /** Logado, mas sem permissão para a ação. */
    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<Map<String, String>> handleAcessoNegado(AcessoNegadoException ex) {
        return erro(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    /** Recurso inexistente (ex.: perfil de empresa com id que não existe). */
    @ExceptionHandler(NaoEncontradoException.class)
    public ResponseEntity<Map<String, String>> handleNaoEncontrado(NaoEncontradoException ex) {
        return erro(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** Regras de negócio: e-mail em uso, CNPJ inválido ou repetido... */
    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<Map<String, String>> handleRegraNegocio(RegraNegocioException ex) {
        return erro(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /** Login com e-mail ou senha errados */
    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<Map<String, String>> handleCredenciais(CredenciaisInvalidasException ex) {
        return erro(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    /** Login bloqueado por excesso de tentativas. Retry-After diz, em segundos, quando tentar de novo. */
    @ExceptionHandler(MuitasTentativasException.class)
    public ResponseEntity<Map<String, String>> handleMuitasTentativas(MuitasTentativasException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getSegundos()))
                .body(Map.of("erro", ex.getMessage()));
    }

    /**
     * O banco recusou a gravação. O caso que interessa é o de duas requisições simultâneas
     * com o mesmo e-mail/CNPJ: as duas passam no existsBy..., mas a constraint UNIQUE barra a segunda.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleIntegridade(DataIntegrityViolationException ex) {
        Throwable causa = NestedExceptionUtils.getMostSpecificCause(ex);
        String detalhe = causa.getMessage() == null ? "" : causa.getMessage().toLowerCase();

        if (detalhe.contains("duplicate key") || detalhe.contains("unique")) {
            // O PostgreSQL informa a coluna na mensagem: "Key (email)=(...) already exists."
            if (detalhe.contains("(email)")) {
                return erro(HttpStatus.CONFLICT, "Este e-mail já está em uso.");
            }
            if (detalhe.contains("(cnpj)")) {
                return erro(HttpStatus.CONFLICT, "Este CNPJ já está cadastrado.");
            }
            // dois envios simultâneos da mesma avaliação: a regra "uma por usuário" no banco barra o segundo
            if (detalhe.contains("(empresa_id, autor_id)")) {
                return erro(HttpStatus.CONFLICT, "Você já avaliou esta empresa. Atualize a página e edite a sua avaliação.");
            }
            return erro(HttpStatus.CONFLICT, "Já existe um cadastro com estes dados.");
        }

        // Qualquer outra violação (coluna obrigatória vazia etc.) é problema nosso, não do usuário
        log.error("Violação de integridade não esperada", ex);
        return erro(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao salvar os dados. Tente novamente.");
    }

    // ---------- auxiliares ----------

    private static ResponseEntity<Map<String, String>> erro(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(Map.of("erro", mensagem));
    }

    private static int posicao(String campo) {
        int i = ORDEM_CAMPOS.indexOf(campo);
        return i < 0 ? Integer.MAX_VALUE : i;
    }

    private static boolean isObrigatorio(FieldError e) {
        return "NotBlank".equals(e.getCode()) || "NotNull".equals(e.getCode());
    }
}
