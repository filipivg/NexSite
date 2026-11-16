package com.nexsite.backend.dto;

import java.util.List;

/** Listas que o front usa para montar filtros e formulário, vindas do próprio back-end. */
public record OpcoesDTO(List<OpcaoDTO> categorias, List<OpcaoDTO> tipos) {}
