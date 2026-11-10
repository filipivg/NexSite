package com.nexsite.backend.service;

import com.nexsite.backend.dto.CadastroDTO;
import com.nexsite.backend.model.Empresa;
import com.nexsite.backend.model.Usuario;
import com.nexsite.backend.repository.EmpresaRepository;
import com.nexsite.backend.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CadastroService {

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;

    // O Spring injeta automaticamente o acesso ao banco de dados aqui
    public CadastroService(UsuarioRepository usuarioRepository, EmpresaRepository empresaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
    }

    @Transactional
    public Usuario registrarConta(CadastroDTO dto) {
        
        // 1. Verificação no PostgreSQL se o e-mail existe
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Este e-mail já está em uso.");
        }

        boolean isEmpresa = "Perfil empresarial".equals(dto.getTipoPerfil());

        // 2. Verificação no PostgreSQL se o CNPJ existe
        if (isEmpresa && dto.getCnpj() != null && !dto.getCnpj().isEmpty()) {
            if (empresaRepository.existsByCnpj(dto.getCnpj())) {
                throw new IllegalArgumentException("Este CNPJ já está cadastrado.");
            }
        }

        String senhaCriptografada = "[HASH_GERADO]-" + dto.getSenha();

        // 4. Salva o usuário no banco de dados real
        Usuario novoUsuario = new Usuario(dto.getNome(), dto.getEmail(), senhaCriptografada, dto.getTipoPerfil());
        Usuario usuarioSalvo = usuarioRepository.save(novoUsuario);

        // 5. Salva a empresa atrelada ao usuário recém-criado
        if (isEmpresa) {
            Empresa novaEmpresa = new Empresa(dto.getCnpj(), usuarioSalvo);
            empresaRepository.save(novaEmpresa);
        }
        
        return usuarioSalvo;
    }
}