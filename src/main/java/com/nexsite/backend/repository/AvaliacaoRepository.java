package com.nexsite.backend.repository;

import com.nexsite.backend.model.Avaliacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {

    /** Avaliações de uma empresa, da mais nova para a mais antiga, já com o autor (sem consulta extra por item). */
    @Query(value = "select a from Avaliacao a join fetch a.autor where a.empresa.id = :empresaId order by a.criadoEm desc, a.id desc",
           countQuery = "select count(a) from Avaliacao a where a.empresa.id = :empresaId")
    Page<Avaliacao> listarDaEmpresa(@Param("empresaId") Long empresaId, Pageable pageable);

    @Query("select a from Avaliacao a where a.empresa.id = :empresaId and a.autor.id = :autorId")
    Optional<Avaliacao> buscarDoAutor(@Param("empresaId") Long empresaId, @Param("autorId") Long autorId);

    /** Média e quantidade de uma empresa (sem avaliações: média nula e total 0). */
    @Query("select avg(a.nota) as media, count(a) as total from Avaliacao a where a.empresa.id = :empresaId")
    ResumoNota resumoDaEmpresa(@Param("empresaId") Long empresaId);

    /** Média e quantidade de várias empresas de uma vez (usado nas listagens, sem consulta por empresa). */
    @Query("select a.empresa.id as empresaId, avg(a.nota) as media, count(a) as total from Avaliacao a where a.empresa.id in :ids group by a.empresa.id")
    List<ResumoPorEmpresa> resumoDasEmpresas(@Param("ids") Collection<Long> ids);

    /** Média de TODAS as avaliações do site (null se ninguém avaliou ainda). Base do ranking "Mais bem avaliadas". */
    @Query("select avg(a.nota) from Avaliacao a")
    Double mediaGeral();

    interface ResumoNota {
        Double getMedia();
        long getTotal();
    }

    interface ResumoPorEmpresa {
        Long getEmpresaId();
        Double getMedia();
        long getTotal();
    }
}
