package com.nexsite.backend.repository;

import com.nexsite.backend.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    // O Spring cria a query de busca automaticamente só de ler o nome do método
    boolean existsByEmail(String email);

    Optional<Usuario> findByEmail(String email);
}
