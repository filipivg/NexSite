package com.nexsite.backend.controller;

import com.nexsite.backend.dto.*;
import com.nexsite.backend.model.Categoria;
import com.nexsite.backend.model.Estado;
import com.nexsite.backend.model.TipoEmpresa;
import com.nexsite.backend.service.EmpresaService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/** Consulta pública de empresas (não exige login). */
@RestController
@RequestMapping("/api/empresas")
@CrossOrigin(origins = "*")
public class EmpresaController {

    private final EmpresaService empresaService;

    public EmpresaController(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    /**
     * Exemplo: GET /api/empresas?categorias=TECNOLOGIA,MARKETING&tipos=LOJA&estado=GO&cidade=Goiânia
     *                           &precoMin=100&precoMax=500&notaMin=4&q=padaria&ordem=nome&pagina=0&tamanho=10
     * "notaMin": só empresas com média de avaliações igual ou maior (ex.: 4 ou 4.5). Quem não tem avaliações fica de fora.
     * Todos os parâmetros são opcionais. "ordem": recentes (padrão), melhores (nota ponderada pela quantidade de avaliações), nome ou preco.
     */
    @GetMapping
    public PaginaDTO<EmpresaResumoDTO> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) List<Categoria> categorias,
            @RequestParam(required = false) List<TipoEmpresa> tipos,
            @RequestParam(required = false) Estado estado,
            @RequestParam(required = false) String cidade,
            @RequestParam(required = false) BigDecimal precoMin,
            @RequestParam(required = false) BigDecimal precoMax,
            @RequestParam(required = false) BigDecimal notaMin,
            @RequestParam(defaultValue = "recentes") String ordem,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho) {

        FiltroEmpresa filtro = new FiltroEmpresa(q, categorias, tipos, estado, cidade, precoMin, precoMax, notaMin);
        return empresaService.buscar(filtro, ordem, pagina, tamanho);
    }

    /** Perfil completo de uma empresa. Responde 404 se o id não existir. */
    @GetMapping("/{id}")
    public EmpresaPerfilDTO perfil(@PathVariable Long id) {
        return empresaService.buscarPorId(id);
    }

    /** Totais por categoria (página inicial). */
    @GetMapping("/resumo")
    public ResumoDTO resumo() {
        return empresaService.resumo();
    }

    /** Listas de categorias e tipos, para o front montar filtros e formulário sem repetir os valores. */
    @GetMapping("/opcoes")
    public OpcoesDTO opcoes() {
        return empresaService.opcoes();
    }
}
