package com.nexsite.backend.service;

import com.nexsite.backend.dto.CadastroDTO;
import com.nexsite.backend.exception.RegraNegocioException;
import com.nexsite.backend.model.Empresa;
import com.nexsite.backend.model.TipoPerfil;
import com.nexsite.backend.model.Usuario;
import com.nexsite.backend.repository.EmpresaRepository;
import com.nexsite.backend.repository.UsuarioRepository;
import com.nexsite.backend.util.CnpjValidator;
import com.nexsite.backend.util.ContatoUtil;
import java.math.BigDecimal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CadastroService {

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final PasswordEncoder passwordEncoder;

    // O Spring injeta automaticamente as dependências aqui
    public CadastroService(UsuarioRepository usuarioRepository,
                           EmpresaRepository empresaRepository,
                           PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Se duas requisições simultâneas passarem pelas checagens abaixo com o mesmo e-mail/CNPJ,
    // o banco barra a segunda (UNIQUE) e o GlobalExceptionHandler devolve a mensagem de "já em uso".
    @Transactional
    public Usuario registrarConta(CadastroDTO dto) {

        // E-mail sempre minúsculo e sem espaços, para o login encontrar a mesma conta
        String email = dto.getEmail().trim().toLowerCase();
        boolean isEmpresa = dto.getTipoPerfil() == TipoPerfil.EMPRESA;

        // Para empresa o CNPJ é obrigatório e precisa ter dígitos verificadores corretos;
        // para usuário comum ele é ignorado (fica null)
        String cnpj = null;
        String site = null;
        String telefone = null;
        if (isEmpresa) {
            cnpj = dto.getCnpj() == null ? "" : dto.getCnpj().trim();
            if (cnpj.isEmpty()) {
                throw new RegraNegocioException("O CNPJ é obrigatório para perfil empresarial.");
            }
            if (!CnpjValidator.isValido(cnpj)) {
                throw new RegraNegocioException("CNPJ inválido. Confira os números digitados.");
            }
            validarDadosDaEmpresa(dto);
            site = ContatoUtil.normalizarSite(dto.getSite());           // null se vazio; erro se inválido
            telefone = ContatoUtil.normalizarTelefone(dto.getTelefone()); // só dígitos
        }

        // 1. Verificação no PostgreSQL se o e-mail existe
        if (usuarioRepository.existsByEmail(email)) {
            throw new RegraNegocioException("Este e-mail já está em uso.");
        }

        // 2. Verificação no PostgreSQL se o CNPJ existe
        if (isEmpresa && empresaRepository.existsByCnpj(cnpj)) {
            throw new RegraNegocioException("Este CNPJ já está cadastrado.");
        }

        // 3. Hash BCrypt da senha (a senha original nunca é salva)
        String senhaHash = passwordEncoder.encode(dto.getSenha());

        // 4. Salva o usuário
        Usuario novoUsuario = new Usuario(dto.getNome().trim(), email, senhaHash, dto.getTipoPerfil());
        Usuario usuarioSalvo = usuarioRepository.save(novoUsuario);

        // 5. Salva a empresa atrelada ao usuário recém-criado
        if (isEmpresa) {
            Empresa empresa = new Empresa(cnpj, usuarioSalvo);
            empresa.setCategoria(dto.getCategoria());
            empresa.setTipo(dto.getTipoEmpresa());
            empresa.setEstado(dto.getEstado());
            empresa.setCidade(dto.getCidade().trim().replaceAll("\\s+", " "));
            empresa.setPrecoMin(dto.getPrecoMin());
            empresa.setPrecoMax(dto.getPrecoMax());
            String descricao = dto.getDescricao() == null ? "" : dto.getDescricao().trim();
            empresa.setDescricao(descricao.isEmpty() ? null : descricao);
            empresa.setSite(site);
            empresa.setTelefone(telefone);
            empresaRepository.save(empresa);
        }

        return usuarioSalvo;
    }
    /** Campos do anúncio que são obrigatórios para quem cadastra uma empresa. */
    private void validarDadosDaEmpresa(CadastroDTO dto) {
        if (dto.getCategoria() == null) {
            throw new RegraNegocioException("Escolha a categoria da empresa.");
        }
        if (dto.getTipoEmpresa() == null) {
            throw new RegraNegocioException("Informe se a empresa é prestadora de serviços, loja ou site.");
        }
        if (dto.getEstado() == null || dto.getCidade() == null || dto.getCidade().isBlank()) {
            throw new RegraNegocioException("Informe o estado e a cidade da empresa.");
        }
        BigDecimal min = dto.getPrecoMin();
        BigDecimal max = dto.getPrecoMax();
        if (min == null || max == null) {
            throw new RegraNegocioException("Informe a faixa de preço (mínimo e máximo).");
        }
        if (max.compareTo(min) < 0) {
            throw new RegraNegocioException("O preço máximo não pode ser menor que o mínimo.");
        }
    }
}
