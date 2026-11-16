package com.nexsite.backend.repository;

import com.nexsite.backend.dto.FiltroEmpresa;
import com.nexsite.backend.model.Categoria;
import com.nexsite.backend.model.Avaliacao;
import com.nexsite.backend.model.Empresa;
import com.nexsite.backend.model.TipoEmpresa;
import com.nexsite.backend.model.Usuario;
import com.nexsite.backend.util.RankingNotas;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Monta a consulta de empresas a partir dos filtros. Só entram as condições que foram preenchidas. */
public final class EmpresaSpecs {

    private static final int MAX_TERMOS = 5;

    private EmpresaSpecs() {}

    @SuppressWarnings("unchecked")
    public static Specification<Empresa> comFiltros(FiltroEmpresa f) {
        return (root, query, cb) -> {

            // A consulta de contagem (total de páginas) não pode ter "fetch"; a da lista tem,
            // para trazer o usuário junto e não fazer uma consulta extra por empresa.
            Class<?> tipoResultado = query.getResultType();
            boolean contagem = tipoResultado == Long.class || tipoResultado == long.class;
            Join<Empresa, Usuario> responsavel = contagem
                    ? root.join("responsavel", JoinType.INNER)
                    : (Join<Empresa, Usuario>) root.<Empresa, Usuario>fetch("responsavel", JoinType.INNER);

            List<Predicate> condicoes = new ArrayList<>();

            // ---- Texto livre: cada palavra precisa aparecer no nome, descrição, cidade ou categoria/tipo ----
            for (String termo : separarTermos(f.q())) {
                String padrao = "%" + escaparLike(termo) + "%";
                List<Predicate> ou = new ArrayList<>();
                ou.add(cb.like(cb.lower(responsavel.get("nome")), padrao, '\\'));
                ou.add(cb.like(cb.lower(root.get("descricao")), padrao, '\\'));
                ou.add(cb.like(cb.lower(root.get("cidade")), padrao, '\\'));

                List<Categoria> categoriasDoTermo = categoriasQueContem(termo);
                if (!categoriasDoTermo.isEmpty()) {
                    ou.add(root.get("categoria").in(categoriasDoTermo));
                }
                List<TipoEmpresa> tiposDoTermo = tiposQueContem(termo);
                if (!tiposDoTermo.isEmpty()) {
                    ou.add(root.get("tipo").in(tiposDoTermo));
                }
                condicoes.add(cb.or(ou.toArray(new Predicate[0])));
            }

            // ---- Filtros marcados ----
            if (f.categorias() != null && !f.categorias().isEmpty()) {
                condicoes.add(root.get("categoria").in(f.categorias()));
            }
            if (f.tipos() != null && !f.tipos().isEmpty()) {
                condicoes.add(root.get("tipo").in(f.tipos()));
            }
            if (f.estado() != null) {
                condicoes.add(cb.equal(root.get("estado"), f.estado()));
            }
            if (f.cidade() != null && !f.cidade().isBlank()) {
                condicoes.add(cb.equal(cb.lower(root.get("cidade")), f.cidade().trim().toLowerCase(Locale.ROOT)));
            }

            // ---- Nota mínima: a média (a mesma que aparece na tela, com 1 casa decimal) precisa ser >= notaMin ----
            // Média arredondada >= t  <=>  média >= t - 0,05  <=>  20 x soma >= (20 x t - 1) x total.
            // A conta usa só números inteiros: sem erro de arredondamento de decimal e igual ao que o usuário vê.
            // Quem não tem avaliações (total 0) fica de fora.
            if (f.notaMin() != null) {
                long limite = f.notaMin().multiply(BigDecimal.valueOf(20)).longValueExact() - 1;
                Subquery<Long> total = totalDeAvaliacoes(query, cb, root);
                Subquery<Long> soma = somaDasNotas(query, cb, root);
                condicoes.add(cb.and(
                        cb.greaterThan(total, 0L),
                        cb.greaterThanOrEqualTo(cb.prod(soma, 20L), cb.prod(total, limite))));
            }

            // ---- Faixa de preço: a empresa entra se a faixa dela SE SOBREPÕE à faixa pedida ----
            // (ex.: pedi R$ 100–300; empresa de R$ 250–900 serve, pois R$ 250–300 está na minha faixa)
            if (f.precoMin() != null) {
                condicoes.add(cb.greaterThanOrEqualTo(root.get("precoMax"), f.precoMin()));
            }
            if (f.precoMax() != null) {
                condicoes.add(cb.lessThanOrEqualTo(root.get("precoMin"), f.precoMax()));
            }

            return cb.and(condicoes.toArray(new Predicate[0]));
        };
    }

    /**
     * Ordena por "mais bem avaliadas" (ver RankingNotas): pontuação ponderada pela quantidade de avaliações.
     * Para cada empresa o banco calcula a soma das notas e o total de avaliações (subconsultas) e aplica a fórmula.
     *
     * Ordem final:
     *  1. empresas COM avaliações primeiro; as sem nenhuma avaliação vão para o fim;
     *  2. maior pontuação;
     *  3. empate: mais avaliações primeiro;
     *  4. empate: cadastrada mais recentemente (e, por fim, o id, para a paginação nunca repetir nem pular empresa).
     *
     * Só define a ordenação (não filtra nada), por isso combina com comFiltros(...) usando .and(...).
     * Use com um Pageable SEM ordenação, senão o Spring sobrescreve esta.
     */
    public static Specification<Empresa> ordenarPorNota(double mediaGeral) {
        return (root, query, cb) -> {
            Class<?> tipoResultado = query.getResultType();
            if (tipoResultado == Long.class || tipoResultado == long.class) {
                return null; // consulta de contagem não precisa de ordenação
            }

            Subquery<Long> total = totalDeAvaliacoes(query, cb, root);
            Subquery<Long> soma = somaDasNotas(query, cb, root);

            // (soma + PESO x média geral) / (total + PESO), em ponto flutuante
            Expression<Double> numerador = cb.sum(cb.toDouble(soma), cb.literal(RankingNotas.PESO_PRIOR * mediaGeral));
            Expression<Double> denominador = cb.toDouble(cb.sum(total, cb.literal((long) RankingNotas.PESO_PRIOR)));
            Expression<Number> pontuacao = cb.quot(numerador, denominador);

            Expression<Integer> semAvaliacao = cb.<Integer>selectCase().when(cb.equal(total, 0L), 1).otherwise(0);

            query.orderBy(
                    cb.asc(semAvaliacao),
                    cb.desc(pontuacao),
                    cb.desc(total),
                    cb.desc(root.get("criadoEm")),
                    cb.desc(root.get("id")));
            return null;
        };
    }

    // ---------- auxiliares ----------

    /** Subconsulta: quantas avaliações a empresa (root) tem. */
    private static Subquery<Long> totalDeAvaliacoes(CriteriaQuery<?> query, CriteriaBuilder cb, Root<Empresa> root) {
        Subquery<Long> total = query.subquery(Long.class);
        Root<Avaliacao> a = total.from(Avaliacao.class);
        total.select(cb.count(a)).where(cb.equal(a.get("empresa"), root));
        return total;
    }

    /** Subconsulta: soma das notas da empresa (root); 0 se não tiver avaliações. */
    private static Subquery<Long> somaDasNotas(CriteriaQuery<?> query, CriteriaBuilder cb, Root<Empresa> root) {
        Subquery<Long> soma = query.subquery(Long.class);
        Root<Avaliacao> a = soma.from(Avaliacao.class);
        soma.select(cb.coalesce(cb.sumAsLong(a.<Integer>get("nota")), 0L)).where(cb.equal(a.get("empresa"), root));
        return soma;
    }

    static List<String> separarTermos(String q) {
        if (q == null || q.isBlank()) return List.of();
        return Arrays.stream(q.trim().toLowerCase(Locale.ROOT).split("\\s+"))
                .filter(t -> !t.isEmpty())
                .limit(MAX_TERMOS)
                .toList();
    }

    /** Protege %, _ e \ digitados pelo usuário, para não virarem curingas do LIKE. */
    static String escaparLike(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    // "marketing" acha a categoria Marketing; "educacao" acha Educação (sem acento). Termos curtos são ignorados.
    static List<Categoria> categoriasQueContem(String termo) {
        if (termo.length() < 3) return List.of();
        String t = semAcento(termo);
        return Arrays.stream(Categoria.values())
                .filter(c -> semAcento(c.getRotulo()).contains(t))
                .toList();
    }

    static List<TipoEmpresa> tiposQueContem(String termo) {
        if (termo.length() < 3) return List.of();
        String t = semAcento(termo);
        return Arrays.stream(TipoEmpresa.values())
                .filter(c -> semAcento(c.getRotulo()).contains(t))
                .toList();
    }

    static String semAcento(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
