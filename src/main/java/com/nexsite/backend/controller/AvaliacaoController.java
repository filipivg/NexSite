package com.nexsite.backend.controller;

import com.nexsite.backend.dto.AvaliacaoDTO;
import com.nexsite.backend.dto.AvaliacaoRequestDTO;
import com.nexsite.backend.dto.AvaliacoesDTO;
import com.nexsite.backend.model.Usuario;
import com.nexsite.backend.service.AvaliacaoService;
import com.nexsite.backend.service.SessaoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/empresas/{empresaId}/avaliacoes")
@CrossOrigin(origins = "*")
public class AvaliacaoController {

    private final AvaliacaoService avaliacaoService;
    private final SessaoService sessaoService;

    public AvaliacaoController(AvaliacaoService avaliacaoService, SessaoService sessaoService) {
        this.avaliacaoService = avaliacaoService;
        this.sessaoService = sessaoService;
    }

    /** Público: média, total e a página pedida (padrão: 5 por vez, pagina=0 é a primeira). */
    @GetMapping
    public AvaliacoesDTO listar(@PathVariable Long empresaId,
                                @RequestParam(defaultValue = "0") int pagina,
                                @RequestParam(defaultValue = "5") int tamanho) {
        return avaliacaoService.listar(empresaId, pagina, tamanho);
    }

    /** Exige login. 204 se o usuário ainda não avaliou esta empresa. */
    @GetMapping("/minha")
    public ResponseEntity<AvaliacaoDTO> minha(@PathVariable Long empresaId,
                                              @RequestHeader(value = "Authorization", required = false) String authorization) {
        Usuario usuario = sessaoService.exigirUsuario(authorization);
        return avaliacaoService.minha(empresaId, usuario)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /** Exige login de usuário comum. Cria a avaliação ou atualiza a que já existe. */
    @PutMapping("/minha")
    public AvaliacaoDTO salvar(@PathVariable Long empresaId,
                               @RequestHeader(value = "Authorization", required = false) String authorization,
                               @Valid @RequestBody AvaliacaoRequestDTO pedido) {
        Usuario usuario = sessaoService.exigirUsuario(authorization);
        return avaliacaoService.salvar(empresaId, usuario, pedido);
    }

    /** Exige login. Apaga a avaliação do próprio usuário. */
    @DeleteMapping("/minha")
    public ResponseEntity<Void> excluir(@PathVariable Long empresaId,
                                        @RequestHeader(value = "Authorization", required = false) String authorization) {
        Usuario usuario = sessaoService.exigirUsuario(authorization);
        avaliacaoService.excluir(empresaId, usuario);
        return ResponseEntity.noContent().build();
    }
}
