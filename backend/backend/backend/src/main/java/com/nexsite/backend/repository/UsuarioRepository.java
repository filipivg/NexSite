package com.nexsite.backend.repository;

import com.nexsite.backend.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    // O Spring cria a query de busca automaticamente só de ler o nome do método
    boolean existsByEmail(String email);
}
