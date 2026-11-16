package com.nexsite.backend.repository;

import com.nexsite.backend.model.Categoria;
import com.nexsite.backend.model.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Long>, JpaSpecificationExecutor<Empresa> {

    boolean existsByCnpj(String cnpj);

    /** Quantas empresas há em cada categoria (só aparecem categorias que têm pelo menos uma). */
    @Query("select e.categoria as categoria, count(e) as total from Empresa e group by e.categoria")
    List<ContagemCategoria> contarPorCategoria();

    interface ContagemCategoria {
        Categoria getCategoria();
        long getTotal();
    }
}
