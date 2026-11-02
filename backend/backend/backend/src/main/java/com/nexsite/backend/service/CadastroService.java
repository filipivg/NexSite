package com.nexsite.backend.service;

import com.nexsite.backend.dto.CadastroDTO;
import com.nexsite.backend.model.Empresa;
import com.nexsite.backend.model.Usuario;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CadastroService {

    // Simulação da Base de Dados
    private final List<Usuario> bancoDeUsuarios = new ArrayList<>();
    private final List<Empresa> bancoDeEmpresas = new ArrayList<>();

    public Usuario registrarConta(CadastroDTO dto) {
        
        // Regra 1: Verificação de duplicidade de e-mail
        boolean emailExiste = bancoDeUsuarios.stream()
                .anyMatch(u -> u.getEmail().equals(dto.getEmail()));
        
        if (emailExiste) {
            throw new IllegalArgumentException("Este e-mail já está em uso.");
        }

        boolean isEmpresa = "Perfil empresarial".equals(dto.getTipoPerfil());

        // Regra 2: Verificação de duplicidade de CNPJ
        if (isEmpresa && dto.getCnpj() != null && !dto.getCnpj().isEmpty()) {
            boolean cnpjExiste = bancoDeEmpresas.stream()
                    .anyMatch(e -> e.getCnpj().equals(dto.getCnpj()));
            
            if (cnpjExiste) {
                throw new IllegalArgumentException("Este CNPJ já está cadastrado.");
            }
        }

        // Regra 3: Criptografia da senha (Simulação)
        String senhaCriptografada = "[HASH_GERADO]-" + dto.getSenha();

        // 4. Criação do Utilizador
        Usuario novoUsuario = new Usuario(dto.getNome(), dto.getEmail(), senhaCriptografada, dto.getTipoPerfil());
        bancoDeUsuarios.add(novoUsuario);

        // 5. Criação da Empresa (se aplicável)
        if (isEmpresa) {
            Empresa novaEmpresa = new Empresa(dto.getCnpj(), novoUsuario);
            bancoDeEmpresas.add(novaEmpresa);
            System.out.println("Empresa registada com sucesso: " + novaEmpresa.getCnpj());
        }

        System.out.println("Utilizador registado com sucesso: " + novoUsuario.getEmail());
        
        return novoUsuario;
    }
}