package com.nexsite.backend.service;

import com.nexsite.backend.dto.*;
import com.nexsite.backend.exception.AcessoNegadoException;
import com.nexsite.backend.exception.NaoEncontradoException;
import com.nexsite.backend.exception.RegraNegocioException;
import com.nexsite.backend.model.Avaliacao;
import com.nexsite.backend.model.Empresa;
import com.nexsite.backend.model.TipoPerfil;
import com.nexsite.backend.model.Usuario;
import com.nexsite.backend.repository.AvaliacaoRepository;
import com.nexsite.backend.repository.EmpresaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AvaliacaoService {

    static final int TAMANHO_MAXIMO = 20;

    private final AvaliacaoRepository avaliacaoRepository;
    private final EmpresaRepository empresaRepository;

    public AvaliacaoService(AvaliacaoRepository avaliacaoRepository, EmpresaRepository empresaRepository) {
        this.avaliacaoRepository = avaliacaoRepository;
        this.empresaRepository = empresaRepository;
    }

    /** Lista pública: média, total e uma página de avaliações (mais recentes primeiro). */
    @Transactional(readOnly = true)
    public AvaliacoesDTO listar(Long empresaId, int pagina, int tamanho) {
        exigirEmpresa(empresaId);
        if (pagina < 0) throw new RegraNegocioException("Página inválida.");
        int tamanhoSeguro = Math.min(Math.max(tamanho, 1), TAMANHO_MAXIMO);

        Page<Avaliacao> resultado = avaliacaoRepository.listarDaEmpresa(empresaId, PageRequest.of(pagina, tamanhoSeguro));
        var resumo = avaliacaoRepository.resumoDaEmpresa(empresaId);
        NotaResumo nota = NotaResumo.de(resumo.getMedia(), resumo.getTotal());

        return new AvaliacoesDTO(
                nota.media(),
                nota.total(),
                resultado.getContent().stream().map(AvaliacaoDTO::de).toList(),
                resultado.getNumber(),
                resultado.getSize(),
                resultado.getTotalPages());
    }

    /** A avaliação que o próprio usuário já deu a esta empresa (para preencher o formulário). */
    @Transactional(readOnly = true)
    public Optional<AvaliacaoDTO> minha(Long empresaId, Usuario usuario) {
        exigirEmpresa(empresaId);
        return avaliacaoRepository.buscarDoAutor(empresaId, usuario.getId()).map(AvaliacaoDTO::de);
    }

    /** Cria a avaliação ou, se o usuário já avaliou esta empresa, atualiza a que existe (uma por pessoa). */
    @Transactional
    public AvaliacaoDTO salvar(Long empresaId, Usuario autor, AvaliacaoRequestDTO pedido) {
        if (autor.getTipoPerfil() != TipoPerfil.USUARIO) {
            throw new AcessoNegadoException("Apenas contas de usuário podem avaliar empresas.");
        }
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new NaoEncontradoException("Empresa não encontrada."));

        String comentario = pedido.getComentario() == null ? "" : pedido.getComentario().trim();
        String comentarioFinal = comentario.isEmpty() ? null : comentario;

        Avaliacao avaliacao = avaliacaoRepository.buscarDoAutor(empresaId, autor.getId()).orElse(null);
        if (avaliacao == null) {
            avaliacao = new Avaliacao(empresa, autor, pedido.getNota(), comentarioFinal);
        } else {
            avaliacao.atualizar(pedido.getNota(), comentarioFinal);
        }
        return AvaliacaoDTO.de(avaliacaoRepository.save(avaliacao));
    }

    /** Remove a avaliação do próprio usuário. Se não existir, não há o que fazer (e não é erro). */
    @Transactional
    public void excluir(Long empresaId, Usuario autor) {
        exigirEmpresa(empresaId);
        avaliacaoRepository.buscarDoAutor(empresaId, autor.getId()).ifPresent(avaliacaoRepository::delete);
    }

    private void exigirEmpresa(Long empresaId) {
        if (!empresaRepository.existsById(empresaId)) {
            throw new NaoEncontradoException("Empresa não encontrada.");
        }
    }
}
