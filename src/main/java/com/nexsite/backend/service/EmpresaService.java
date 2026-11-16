package com.nexsite.backend.service;

import com.nexsite.backend.dto.*;
import com.nexsite.backend.exception.NaoEncontradoException;
import com.nexsite.backend.exception.RegraNegocioException;
import com.nexsite.backend.model.Categoria;
import com.nexsite.backend.model.Empresa;
import com.nexsite.backend.model.TipoEmpresa;
import com.nexsite.backend.repository.AvaliacaoRepository;
import com.nexsite.backend.repository.EmpresaRepository;
import com.nexsite.backend.repository.EmpresaSpecs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmpresaService {

    static final int TAMANHO_MAXIMO = 50;
    private static final int MAX_TEXTO = 100;

    private final EmpresaRepository empresaRepository;
    private final AvaliacaoRepository avaliacaoRepository;

    public EmpresaService(EmpresaRepository empresaRepository, AvaliacaoRepository avaliacaoRepository) {
        this.empresaRepository = empresaRepository;
        this.avaliacaoRepository = avaliacaoRepository;
    }

    @Transactional(readOnly = true)
    public PaginaDTO<EmpresaResumoDTO> buscar(FiltroEmpresa filtro, String ordem, int pagina, int tamanho) {
        validar(filtro);

        if (pagina < 0) {
            throw new RegraNegocioException("Página inválida.");
        }
        // Limita o tamanho da página: ninguém consegue pedir a tabela inteira de uma vez
        int tamanhoSeguro = Math.min(Math.max(tamanho, 1), TAMANHO_MAXIMO);

        Specification<Empresa> consulta = EmpresaSpecs.comFiltros(filtro);
        Sort ordenacao;
        if ("melhores".equals(ordem)) {
            // "Mais bem avaliadas": nota ponderada pela quantidade de avaliações (ver RankingNotas)
            Double mediaGeral = avaliacaoRepository.mediaGeral();
            if (mediaGeral == null) {
                ordenacao = ordenacao("recentes"); // ninguém avaliou ainda: não há o que ranquear
            } else {
                consulta = consulta.and(EmpresaSpecs.ordenarPorNota(mediaGeral));
                ordenacao = Sort.unsorted(); // a ordenação já vem dentro da consulta
            }
        } else {
            ordenacao = ordenacao(ordem);
        }

        Page<Empresa> resultado = empresaRepository.findAll(
                consulta,
                PageRequest.of(pagina, tamanhoSeguro, ordenacao));

        // Notas de todas as empresas da página em UMA consulta (sem uma consulta por empresa)
        Map<Long, NotaResumo> notas = notasDe(resultado.getContent());

        return new PaginaDTO<>(
                resultado.getContent().stream()
                        .map(e -> EmpresaResumoDTO.de(e, notas.getOrDefault(e.getId(), NotaResumo.VAZIO)))
                        .toList(),
                resultado.getNumber(),
                resultado.getSize(),
                resultado.getTotalElements(),
                resultado.getTotalPages());
    }

    @Transactional(readOnly = true)
    public EmpresaPerfilDTO buscarPorId(Long id) {
        Empresa empresa = empresaRepository.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Empresa não encontrada."));
        var resumo = avaliacaoRepository.resumoDaEmpresa(id);
        return EmpresaPerfilDTO.de(empresa, NotaResumo.de(resumo.getMedia(), resumo.getTotal()));
    }

    @Transactional(readOnly = true)
    public ResumoDTO resumo() {
        // Todas as categorias aparecem, mesmo as sem empresas (com 0)
        Map<String, Long> porCategoria = new LinkedHashMap<>();
        for (Categoria c : Categoria.values()) {
            porCategoria.put(c.name(), 0L);
        }
        empresaRepository.contarPorCategoria()
                .forEach(c -> porCategoria.put(c.getCategoria().name(), c.getTotal()));

        long total = porCategoria.values().stream().mapToLong(Long::longValue).sum();
        return new ResumoDTO(total, porCategoria);
    }

    public OpcoesDTO opcoes() {
        return new OpcoesDTO(
                Arrays.stream(Categoria.values()).map(c -> new OpcaoDTO(c.name(), c.getRotulo())).toList(),
                Arrays.stream(TipoEmpresa.values()).map(t -> new OpcaoDTO(t.name(), t.getRotulo())).toList());
    }

    // ---------- auxiliares ----------

    private Map<Long, NotaResumo> notasDe(List<Empresa> empresas) {
        Map<Long, NotaResumo> notas = new HashMap<>();
        if (empresas.isEmpty()) return notas;
        avaliacaoRepository.resumoDasEmpresas(empresas.stream().map(Empresa::getId).toList())
                .forEach(r -> notas.put(r.getEmpresaId(), NotaResumo.de(r.getMedia(), r.getTotal())));
        return notas;
    }

    private static Sort ordenacao(String ordem) {
        String o = ordem == null ? "recentes" : ordem;
        return switch (o) {
            case "recentes" -> Sort.by(Sort.Order.desc("criadoEm"), Sort.Order.desc("id"));
            case "nome" -> Sort.by(Sort.Order.asc("responsavel.nome"), Sort.Order.asc("id"));
            case "preco" -> Sort.by(Sort.Order.asc("precoMin"), Sort.Order.asc("responsavel.nome"), Sort.Order.asc("id"));
            default -> throw new RegraNegocioException("Ordenação inválida.");
        };
    }

    private static void validar(FiltroEmpresa f) {
        if (f.q() != null && f.q().length() > MAX_TEXTO) {
            throw new RegraNegocioException("O texto da busca é muito longo.");
        }
        if (f.cidade() != null && f.cidade().length() > MAX_TEXTO) {
            throw new RegraNegocioException("O nome da cidade é muito longo.");
        }
        if (isNegativo(f.precoMin()) || isNegativo(f.precoMax())) {
            throw new RegraNegocioException("A faixa de preço não pode ser negativa.");
        }
        if (f.precoMin() != null && f.precoMax() != null && f.precoMin().compareTo(f.precoMax()) > 0) {
            throw new RegraNegocioException("O preço mínimo não pode ser maior que o máximo.");
        }
        BigDecimal nota = f.notaMin();
        if (nota != null) {
            if (nota.compareTo(BigDecimal.ONE) < 0 || nota.compareTo(BigDecimal.valueOf(5)) > 0) {
                throw new RegraNegocioException("A nota mínima deve estar entre 1 e 5.");
            }
            if (nota.stripTrailingZeros().scale() > 1) {
                throw new RegraNegocioException("A nota mínima deve ter no máximo uma casa decimal (ex.: 4 ou 4.5).");
            }
        }
    }

    private static boolean isNegativo(BigDecimal valor) {
        return valor != null && valor.signum() < 0;
    }
}
