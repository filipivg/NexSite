package com.nexsite.backend.repository;

import com.nexsite.backend.model.Sessao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface SessaoRepository extends JpaRepository<Sessao, Long> {

    @Query("select s from Sessao s join fetch s.usuario where s.tokenHash = :hash")
    Optional<Sessao> buscarPorHash(@Param("hash") String hash);

    @Modifying
    @Query("delete from Sessao s where s.tokenHash = :hash")
    int apagarPorHash(@Param("hash") String hash);

    @Modifying
    @Query("delete from Sessao s where s.expiraEm < :agora")
    int apagarExpiradas(@Param("agora") Instant agora);
}
