// =====================================================================
// NexSite: script único das páginas (index, busca, filtros, cadastro, login)
// Cada página só ativa o trecho dela, quando encontra os elementos certos.
//
// REGRA DE SEGURANÇA: nome, descrição e cidade das empresas são digitados por
// outras pessoas. Por isso tudo que vem da API entra na tela com textContent
// (texto puro) e nunca com innerHTML.
// =====================================================================

const API_URL = 'http://localhost:8080/api';
const IBGE_URL = 'https://servicodados.ibge.gov.br/api/v1/localidades';
const CHAVE_USUARIO = 'nexsiteUsuario';
const TAMANHO_PAGINA = 10;

// Filtro de nota mínima: média de avaliações "igual ou maior que". O valor vai para a API em ?notaMin=
const OPCOES_NOTA = [
    { valor: '4.5', rotulo: '4,5 ou mais', curto: '4,5+' },
    { valor: '4', rotulo: '4 ou mais', curto: '4+' },
    { valor: '3', rotulo: '3 ou mais', curto: '3+' },
    { valor: '2', rotulo: '2 ou mais', curto: '2+' }
];

// Lista fixa das 27 UFs: só é usada se a API do IBGE estiver fora do ar.
const UFS_FALLBACK = [
    ['AC', 'Acre'], ['AL', 'Alagoas'], ['AP', 'Amapá'], ['AM', 'Amazonas'], ['BA', 'Bahia'], ['CE', 'Ceará'],
    ['DF', 'Distrito Federal'], ['ES', 'Espírito Santo'], ['GO', 'Goiás'], ['MA', 'Maranhão'], ['MT', 'Mato Grosso'],
    ['MS', 'Mato Grosso do Sul'], ['MG', 'Minas Gerais'], ['PA', 'Pará'], ['PB', 'Paraíba'], ['PR', 'Paraná'],
    ['PE', 'Pernambuco'], ['PI', 'Piauí'], ['RJ', 'Rio de Janeiro'], ['RN', 'Rio Grande do Norte'],
    ['RS', 'Rio Grande do Sul'], ['RO', 'Rondônia'], ['RR', 'Roraima'], ['SC', 'Santa Catarina'],
    ['SP', 'São Paulo'], ['SE', 'Sergipe'], ['TO', 'Tocantins']
].map(([sigla, nome]) => ({ sigla, nome }));

// =====================================================================
// AUXILIARES
// =====================================================================

class ErroApi extends Error {
    constructor(mensagem, status) {
        super(mensagem);
        this.status = status; // código HTTP (404 = não encontrado)
    }
}

const MSG_SEM_CONEXAO = 'Não foi possível conectar à API. Verifique se o back-end está rodando na porta 8080.';

function mensagemDoErro(erro) {
    return erro instanceof ErroApi ? erro.message : MSG_SEM_CONEXAO;
}

async function getJson(url) {
    const resposta = await fetch(url);
    if (!resposta.ok) {
        let mensagem = 'Não foi possível concluir a consulta.';
        try {
            const corpo = await resposta.json();
            if (corpo && corpo.erro) mensagem = corpo.erro;
        } catch (_) { /* resposta sem JSON: mantém a mensagem padrão */ }
        throw new ErroApi(mensagem, resposta.status);
    }
    return resposta.json();
}

function criar(tag, classe, texto) {
    const elemento = document.createElement(tag);
    if (classe) elemento.className = classe;
    if (texto !== undefined && texto !== null) elemento.textContent = texto;
    return elemento;
}

function atrasar(funcao, ms) {
    let temporizador;
    return (...args) => {
        clearTimeout(temporizador);
        temporizador = setTimeout(() => funcao(...args), ms);
    };
}

// Valor redondo sem centavos (R$ 120); com centavos sempre 2 casas (R$ 120,50, nunca R$ 120,5)
const moedaInteira = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL', minimumFractionDigits: 0, maximumFractionDigits: 0 });
const moedaCentavos = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL', minimumFractionDigits: 2, maximumFractionDigits: 2 });

function formatarMoeda(valor) {
    const numero = Number(valor);
    return (Number.isInteger(numero) ? moedaInteira : moedaCentavos).format(numero);
}

function formatarFaixaPreco(min, max) {
    if (min === null || min === undefined || max === null || max === undefined) return '';
    return Number(min) === Number(max)
        ? formatarMoeda(min)
        : formatarMoeda(min) + ' – ' + formatarMoeda(max);
}

function pluralizar(n, singular, plural) {
    return n.toLocaleString('pt-BR') + ' ' + (n === 1 ? singular : plural);
}

function inicial(nome) {
    return ((nome || '').trim()[0] || '?').toUpperCase();
}

// "Novo" = cadastrada há menos de 30 dias
function ehNovo(dataIso) {
    const quando = new Date(dataIso).getTime();
    return !Number.isNaN(quando) && Date.now() - quando < 30 * 24 * 60 * 60 * 1000;
}

// "novembro de 2026"
function formatarMesAno(dataIso) {
    const data = new Date(dataIso);
    return Number.isNaN(data.getTime()) ? '' : data.toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' });
}

// Só aceita links http/https. Mesmo que algo estranho chegue da API, "javascript:" etc. nunca vira link.
function urlSegura(valor) {
    try {
        const url = new URL(valor);
        return url.protocol === 'http:' || url.protocol === 'https:' ? url : null;
    } catch (_) {
        return null;
    }
}

// "62999999999" -> "(62) 99999-9999"; "6233334444" -> "(62) 3333-4444"
function formatarTelefone(digitos) {
    if (!/^\d{10,11}$/.test(digitos || '')) return '';
    const corte = digitos.length === 11 ? 7 : 6;
    return `(${digitos.slice(0, 2)}) ${digitos.slice(2, corte)}-${digitos.slice(corte)}`;
}

// Máscara enquanto a pessoa digita: (62) 99999-9999 ou (62) 3333-4444
function mascararTelefone(valor) {
    let d = valor.replace(/\D/g, '');
    if (d.length > 11 && d.startsWith('55')) d = d.slice(2); // colou com +55
    d = d.slice(0, 11);
    if (d.length === 0) return '';
    if (d.length <= 2) return '(' + d;
    if (d.length <= 6) return `(${d.slice(0, 2)}) ${d.slice(2)}`;
    const corte = d.length === 11 ? 7 : 6;
    return `(${d.slice(0, 2)}) ${d.slice(2, corte)}-${d.slice(corte)}`;
}

function linkPerfil(empresa) {
    return 'perfil.html?id=' + encodeURIComponent(empresa.id);
}

// Nome da empresa como link para o perfil
function criarNomeComLink(empresa) {
    const nome = criar('div', 'co-name');
    const link = criar('a', null, empresa.nome);
    link.href = linkPerfil(empresa);
    nome.append(link);
    return nome;
}

// Estrelas de 0 a 5 (aceita fração: 4,3 enche 86% da quinta estrela)
function criarEstrelas(valor, rotulo) {
    const caixa = criar('span', 'estrelas');
    caixa.setAttribute('role', 'img');
    caixa.setAttribute('aria-label', rotulo || `Nota ${valor} de 5`);
    const cheias = criar('span', 'estrelas-cheias', '★★★★★');
    cheias.setAttribute('aria-hidden', 'true');
    cheias.style.width = (Math.max(0, Math.min(5, Number(valor) || 0)) / 5) * 100 + '%';
    caixa.append(cheias);
    return caixa;
}

// 4.3 -> "4,3"; 5 -> "5,0"
function formatarNota(nota) {
    return Number(nota).toLocaleString('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 1 });
}

// "★ 4,3 (12)" ou "Sem avaliações" para os cartões
function criarSeloNota(empresa) {
    if (!empresa.totalAvaliacoes) return criar('span', 'co-rating sem', 'Sem avaliações');
    const selo = criar('span', 'co-rating', `★ ${formatarNota(empresa.notaMedia)} (${empresa.totalAvaliacoes.toLocaleString('pt-BR')})`);
    selo.title = `Nota média ${formatarNota(empresa.notaMedia)} de 5, ${pluralizar(empresa.totalAvaliacoes, 'avaliação', 'avaliações')}`;
    return selo;
}

function formatarData(dataIso) {
    const data = new Date(dataIso);
    return Number.isNaN(data.getTime()) ? '' : data.toLocaleDateString('pt-BR', { day: 'numeric', month: 'long', year: 'numeric' });
}

function mostrarMensagem(container, titulo, texto, erro, acao) {
    const caixa = criar('div', 'state-msg' + (erro ? ' erro' : ''));
    caixa.append(criar('b', null, titulo), document.createTextNode(texto));
    if (acao) caixa.append(document.createElement('br'), acao);
    container.replaceChildren(caixa);
}

// =====================================================================
// OPÇÕES (categorias e tipos) vindas do back-end: uma única fonte da verdade
// =====================================================================

let _opcoes = null;

function carregarOpcoes() {
    if (!_opcoes) {
        _opcoes = getJson(`${API_URL}/empresas/opcoes`).catch((erro) => {
            _opcoes = null; // tenta de novo na próxima vez
            throw erro;
        });
    }
    return _opcoes;
}

// =====================================================================
// ESTADOS E CIDADES (API do IBGE)
// =====================================================================

let _estados = null;
const _cidades = {};

async function carregarEstados() {
    if (_estados) return _estados;
    try {
        const dados = await getJson(`${IBGE_URL}/estados?orderBy=nome`);
        _estados = dados.map((e) => ({ sigla: e.sigla, nome: e.nome }));
    } catch (_) {
        _estados = UFS_FALLBACK; // IBGE fora do ar: usa a lista fixa
    }
    return _estados;
}

// Devolve a lista de cidades, ou null se o IBGE não respondeu
async function carregarCidades(uf) {
    if (_cidades[uf]) return _cidades[uf];
    try {
        const dados = await getJson(`${IBGE_URL}/estados/${encodeURIComponent(uf)}/municipios?orderBy=nome`);
        _cidades[uf] = dados.map((m) => m.nome);
        return _cidades[uf];
    } catch (_) {
        return null;
    }
}

/**
 * Liga um <select> de estado a um <select> de cidade (carregada conforme o estado).
 * Se o IBGE falhar, a cidade vira um campo de texto, para ninguém ficar travado.
 */
function criarSeletorLocalidade({ estado, cidade, cidadeTexto, rotuloEstado, rotuloCidade, aoMudar }) {

    function resetarCidade() {
        cidade.replaceChildren(new Option(rotuloCidade, ''));
        cidade.disabled = true;
        cidade.hidden = false;
        if (cidadeTexto) {
            cidadeTexto.hidden = true;
            cidadeTexto.value = '';
        }
    }

    async function preencherCidades(uf, selecionada) {
        resetarCidade();
        if (!uf) return;

        cidade.replaceChildren(new Option('Carregando…', ''));
        const lista = await carregarCidades(uf);
        if (estado.value !== uf) return; // o usuário trocou de estado enquanto carregava

        if (lista) {
            cidade.replaceChildren(new Option(rotuloCidade, ''), ...lista.map((nome) => new Option(nome, nome)));
            cidade.disabled = false;
            if (selecionada) {
                const igual = Array.from(cidade.options).find((o) => o.value.toLowerCase() === selecionada.toLowerCase());
                cidade.value = igual ? igual.value : '';
            }
        } else if (cidadeTexto) {
            cidade.hidden = true;
            cidadeTexto.hidden = false;
            cidadeTexto.value = selecionada || '';
        } else {
            cidade.replaceChildren(new Option(rotuloCidade, ''));
        }
    }

    estado.addEventListener('change', async () => {
        await preencherCidades(estado.value, '');
        if (aoMudar) aoMudar();
    });
    cidade.addEventListener('change', () => { if (aoMudar) aoMudar(); });
    if (cidadeTexto) {
        cidadeTexto.addEventListener('input', atrasar(() => { if (aoMudar) aoMudar(); }, 400));
    }

    return {
        async iniciar() {
            const estados = await carregarEstados();
            estado.replaceChildren(new Option(rotuloEstado, ''), ...estados.map((e) => new Option(e.nome, e.sigla)));
            resetarCidade();
        },
        async definir(uf, nomeCidade) {
            const existe = Array.from(estado.options).some((o) => o.value === uf);
            estado.value = existe ? uf : '';
            await preencherCidades(estado.value, nomeCidade);
        },
        obterEstado() { return estado.value; },
        obterCidade() {
            return cidadeTexto && !cidadeTexto.hidden ? cidadeTexto.value.trim() : cidade.value;
        }
    };
}

// =====================================================================
// FILTROS <-> URL <-> API
// URL da busca: busca.html?categorias=TECNOLOGIA,MARKETING&estado=GO&cidade=Goiânia&precoMin=100&p=2
// (na URL a página começa em 1; na API começa em 0)
// =====================================================================

function filtrosDaUrl(search) {
    const p = new URLSearchParams(search);
    const lista = (nome) => p.getAll(nome).flatMap((v) => v.split(',')).map((v) => v.trim()).filter(Boolean);
    return {
        q: (p.get('q') || '').trim(),
        categorias: lista('categorias'),
        tipos: lista('tipos'),
        estado: (p.get('estado') || '').trim().toUpperCase(),
        cidade: (p.get('cidade') || '').trim(),
        precoMin: (p.get('precoMin') || '').trim(),
        precoMax: (p.get('precoMax') || '').trim(),
        notaMin: (p.get('notaMin') || '').trim(),
        ordem: p.get('ordem') || 'recentes',
        pagina: Math.max(1, parseInt(p.get('p'), 10) || 1)
    };
}

function preencherParametros(p, f) {
    if (f.q) p.set('q', f.q);
    if (f.categorias.length) p.set('categorias', f.categorias.join(','));
    if (f.tipos.length) p.set('tipos', f.tipos.join(','));
    if (f.estado) p.set('estado', f.estado);
    if (f.cidade) p.set('cidade', f.cidade);
    if (f.precoMin !== '') p.set('precoMin', f.precoMin);
    if (f.precoMax !== '') p.set('precoMax', f.precoMax);
    if (f.notaMin !== '') p.set('notaMin', f.notaMin);
}

// Parâmetros da barra de endereço (só o que foge do padrão)
function paramsDaUrl(f) {
    const p = new URLSearchParams();
    preencherParametros(p, f);
    if (f.ordem && f.ordem !== 'recentes') p.set('ordem', f.ordem);
    if (f.pagina > 1) p.set('p', String(f.pagina));
    return p.toString();
}

// Parâmetros da chamada à API
function paramsDaApi(f, tamanho) {
    const p = new URLSearchParams();
    preencherParametros(p, f);
    p.set('ordem', f.ordem || 'recentes');
    p.set('pagina', String(f.pagina - 1));
    p.set('tamanho', String(tamanho));
    return p.toString();
}

function temFiltros(f) {
    return Boolean(f.q || f.categorias.length || f.tipos.length || f.estado || f.cidade || f.precoMin !== '' || f.precoMax !== '' || f.notaMin !== '');
}

// Descarta da URL valores que o back-end não reconhece (evita erro 400 por URL editada à mão)
function sanearFiltros(f, opcoes) {
    const categorias = new Set(opcoes.categorias.map((o) => o.valor));
    const tipos = new Set(opcoes.tipos.map((o) => o.valor));
    f.categorias = f.categorias.filter((v) => categorias.has(v));
    f.tipos = f.tipos.filter((v) => tipos.has(v));
    if (!['recentes', 'melhores', 'nome', 'preco'].includes(f.ordem)) f.ordem = 'recentes';
    if (!UFS_FALLBACK.some((uf) => uf.sigla === f.estado)) f.estado = ''; // só as 27 siglas reais
    if (f.precoMin !== '' && !(Number(f.precoMin) >= 0)) f.precoMin = '';
    if (f.precoMax !== '' && !(Number(f.precoMax) >= 0)) f.precoMax = '';
    // "4.0" vira "4"; só valores que o filtro oferece são aceitos (evita erro 400 por URL editada à mão)
    if (f.notaMin !== '') f.notaMin = String(Number(f.notaMin));
    if (!OPCOES_NOTA.some((o) => o.valor === f.notaMin)) f.notaMin = '';
    return f;
}

// =====================================================================
// CARTÕES DE EMPRESA
// =====================================================================

// Linha da página de busca
function criarCartaoLista(e) {
    const linha = criar('div', 'co-row');
    linha.append(criar('div', 'co-logo', inicial(e.nome)));

    const corpo = criar('div', 'co-body');
    const topo = criar('div', 'co-top');
    const identificacao = document.createElement('div');
    identificacao.append(criarNomeComLink(e), criar('div', 'co-cat', `${e.categoriaRotulo} · ${e.tipoRotulo}`));
    topo.append(identificacao);
    corpo.append(topo);

    if (e.descricao) corpo.append(criar('p', 'co-desc', e.descricao));

    const meta = criar('div', 'co-meta');
    meta.append(criarSeloNota(e), criar('span', 'co-loc', `📍 ${e.cidade}, ${e.estado}`), criar('span', 'co-price', formatarFaixaPreco(e.precoMin, e.precoMax)));
    corpo.append(meta);

    const acoes = criar('div', 'co-actions');
    const ver = criar('a', 'co-btn', 'Ver empresa');
    ver.href = linkPerfil(e);
    const favorito = criar('button', 'co-fav', '♡');
    favorito.type = 'button';
    favorito.setAttribute('aria-label', 'Favoritar');
    acoes.append(ver, favorito);

    linha.append(corpo, acoes);
    return linha;
}

// Cartão da seção "Empresas em destaque" da home
function criarCartaoDestaque(e) {
    const cartao = criar('div', 'co-card');

    const topo = criar('div', 'co-top');
    const id = criar('div', 'co-id');
    const textos = document.createElement('div');
    textos.append(criarNomeComLink(e), criar('div', 'co-cat', `${e.categoriaRotulo} · ${e.tipoRotulo}`));
    id.append(criar('div', 'co-logo', inicial(e.nome)), textos);
    topo.append(id);
    cartao.append(topo);

    if (e.descricao) cartao.append(criar('p', 'co-desc', e.descricao));

    const meta = criar('div', 'co-meta');
    meta.append(criarSeloNota(e), criar('span', 'co-loc', `${e.cidade}, ${e.estado}`), criar('span', 'co-price', formatarFaixaPreco(e.precoMin, e.precoMax)));
    cartao.append(meta);
    const conhecer = criar('a', 'co-btn', 'Conhecer empresa');
    conhecer.href = linkPerfil(e);
    cartao.append(conhecer);
    return cartao;
}

// Cartão da seção "Descubra novos negócios" da home
function criarCartaoNovo(e) {
    const cartao = criar('div', 'new-card');
    if (ehNovo(e.criadoEm)) cartao.append(criar('span', 'new-badge', 'Novo'));
    cartao.append(criarNomeComLink(e), criar('div', 'co-cat', e.categoriaRotulo));
    if (e.descricao) cartao.append(criar('p', 'co-desc', e.descricao));
    const conhecer = criar('a', 'new-link', 'Conhecer empresa →');
    conhecer.href = linkPerfil(e);
    cartao.append(conhecer);
    return cartao;
}

// =====================================================================
// PÁGINA INICIAL: totais por categoria e empresas recentes (tudo do banco)
// =====================================================================

async function iniciarHome() {
    const resumoHome = document.getElementById('resumoHome');
    if (!resumoHome) return;

    const [resumo, recentes] = await Promise.allSettled([
        getJson(`${API_URL}/empresas/resumo`),
        getJson(`${API_URL}/empresas?tamanho=7`)
    ]);

    // ---- totais ----
    if (resumo.status === 'fulfilled') {
        const { total, porCategoria } = resumo.value;

        document.querySelectorAll('.cat-tile[data-categoria]').forEach((tile) => {
            const n = porCategoria[tile.dataset.categoria] || 0;
            tile.querySelector('.cat-count').textContent = pluralizar(n, 'empresa', 'empresas');
        });

        if (total === 0) {
            resumoHome.textContent = 'Ainda não há empresas cadastradas. Seja a primeira!';
        } else {
            resumoHome.replaceChildren(criar('b', null, total.toLocaleString('pt-BR')), document.createTextNode(total === 1 ? ' empresa na plataforma' : ' empresas na plataforma'));
            const nomes = {};
            document.querySelectorAll('.cat-tile[data-categoria]').forEach((t) => { nomes[t.dataset.categoria] = t.querySelector('.cat-name').textContent; });
            Object.entries(porCategoria)
                .filter(([, n]) => n > 0)
                .sort((a, b) => b[1] - a[1])
                .slice(0, 2)
                .forEach(([categoria, n]) => {
                    resumoHome.append(document.createTextNode(' · '), criar('b', null, n.toLocaleString('pt-BR')), document.createTextNode(` em ${nomes[categoria] || categoria}`));
                });
        }
    }

    // ---- empresas mais recentes ----
    if (recentes.status === 'fulfilled') {
        const empresas = recentes.value.conteudo;

        // cartões decorativos do topo
        const cartoesHero = document.querySelectorAll('.index-card');
        cartoesHero.forEach((cartao, i) => {
            const e = empresas[i];
            if (!e) { cartao.hidden = true; return; }
            cartao.querySelector('.co-name').textContent = e.nome;
            cartao.querySelector('.co-cat').textContent = `${e.categoriaRotulo} · ${e.cidade}, ${e.estado}`;
            cartao.href = linkPerfil(e);
            cartao.hidden = false;
        });

        const destaques = empresas.slice(0, 3);
        const novos = empresas.slice(3, 7);

        if (destaques.length) {
            document.getElementById('gridDestaques').replaceChildren(...destaques.map(criarCartaoDestaque));
            document.getElementById('destaque').hidden = false;
        }
        if (novos.length) {
            document.getElementById('gridNovos').replaceChildren(...novos.map(criarCartaoNovo));
            document.getElementById('novos').hidden = false;
        }
    }
}

// =====================================================================
// PÁGINA DE BUSCA (barra lateral + resultados paginados)
// =====================================================================

async function iniciarBusca() {
    const lista = document.getElementById('listaEmpresas');
    if (!lista) return;

    const elContagem = document.getElementById('contagemResultados');
    const elPaginacao = document.getElementById('paginacao');
    const elCategorias = document.getElementById('filtroCategorias');
    const elTipos = document.getElementById('filtroTipos');
    const elPrecoMin = document.getElementById('filtroPrecoMin');
    const elPrecoMax = document.getElementById('filtroPrecoMax');
    const elErroPreco = document.getElementById('erroPreco');
    const elNota = document.getElementById('filtroNota');
    const elDicaNota = document.getElementById('dicaNota');
    const elOrdenar = document.getElementById('ordenar');
    const elNotaOrdem = document.getElementById('notaOrdem');
    const campoBusca = document.querySelector('.search-bar input[type="text"]');
    const formBusca = document.querySelector('.search-bar');

    let f = filtrosDaUrl(location.search);
    let queryAtual = location.search;   // a URL pode não aceitar replaceState (arquivo aberto direto do disco)
    let requisicao = 0;                 // numera as consultas, para ignorar respostas atrasadas
    let localidadePronta = false;

    document.getElementById('btnFiltrarMobile').addEventListener('click', () => {
        location.href = 'filtros.html' + queryAtual;
    });

    // ---- opções vindas do back-end ----
    let opcoes;
    try {
        opcoes = await carregarOpcoes();
    } catch (erro) {
        elContagem.textContent = '';
        mostrarMensagem(lista, 'Não foi possível carregar a página', mensagemDoErro(erro), true);
        return;
    }
    sanearFiltros(f, opcoes);

    // ---- montar a barra lateral ----
    function montarChecks(container, itens, selecionados) {
        container.replaceChildren();
        itens.forEach((item) => {
            const rotulo = criar('label', 'check-row');
            const caixa = document.createElement('input');
            caixa.type = 'checkbox';
            caixa.value = item.valor;
            caixa.checked = selecionados.includes(item.valor);
            rotulo.append(caixa, document.createTextNode(' ' + item.rotulo));
            container.append(rotulo);
        });
        container.addEventListener('change', aplicar);
    }
    const lerChecks = (container) => Array.from(container.querySelectorAll('input:checked')).map((i) => i.value);

    montarChecks(elCategorias, opcoes.categorias, f.categorias);
    montarChecks(elTipos, opcoes.tipos, f.tipos);
    // grupo "Avaliação": uma escolha só (Qualquer nota, 4,5 ou mais, 4 ou mais...)
    [{ valor: '', rotulo: 'Qualquer nota' }, ...OPCOES_NOTA].forEach((opcao) => {
        const rotulo = criar('label', 'check-row');
        const radio = document.createElement('input');
        radio.type = 'radio';
        radio.name = 'notaMin';
        radio.value = opcao.valor;
        radio.checked = opcao.valor === f.notaMin;
        rotulo.append(radio);
        if (opcao.valor) rotulo.append(criarEstrelas(Number(opcao.valor), `${opcao.rotulo.replace(' ou mais', '')} estrelas ou mais`));
        rotulo.append(document.createTextNode(' ' + opcao.rotulo));
        elNota.append(rotulo);
    });
    elNota.addEventListener('change', aplicar);
    elDicaNota.hidden = f.notaMin === '';

    elPrecoMin.value = f.precoMin;
    elPrecoMax.value = f.precoMax;
    elOrdenar.value = f.ordem;
    if (campoBusca) campoBusca.value = f.q;

    const seletor = criarSeletorLocalidade({
        estado: document.getElementById('filtroEstado'),
        cidade: document.getElementById('filtroCidade'),
        cidadeTexto: document.getElementById('filtroCidadeTexto'),
        rotuloEstado: 'Todos os estados',
        rotuloCidade: 'Todas as cidades',
        aoMudar: () => aplicar()
    });

    // ---- ler a tela / sincronizar ----
    function lerFiltrosDaTela() {
        f.categorias = lerChecks(elCategorias);
        f.tipos = lerChecks(elTipos);
        if (localidadePronta) {
            f.estado = seletor.obterEstado();
            f.cidade = seletor.obterCidade();
        }
        f.precoMin = elPrecoMin.value.trim();
        f.precoMax = elPrecoMax.value.trim();
        const marcada = elNota.querySelector('input:checked');
        f.notaMin = marcada ? marcada.value : '';
        elDicaNota.hidden = f.notaMin === '';
        f.ordem = elOrdenar.value;
    }

    function precoValido() {
        const invalido = f.precoMin !== '' && f.precoMax !== '' && Number(f.precoMin) > Number(f.precoMax);
        elErroPreco.hidden = !invalido;
        return !invalido;
    }

    function sincronizarUrl() {
        const qs = paramsDaUrl(f);
        queryAtual = qs ? '?' + qs : '';
        try {
            history.replaceState(null, '', location.pathname + queryAtual);
        } catch (_) { /* arquivo aberto direto do disco: o navegador não deixa mexer na URL */ }
    }

    function aplicar() {
        lerFiltrosDaTela();
        f.pagina = 1;
        if (!precoValido()) return;
        sincronizarUrl();
        buscar();
    }
    const aplicarComAtraso = atrasar(aplicar, 400);

    elPrecoMin.addEventListener('input', aplicarComAtraso);
    elPrecoMax.addEventListener('input', aplicarComAtraso);
    elOrdenar.addEventListener('change', aplicar);

    if (formBusca) {
        formBusca.addEventListener('submit', (e) => {
            e.preventDefault();
            lerFiltrosDaTela();
            f.q = campoBusca.value.trim();
            f.pagina = 1;
            if (!precoValido()) return;
            sincronizarUrl();
            buscar();
        });
    }

    async function limparTudo() {
        f = { ...f, q: '', categorias: [], tipos: [], estado: '', cidade: '', precoMin: '', precoMax: '', notaMin: '', pagina: 1 };
        elNota.querySelectorAll('input').forEach((i) => { i.checked = i.value === ''; });
        elDicaNota.hidden = true;
        elCategorias.querySelectorAll('input').forEach((i) => { i.checked = false; });
        elTipos.querySelectorAll('input').forEach((i) => { i.checked = false; });
        elPrecoMin.value = '';
        elPrecoMax.value = '';
        elErroPreco.hidden = true;
        if (campoBusca) campoBusca.value = '';
        if (localidadePronta) await seletor.definir('', '');
        sincronizarUrl();
        buscar();
    }
    document.getElementById('limparFiltros').addEventListener('click', limparTudo);

    // favoritar (só visual, por enquanto)
    lista.addEventListener('click', (e) => {
        const botao = e.target.closest('.co-fav');
        if (!botao) return;
        const favorito = botao.classList.toggle('favorited');
        botao.textContent = favorito ? '♥' : '♡';
        botao.style.color = favorito ? '#F2A93B' : '';
        botao.style.borderColor = favorito ? '#F2A93B' : '';
    });

    // ---- consulta e desenho ----
    async function buscar() {
        const id = ++requisicao;
        lista.classList.add('carregando');
        elNotaOrdem.hidden = f.ordem !== 'melhores'; // explica o critério de "Mais bem avaliadas"
        try {
            const dados = await getJson(`${API_URL}/empresas?${paramsDaApi(f, TAMANHO_PAGINA)}`);
            if (id !== requisicao) return;

            // página pedida na URL além do fim: volta para a última
            if (dados.conteudo.length === 0 && dados.totalElementos > 0 && f.pagina > dados.totalPaginas) {
                f.pagina = dados.totalPaginas;
                sincronizarUrl();
                return buscar();
            }
            desenhar(dados);
        } catch (erro) {
            if (id !== requisicao) return;
            elContagem.textContent = '';
            elPaginacao.replaceChildren();
            mostrarMensagem(lista, 'Não foi possível carregar as empresas', mensagemDoErro(erro), true);
        } finally {
            if (id === requisicao) lista.classList.remove('carregando');
        }
    }

    function desenhar(dados) {
        const total = dados.totalElementos;
        elContagem.replaceChildren(criar('b', null, total.toLocaleString('pt-BR')), document.createTextNode(total === 1 ? ' empresa encontrada' : ' empresas encontradas'));

        if (dados.conteudo.length === 0) {
            if (temFiltros(f)) {
                const limpar = criar('button', null, 'Limpar filtros');
                limpar.type = 'button';
                limpar.addEventListener('click', limparTudo);
                mostrarMensagem(lista, 'Nenhuma empresa encontrada', 'Tente remover algum filtro ou buscar por outro termo.', false, limpar);
            } else {
                const cadastrar = criar('a', null, 'Cadastrar minha empresa');
                cadastrar.href = 'cadastro.html';
                mostrarMensagem(lista, 'Ainda não há empresas cadastradas', 'Seja a primeira a aparecer por aqui.', false, cadastrar);
            }
            elPaginacao.replaceChildren();
            return;
        }

        lista.replaceChildren(...dados.conteudo.map(criarCartaoLista));
        desenharPaginacao(f.pagina, dados.totalPaginas);
    }

    function numerosDePagina(atual, total) {
        if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1);
        const conjunto = new Set([1, total, atual - 1, atual, atual + 1]);
        const numeros = Array.from(conjunto).filter((n) => n >= 1 && n <= total).sort((a, b) => a - b);
        const saida = [];
        numeros.forEach((n, i) => {
            if (i > 0 && n - numeros[i - 1] > 1) saida.push('…');
            saida.push(n);
        });
        return saida;
    }

    function desenharPaginacao(atual, total) {
        elPaginacao.replaceChildren();
        if (total <= 1) return;

        const botao = (texto, pagina, ativo, desativado, rotuloAcessivel) => {
            const b = criar('button', 'page-btn' + (ativo ? ' active' : ''), texto);
            b.type = 'button';
            b.disabled = Boolean(desativado);
            if (rotuloAcessivel) b.setAttribute('aria-label', rotuloAcessivel);
            if (ativo) b.setAttribute('aria-current', 'page');
            b.addEventListener('click', () => {
                f.pagina = pagina;
                sincronizarUrl();
                window.scrollTo({ top: 0, behavior: 'smooth' });
                buscar();
            });
            return b;
        };

        elPaginacao.append(botao('←', atual - 1, false, atual === 1, 'Página anterior'));
        numerosDePagina(atual, total).forEach((n) => {
            elPaginacao.append(n === '…' ? criar('span', 'page-gap', '…') : botao(String(n), n, n === atual, false));
        });
        elPaginacao.append(botao('→', atual + 1, false, atual === total, 'Próxima página'));
    }

    // ---- começa: busca já com os filtros da URL; a localidade carrega em paralelo ----
    buscar();
    await seletor.iniciar();
    await seletor.definir(f.estado, f.cidade);
    localidadePronta = true;
}

// =====================================================================
// TELA DE FILTROS (versão celular)
// =====================================================================

async function iniciarFiltrosMobile() {
    const chipsCategorias = document.getElementById('chipsCategorias');
    if (!chipsCategorias) return;

    const chipsTipos = document.getElementById('chipsTipos');
    const listaOrdem = document.getElementById('listaOrdem');
    const elPrecoMin = document.getElementById('filtroPrecoMin');
    const elPrecoMax = document.getElementById('filtroPrecoMax');
    const elErroPreco = document.getElementById('erroPreco');
    const chipsNota = document.getElementById('chipsNota');
    const elDicaNota = document.getElementById('dicaNota');
    const btnVer = document.getElementById('btnVer');
    const original = location.search; // filtros que a página de busca já tinha

    document.getElementById('btnFechar').href = 'busca.html' + original;
    document.getElementById('btnCancelar').href = 'busca.html' + original;
    btnVer.href = 'busca.html' + original;

    let f = filtrosDaUrl(original);
    let requisicao = 0;
    let localidadePronta = false;

    let opcoes;
    try {
        opcoes = await carregarOpcoes();
    } catch (erro) {
        chipsCategorias.replaceChildren(criar('p', 'filter-error', mensagemDoErro(erro)));
        return;
    }
    sanearFiltros(f, opcoes);

    function montarChips(container, itens, selecionados) {
        container.replaceChildren();
        itens.forEach((item) => {
            const chip = criar('button', 'chip-option' + (selecionados.includes(item.valor) ? ' active' : ''), item.rotulo);
            chip.type = 'button';
            chip.dataset.valor = item.valor;
            chip.addEventListener('click', () => {
                chip.classList.toggle('active');
                atualizar();
            });
            container.append(chip);
        });
    }
    const lerChips = (container) => Array.from(container.querySelectorAll('.chip-option.active')).map((c) => c.dataset.valor);

    montarChips(chipsCategorias, opcoes.categorias, f.categorias);
    montarChips(chipsTipos, opcoes.tipos, f.tipos);
    // Avaliação: chips de escolha única; tocar no que já está ativo desmarca
    OPCOES_NOTA.forEach((opcao) => {
        const chip = criar('button', 'chip-option' + (opcao.valor === f.notaMin ? ' active' : ''), '★ ' + opcao.curto);
        chip.type = 'button';
        chip.dataset.valor = opcao.valor;
        chip.setAttribute('aria-label', opcao.rotulo + ' estrelas');
        chip.addEventListener('click', () => {
            const ativar = !chip.classList.contains('active');
            chipsNota.querySelectorAll('.chip-option').forEach((c) => c.classList.remove('active'));
            if (ativar) chip.classList.add('active');
            atualizar();
        });
        chipsNota.append(chip);
    });
    elDicaNota.hidden = f.notaMin === '';

    elPrecoMin.value = f.precoMin;
    elPrecoMax.value = f.precoMax;

    function marcarOrdem(valor) {
        listaOrdem.querySelectorAll('.radio-row').forEach((linha) => {
            linha.classList.toggle('selected', linha.dataset.valor === valor);
        });
    }
    marcarOrdem(f.ordem);
    listaOrdem.addEventListener('click', (e) => {
        const linha = e.target.closest('.radio-row');
        if (!linha) return;
        marcarOrdem(linha.dataset.valor);
        atualizar();
    });

    const seletor = criarSeletorLocalidade({
        estado: document.getElementById('filtroEstado'),
        cidade: document.getElementById('filtroCidade'),
        cidadeTexto: document.getElementById('filtroCidadeTexto'),
        rotuloEstado: 'Todos os estados',
        rotuloCidade: 'Todas as cidades',
        aoMudar: () => atualizar()
    });

    function lerFiltrosDaTela() {
        f.categorias = lerChips(chipsCategorias);
        f.tipos = lerChips(chipsTipos);
        if (localidadePronta) {
            f.estado = seletor.obterEstado();
            f.cidade = seletor.obterCidade();
        }
        f.precoMin = elPrecoMin.value.trim();
        f.precoMax = elPrecoMax.value.trim();
        const notaAtiva = chipsNota.querySelector('.chip-option.active');
        f.notaMin = notaAtiva ? notaAtiva.dataset.valor : '';
        elDicaNota.hidden = f.notaMin === '';
        const marcada = listaOrdem.querySelector('.radio-row.selected');
        f.ordem = marcada ? marcada.dataset.valor : 'recentes';
        f.pagina = 1;
    }

    function precoValido() {
        const invalido = f.precoMin !== '' && f.precoMax !== '' && Number(f.precoMin) > Number(f.precoMax);
        elErroPreco.hidden = !invalido;
        return !invalido;
    }

    // Atualiza o link do botão e o número de empresas ("Ver 12 empresas")
    const contar = atrasar(async () => {
        const id = ++requisicao;
        try {
            const dados = await getJson(`${API_URL}/empresas?${paramsDaApi(f, 1)}`);
            if (id !== requisicao) return;
            btnVer.textContent = 'Ver ' + pluralizar(dados.totalElementos, 'empresa', 'empresas');
        } catch (_) {
            if (id === requisicao) btnVer.textContent = 'Ver empresas';
        }
    }, 250);

    function atualizar() {
        lerFiltrosDaTela();
        if (!precoValido()) return;
        const qs = paramsDaUrl(f);
        btnVer.href = 'busca.html' + (qs ? '?' + qs : '');
        contar();
    }

    elPrecoMin.addEventListener('input', atualizar);
    elPrecoMax.addEventListener('input', atualizar);
    btnVer.addEventListener('click', (e) => { if (!precoValido()) e.preventDefault(); });

    document.getElementById('btnLimpar').addEventListener('click', async () => {
        chipsCategorias.querySelectorAll('.chip-option').forEach((c) => c.classList.remove('active'));
        chipsTipos.querySelectorAll('.chip-option').forEach((c) => c.classList.remove('active'));
        chipsNota.querySelectorAll('.chip-option').forEach((c) => c.classList.remove('active'));
        elPrecoMin.value = '';
        elPrecoMax.value = '';
        marcarOrdem('recentes');
        if (localidadePronta) await seletor.definir('', '');
        atualizar();
    });

    contar();
    await seletor.iniciar();
    await seletor.definir(f.estado, f.cidade);
    localidadePronta = true;
}

// =====================================================================
// CADASTRO: campos extras da empresa (categoria, tipo, local, preço)
// =====================================================================

let seletorCadastro = null;

async function iniciarCadastroEmpresa() {
    const campos = document.getElementById('empresaFields');
    if (!campos) return;

    seletorCadastro = criarSeletorLocalidade({
        estado: document.getElementById('estado'),
        cidade: document.getElementById('cidade'),
        cidadeTexto: document.getElementById('cidadeTexto'),
        rotuloEstado: 'Selecione o estado',
        rotuloCidade: 'Selecione a cidade'
    });

    const telefone = document.getElementById('telefone');
    if (telefone) {
        telefone.addEventListener('input', () => { telefone.value = mascararTelefone(telefone.value); });
    }

    const [opcoes] = await Promise.allSettled([carregarOpcoes(), seletorCadastro.iniciar()]);

    if (opcoes.status === 'fulfilled') {
        const categoria = document.getElementById('categoria');
        const tipo = document.getElementById('tipoEmpresa');
        opcoes.value.categorias.forEach((o) => categoria.append(new Option(o.rotulo, o.valor)));
        opcoes.value.tipos.forEach((o) => tipo.append(new Option(o.rotulo, o.valor)));
    } else {
        document.getElementById('avisoOpcoes').hidden = false;
    }
}

// =====================================================================
// PÁGINA DE PERFIL DA EMPRESA (perfil.html?id=7)
// =====================================================================

function montarPerfil(e) {
    const pagina = document.createDocumentFragment();

    // ---- topo: logo, nome, categoria, tipo e local ----
    const topo = criar('div', 'hero-card');
    topo.append(criar('div', 'p-logo', inicial(e.nome)));
    const cabecalho = criar('div', 'p-head');
    const tags = criar('div', 'p-tags');
    tags.append(criar('span', 'tag', e.categoriaRotulo), criar('span', 'tag tipo', e.tipoRotulo));
    cabecalho.append(criar('h1', 'p-nome', e.nome), tags, criar('p', 'p-local', `📍 ${e.cidade}, ${e.estado}`), criar('div', 'p-nota'));
    topo.append(cabecalho);

    // ---- coluna da esquerda: sobre + informações ----
    const esquerda = criar('div', 'p-col');

    const sobre = criar('section', 'card');
    sobre.append(
        criar('h2', null, 'Sobre a empresa'),
        e.descricao ? criar('p', 'sobre-texto', e.descricao) : criar('p', 'vazio', 'Esta empresa ainda não adicionou uma descrição.')
    );

    const informacoes = criar('section', 'card');
    const lista = criar('dl', 'info-lista');
    [
        ['Categoria', e.categoriaRotulo],
        ['Tipo de empresa', e.tipoRotulo],
        ['Localização', `${e.cidade}, ${e.estado}`],
        ['No NexSite desde', formatarMesAno(e.criadoEm)]
    ].forEach(([rotulo, valor]) => {
        if (!valor) return;
        const item = criar('div', 'info');
        item.append(criar('dt', null, rotulo), criar('dd', null, valor));
        lista.append(item);
    });
    informacoes.append(criar('h2', null, 'Informações'), lista);
    esquerda.append(sobre, informacoes);

    // ---- coluna da direita: preço e contato ----
    const contato = criar('aside', 'card contato');
    contato.append(criar('h2', null, 'Faixa de preço'), criar('div', 'preco-valor', formatarFaixaPreco(e.precoMin, e.precoMax)));

    const acoes = criar('div', 'acoes');
    const site = e.site ? urlSegura(e.site) : null;
    if (site) {
        const botao = criar('a', 'acao site', 'Visitar ' + site.hostname.replace(/^www\./, ''));
        botao.href = site.href;
        botao.target = '_blank';
        botao.rel = 'noopener noreferrer nofollow';
        botao.title = site.href;
        acoes.append(botao);
    }
    if (/^\d{10,11}$/.test(e.telefone || '')) {
        const celular = e.telefone.length === 11 && e.telefone[2] === '9';
        if (celular) {
            const whats = criar('a', 'acao whats', 'Chamar no WhatsApp');
            whats.href = `https://wa.me/55${e.telefone}?text=${encodeURIComponent('Olá! Encontrei a sua empresa no NexSite.')}`;
            whats.target = '_blank';
            whats.rel = 'noopener noreferrer';
            acoes.append(whats);
        }
        const ligar = criar('a', 'acao ligar', 'Ligar · ' + formatarTelefone(e.telefone));
        ligar.href = `tel:+55${e.telefone}`;
        acoes.append(ligar);
    }
    if (acoes.children.length) {
        contato.append(acoes);
    } else {
        const vazio = criar('p', 'vazio', 'Esta empresa ainda não informou site nem telefone.');
        vazio.style.marginTop = '18px';
        contato.append(vazio);
    }
    contato.append(criar('p', 'contato-nota', 'Preço e contatos foram informados pela própria empresa. Confirme os detalhes antes de fechar negócio.'));

    const grade = criar('div', 'p-grid');
    grade.append(esquerda, contato);
    pagina.append(topo, grade);
    return pagina;
}

// Linha com a nota no topo do perfil: "★★★★☆ 4,3 · 12 avaliações" (leva para a seção de avaliações)
function atualizarNotaDoTopo(media, total) {
    const area = document.querySelector('.p-nota');
    if (!area) return;
    if (!total) {
        area.replaceChildren(criar('span', 'sem', 'Ainda sem avaliações'));
        return;
    }
    const link = criar('a', 'p-nota-link');
    link.href = '#avaliacoes';
    link.append(
        criarEstrelas(media, `Nota média ${formatarNota(media)} de 5`),
        criar('b', null, formatarNota(media)),
        document.createTextNode(' · ' + pluralizar(total, 'avaliação', 'avaliações'))
    );
    area.replaceChildren(link);
}

const ROTULOS_NOTA = { 1: 'Ruim', 2: 'Regular', 3: 'Bom', 4: 'Muito bom', 5: 'Excelente' };

function criarItemAvaliacao(av) {
    const item = criar('article', 'av-item');
    const cabecalho = criar('div', 'av-cab');
    const info = criar('div', 'av-info');
    info.append(criar('div', 'av-nome', av.autor), criar('div', 'av-data', formatarData(av.criadoEm) + (av.editada ? ' · editada' : '')));
    cabecalho.append(criar('div', 'av-avatar', inicial(av.autor)), info, criarEstrelas(av.nota, `Nota ${av.nota} de 5`));
    item.append(cabecalho);
    if (av.comentario) item.append(criar('p', 'av-texto', av.comentario));
    return item;
}

/**
 * Seção "Avaliações" do perfil:
 *  - média e total (públicos);
 *  - formulário para quem está logado como usuário (nota 1 a 5 + análise opcional; dá para editar e excluir);
 *  - lista de 5 em 5, com o botão "Mostrar mais avaliações".
 */
function iniciarAvaliacoes(empresa) {
    const secao = document.getElementById('avaliacoes');
    if (!secao) return;

    const TAMANHO = 5;
    const base = `/empresas/${empresa.id}/avaliacoes`;
    const estado = { proxima: 0, vistos: new Set(), carregando: false, minha: null };

    const titulo = criar('h2', null, 'Avaliações');
    const resumo = criar('div', 'av-resumo');
    const areaForm = criar('div', 'av-form');
    const lista = criar('div', 'av-lista');
    const aviso = criar('p', 'av-aviso');
    aviso.hidden = true;
    const botaoMais = criar('button', 'av-mais', 'Mostrar mais avaliações');
    botaoMais.type = 'button';
    botaoMais.hidden = true;
    secao.replaceChildren(titulo, resumo, areaForm, lista, aviso, botaoMais);
    secao.hidden = false;

    // ---------- média ----------
    function desenharResumo(media, total) {
        if (!total) {
            resumo.replaceChildren(criar('p', 'vazio', 'Ainda não há avaliações para esta empresa.'));
            return;
        }
        const bloco = criar('div', 'av-media-bloco');
        bloco.append(criar('div', 'av-media', formatarNota(media)));
        const direita = criar('div', 'av-media-info');
        direita.append(criarEstrelas(media, `Nota média ${formatarNota(media)} de 5`), criar('div', 'av-total', pluralizar(total, 'avaliação', 'avaliações')));
        bloco.append(direita);
        resumo.replaceChildren(bloco);
    }

    // ---------- lista paginada ----------
    async function carregar(recomecar) {
        if (estado.carregando) return;
        estado.carregando = true;
        if (recomecar) {
            estado.proxima = 0;
            estado.vistos.clear();
        }
        botaoMais.disabled = true;
        aviso.hidden = true;
        try {
            const dados = await getJson(`${API_URL}${base}?pagina=${estado.proxima}&tamanho=${TAMANHO}`);
            if (recomecar) lista.replaceChildren();

            desenharResumo(dados.notaMedia, dados.total);
            atualizarNotaDoTopo(dados.notaMedia, dados.total);

            dados.conteudo.forEach((av) => {
                if (estado.vistos.has(av.id)) return; // alguém avaliou no meio do caminho e deslocou a página
                estado.vistos.add(av.id);
                lista.append(criarItemAvaliacao(av));
            });
            estado.proxima = dados.pagina + 1;

            const faltam = Math.max(0, dados.total - estado.vistos.size);
            botaoMais.hidden = estado.proxima >= dados.totalPaginas;
            botaoMais.textContent = faltam > 0 ? `Mostrar mais avaliações (${faltam})` : 'Mostrar mais avaliações';
        } catch (erro) {
            aviso.textContent = mensagemDoErro(erro);
            aviso.hidden = false;
            botaoMais.hidden = lista.children.length === 0 ? true : botaoMais.hidden;
        } finally {
            estado.carregando = false;
            botaoMais.disabled = false;
        }
    }
    botaoMais.addEventListener('click', () => carregar(false));

    // ---------- formulário ----------
    function desenharFormularioDeslogado(mensagem) {
        const caixa = criar('div', 'av-convite');
        caixa.append(criar('p', null, mensagem || 'Entre na sua conta para avaliar esta empresa.'));
        const acoes = criar('div', 'av-convite-acoes');
        const entrar = criar('a', 'av-btn primario', 'Entrar para avaliar');
        entrar.href = 'login.html?retorno=' + encodeURIComponent('perfil.html?id=' + empresa.id);
        const cadastrar = criar('a', 'av-btn', 'Criar conta');
        cadastrar.href = 'cadastro.html';
        acoes.append(entrar, cadastrar);
        caixa.append(acoes);
        areaForm.replaceChildren(caixa);
    }

    function desenharFormulario(mensagem, mensagemErro) {
        const minha = estado.minha;
        let nota = minha ? minha.nota : 0;

        const caixa = criar('form', 'av-caixa');
        caixa.noValidate = true;
        caixa.append(criar('h3', null, minha ? 'Sua avaliação' : 'Avalie esta empresa'));

        // estrelas clicáveis (radiogroup acessível)
        const grupo = criar('div', 'nota-input');
        grupo.setAttribute('role', 'radiogroup');
        grupo.setAttribute('aria-label', 'Sua nota de 1 a 5');
        const legenda = criar('span', 'nota-legenda');
        const botoes = [1, 2, 3, 4, 5].map((n) => {
            const b = criar('button', 'nota-btn', '★');
            b.type = 'button';
            b.setAttribute('role', 'radio');
            b.setAttribute('aria-label', `${n} ${n === 1 ? 'estrela' : 'estrelas'}: ${ROTULOS_NOTA[n]}`);
            b.addEventListener('click', () => { nota = n; pintar(); });
            b.addEventListener('mouseenter', () => pintar(n));
            return b;
        });
        function pintar(previa) {
            const ate = previa || nota;
            botoes.forEach((b, i) => {
                b.classList.toggle('ativa', i < ate);
                b.setAttribute('aria-checked', String(i + 1 === nota));
            });
            legenda.textContent = ate ? `${ate} de 5 · ${ROTULOS_NOTA[ate]}` : 'Toque nas estrelas para dar a sua nota';
        }
        grupo.addEventListener('mouseleave', () => pintar());
        grupo.append(...botoes, legenda);
        pintar();

        // análise opcional
        const campo = criar('textarea', 'av-texto-campo');
        campo.maxLength = 1000;
        campo.rows = 4;
        campo.placeholder = 'Conte como foi usar os serviços ou produtos desta empresa (opcional)';
        campo.value = minha && minha.comentario ? minha.comentario : '';
        const contador = criar('div', 'av-contador', `${campo.value.length}/1000`);
        campo.addEventListener('input', () => { contador.textContent = `${campo.value.length}/1000`; });

        const status = criar('p', 'av-status');
        status.setAttribute('role', 'status');
        status.setAttribute('aria-live', 'polite');
        if (mensagem) status.textContent = mensagem;
        if (mensagemErro) { status.textContent = mensagemErro; status.classList.add('erro'); }

        const acoes = criar('div', 'av-acoes');
        const enviar = criar('button', 'av-btn primario', minha ? 'Atualizar avaliação' : 'Enviar avaliação');
        enviar.type = 'submit';
        acoes.append(enviar);
        let excluir = null;
        if (minha) {
            excluir = criar('button', 'av-btn perigo', 'Excluir minha avaliação');
            excluir.type = 'button';
            acoes.append(excluir);
        }

        caixa.append(grupo, campo, contador, acoes, status);
        areaForm.replaceChildren(caixa);

        function falhou(erro) {
            if (erro instanceof ErroApi && erro.status === 401) {
                desenharFormularioDeslogado('Sua sessão expirou. Entre novamente para avaliar.');
                return;
            }
            status.textContent = mensagemDoErro(erro);
            status.classList.add('erro');
            enviar.disabled = false;
            if (excluir) excluir.disabled = false;
        }

        caixa.addEventListener('submit', async (evento) => {
            evento.preventDefault();
            status.classList.remove('erro');
            if (!nota) {
                status.textContent = 'Escolha uma nota de 1 a 5.';
                status.classList.add('erro');
                return;
            }
            enviar.disabled = true;
            if (excluir) excluir.disabled = true;
            status.textContent = 'Salvando…';
            try {
                estado.minha = await chamarApiAutenticada(`${base}/minha`, { method: 'PUT', corpo: { nota, comentario: campo.value.trim() || null } });
                desenharFormulario('Avaliação salva! Obrigado por ajudar outras pessoas.');
                carregar(true);
            } catch (erro) {
                falhou(erro);
            }
        });

        if (excluir) {
            excluir.addEventListener('click', async () => {
                if (!window.confirm('Excluir a sua avaliação desta empresa?')) return;
                enviar.disabled = true;
                excluir.disabled = true;
                try {
                    await chamarApiAutenticada(`${base}/minha`, { method: 'DELETE' });
                    estado.minha = null;
                    desenharFormulario('Avaliação excluída.');
                    carregar(true);
                } catch (erro) {
                    falhou(erro);
                }
            });
        }
    }

    async function prepararFormulario() {
        const usuario = obterUsuarioLogado();
        if (!usuario || !usuario.token) {
            desenharFormularioDeslogado();
            return;
        }
        if (usuario.tipoPerfil !== 'USUARIO') {
            areaForm.replaceChildren(criar('p', 'vazio', 'Contas de empresa não podem avaliar outras empresas.'));
            return;
        }
        areaForm.replaceChildren(criar('p', 'vazio', 'Carregando…'));
        try {
            estado.minha = await chamarApiAutenticada(`${base}/minha`);
            desenharFormulario();
        } catch (erro) {
            if (erro instanceof ErroApi && erro.status === 401) {
                desenharFormularioDeslogado('Sua sessão expirou. Entre novamente para avaliar.');
            } else {
                estado.minha = null;
                desenharFormulario(null, mensagemDoErro(erro));
            }
        }
    }

    desenharResumo(empresa.notaMedia, empresa.totalAvaliacoes);
    prepararFormulario();
    carregar(true);
}

async function iniciarPerfil() {
    const raiz = document.getElementById('perfilConteudo');
    if (!raiz) return;

    // "Voltar" desfaz a navegação (mantém os filtros da busca); se a página foi aberta direto, vai para a busca
    document.getElementById('btnVoltar').addEventListener('click', (evento) => {
        if (history.length > 1) {
            evento.preventDefault();
            history.back();
        }
    });

    const linkBusca = () => {
        const link = criar('a', null, 'Ver todas as empresas');
        link.href = 'busca.html';
        return link;
    };

    const id = new URLSearchParams(location.search).get('id');
    if (!/^\d{1,18}$/.test(id || '')) {
        mostrarMensagem(raiz, 'Empresa não encontrada', 'O endereço desta página parece estar incompleto.', false, linkBusca());
        document.title = 'Empresa não encontrada | NexSite';
        return;
    }

    let empresa;
    try {
        empresa = await getJson(`${API_URL}/empresas/${id}`);
    } catch (erro) {
        if (erro instanceof ErroApi && erro.status === 404) {
            mostrarMensagem(raiz, 'Empresa não encontrada', 'Ela pode ter sido removida ou o endereço está incorreto.', false, linkBusca());
            document.title = 'Empresa não encontrada | NexSite';
        } else {
            mostrarMensagem(raiz, 'Não foi possível carregar a empresa', mensagemDoErro(erro), true);
        }
        return;
    }

    document.title = `${empresa.nome} | NexSite`;
    raiz.replaceChildren(montarPerfil(empresa));
    atualizarNotaDoTopo(empresa.notaMedia, empresa.totalAvaliacoes);
    iniciarAvaliacoes(empresa);

    // ---- outras empresas da mesma categoria (sem repetir esta) ----
    try {
        const resposta = await getJson(`${API_URL}/empresas?categorias=${encodeURIComponent(empresa.categoria)}&tamanho=5`);
        const outras = resposta.conteudo.filter((e) => e.id !== empresa.id).slice(0, 3);
        if (outras.length) {
            document.getElementById('tituloSimilares').textContent = `Outras empresas em ${empresa.categoriaRotulo}`;
            document.getElementById('gridSimilares').replaceChildren(...outras.map(criarCartaoDestaque));
            document.getElementById('similares').hidden = false;
        }
    } catch (_) { /* sem sugestões: o perfil continua completo */ }
}

// =====================================================================
// LOGIN: quantas empresas há na plataforma (dado real)
// =====================================================================

async function iniciarContagemLogin() {
    const elemento = document.getElementById('contagemPlataforma');
    if (!elemento) return;
    try {
        const { total } = await getJson(`${API_URL}/empresas/resumo`);
        if (total > 0) {
            elemento.textContent = total === 1 ? '1 empresa já está na plataforma' : `${total.toLocaleString('pt-BR')} empresas já estão na plataforma`;
            elemento.hidden = false;
        }
    } catch (_) { /* sem API: simplesmente não mostra a frase */ }
}

// =====================================================================
// SESSÃO: usuário logado, cabeçalho e chamadas que exigem login
// O login devolve um token. Ele fica guardado junto do usuário no navegador e vai no
// cabeçalho "Authorization: Bearer ..." dos pedidos que exigem login (ex.: avaliar uma empresa).
// O servidor guarda só o hash do token e pode encerrar a sessão (logout).
// =====================================================================

function obterUsuarioLogado() {
    try {
        const bruto = localStorage.getItem(CHAVE_USUARIO) || sessionStorage.getItem(CHAVE_USUARIO);
        if (!bruto) return null;
        const usuario = JSON.parse(bruto);
        return usuario && typeof usuario.nome === 'string' && usuario.nome.trim() ? usuario : null;
    } catch (_) {
        return null; // storage bloqueado ou conteúdo corrompido: trata como deslogado
    }
}

function limparLogin() {
    try {
        localStorage.removeItem(CHAVE_USUARIO);
        sessionStorage.removeItem(CHAVE_USUARIO);
    } catch (_) { /* sem acesso ao storage: nada a limpar */ }
}

async function sair() {
    const usuario = obterUsuarioLogado();
    if (usuario && usuario.token) {
        try {
            // Avisa o servidor para apagar a sessão (espera no máximo 1,5 s: sair nunca pode travar)
            await Promise.race([
                fetch(`${API_URL}/auth/logout`, { method: 'POST', headers: { Authorization: 'Bearer ' + usuario.token } }),
                new Promise((resolve) => setTimeout(resolve, 1500))
            ]);
        } catch (_) { /* sem conexão: mesmo assim sai deste navegador */ }
    }
    limparLogin();
    window.location.reload(); // recarrega a página e os botões "Entrar" / "Criar conta" voltam
}

// Monta o cabeçalho conforme o estado de login (só a parte "header-actions")
function renderizarCabecalho() {
    const area = document.querySelector('.header-actions');
    if (!area) return;
    const usuario = obterUsuarioLogado();

    if (usuario) {
        const caixa = criar('div', 'user-box');
        const nome = criar('span', 'user-name', usuario.nome); // textContent: o nome nunca é interpretado como HTML
        nome.title = usuario.nome;
        const botaoSair = criar('button', 'btn btn-ghost', 'Sair');
        botaoSair.type = 'button';
        botaoSair.addEventListener('click', sair);
        caixa.append(nome, botaoSair);
        area.replaceChildren(caixa);
    } else {
        const entrar = criar('a', 'btn btn-ghost', 'Entrar');
        entrar.href = 'login.html';
        const criarConta = criar('a', 'btn btn-primary', 'Criar conta');
        criarConta.href = 'cadastro.html';
        area.replaceChildren(entrar, criarConta);
    }
}

// Chamada que exige login. 204 -> null. Sessão vencida (401) limpa o login e atualiza o cabeçalho.
async function chamarApiAutenticada(caminho, { method = 'GET', corpo } = {}) {
    const usuario = obterUsuarioLogado();
    if (!usuario || !usuario.token) throw new ErroApi('Faça login para continuar.', 401);

    const opcoes = { method, headers: { Authorization: 'Bearer ' + usuario.token } };
    if (corpo !== undefined) {
        opcoes.headers['Content-Type'] = 'application/json';
        opcoes.body = JSON.stringify(corpo);
    }

    const resposta = await fetch(`${API_URL}${caminho}`, opcoes);
    if (resposta.status === 204) return null;
    if (!resposta.ok) {
        let mensagem = 'Não foi possível concluir a ação.';
        try {
            const dados = await resposta.json();
            if (dados && dados.erro) mensagem = dados.erro;
        } catch (_) { /* resposta sem JSON: mantém a mensagem padrão */ }
        if (resposta.status === 401) {
            limparLogin();
            renderizarCabecalho();
        }
        throw new ErroApi(mensagem, resposta.status);
    }
    return resposta.json();
}

// =====================================================================
// INÍCIO
// =====================================================================

document.addEventListener('DOMContentLoaded', () => {

    // 1. Passa o texto digitado na busca da index para o campo da página de busca
    const urlParams = new URLSearchParams(window.location.search);
    const query = urlParams.get('q');
    const searchInput = document.querySelector('.search-bar input[type="text"]');
    if (query && searchInput) {
        searchInput.value = query;
    }

    // 2. Módulos de cada página (só rodam se a página tiver os elementos deles)
    iniciarHome();
    iniciarBusca();
    iniciarFiltrosMobile();
    iniciarCadastroEmpresa();
    iniciarContagemLogin();
    iniciarPerfil();

    // 2.5 Cabeçalho: nome do usuário e "Sair" no lugar de "Entrar" / "Criar conta"
    renderizarCabecalho();

    // =========================================================
    // 3. LOGIN REAL (valida e-mail e senha no back-end)
    // =========================================================
    const loginForm = document.querySelector('.form-panel form');
    if (loginForm) {
        loginForm.addEventListener('submit', async (e) => {
            e.preventDefault();

            const email = document.getElementById('email').value.trim();
            const senha = document.getElementById('senha').value;
            const lembrar = loginForm.querySelector('input[type="checkbox"]').checked;

            if (!email || !senha) {
                alert('Informe o e-mail e a senha.');
                return;
            }

            try {
                const resposta = await fetch(`${API_URL}/auth/login`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ email: email, senha: senha })
                });

                if (resposta.ok) {
                    const usuario = await resposta.json();
                    // Guarda só os dados públicos (para mostrar o nome na tela). Nunca a senha.
                    // "Lembrar de mim" mantém o login após fechar o navegador.
                    localStorage.removeItem(CHAVE_USUARIO);
                    sessionStorage.removeItem(CHAVE_USUARIO);
                    const storage = lembrar ? localStorage : sessionStorage;
                    storage.setItem(CHAVE_USUARIO, JSON.stringify(usuario));
                    // Veio de "Entrar para avaliar" no perfil de uma empresa? Volta para lá. Só aceita esse formato (evita redirecionar para sites de fora).
                    const retorno = new URLSearchParams(window.location.search).get('retorno') || '';
                    window.location.assign(/^perfil\.html\?id=\d{1,18}$/.test(retorno) ? retorno : 'index.html');
                } else {
                    let mensagem = 'Não foi possível entrar. Verifique os dados.';
                    try {
                        const resultado = await resposta.json();
                        if (resultado.erro) mensagem = resultado.erro;
                    } catch (_) { /* resposta sem JSON: mantém a mensagem padrão */ }
                    alert(mensagem);
                }
            } catch (erro) {
                console.error('Falha na comunicação:', erro);
                alert('Erro ao conectar com a API. Verifique se o back-end está rodando na porta 8080.');
            }
        });
    }

    // =========================================================
    // 4. INTEGRAÇÃO REAL DO CADASTRO COM O BACK-END (JAVA)
    // =========================================================
    const cadastroForm = document.querySelector('[data-step="2"] form');
    if (cadastroForm) {
        cadastroForm.addEventListener('submit', async (e) => {
            e.preventDefault();

            const nome = document.getElementById('nome').value;
            const email = document.getElementById('email').value;
            const senha = document.getElementById('senha').value;
            const confirmar = document.getElementById('confirmar').value;
            const cnpj = document.getElementById('cnpj').value.trim();
            // Valor fixo ("USUARIO" ou "EMPRESA"), não o texto exibido na tela
            const tipoPerfil = document.getElementById('typeLabel').dataset.tipo;
            const isEmpresa = tipoPerfil === 'EMPRESA';

            if (senha !== confirmar) {
                alert('As senhas não coincidem!');
                return;
            }

            const dados = { nome: nome, email: email, senha: senha, tipoPerfil: tipoPerfil };

            if (isEmpresa) {
                const categoria = document.getElementById('categoria').value;
                const tipoEmpresa = document.getElementById('tipoEmpresa').value;
                const estado = document.getElementById('estado').value;
                const cidade = seletorCadastro ? seletorCadastro.obterCidade() : '';
                const precoMin = parseFloat(document.getElementById('precoMin').value);
                const precoMax = parseFloat(document.getElementById('precoMax').value);
                const descricao = document.getElementById('descricao').value.trim();
                const site = document.getElementById('site').value.trim();
                const telefone = document.getElementById('telefone').value.trim();

                if (!cnpj) { alert('Informe o CNPJ da empresa.'); return; }
                if (!categoria) { alert('Escolha a categoria da empresa.'); return; }
                if (!tipoEmpresa) { alert('Informe se a empresa é prestadora de serviços, loja ou site.'); return; }
                if (!estado || !cidade) { alert('Informe o estado e a cidade da empresa.'); return; }
                if (Number.isNaN(precoMin) || Number.isNaN(precoMax)) { alert('Informe a faixa de preço (mínimo e máximo).'); return; }
                if (precoMin < 0 || precoMax < 0) { alert('A faixa de preço não pode ser negativa.'); return; }
                if (precoMax < precoMin) { alert('O preço máximo não pode ser menor que o mínimo.'); return; }

                Object.assign(dados, {
                    cnpj: cnpj,
                    categoria: categoria,
                    tipoEmpresa: tipoEmpresa,
                    estado: estado,
                    cidade: cidade,
                    precoMin: precoMin,
                    precoMax: precoMax,
                    descricao: descricao || null,
                    site: site || null,         // opcional: o back-end confere o formato
                    telefone: telefone || null  // opcional
                });
            }

            try {
                const resposta = await fetch(`${API_URL}/auth/cadastro`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(dados)
                });

                if (resposta.ok) {
                    alert('Conta criada com sucesso! Faça login para continuar.');
                    window.location.assign('login.html');
                } else {
                    let mensagem = 'Erro ao processar o cadastro. Verifique os dados.';
                    try {
                        const resultado = await resposta.json();
                        if (resultado.erro) mensagem = resultado.erro;
                    } catch (_) { /* resposta sem JSON: mantém a mensagem padrão */ }
                    alert('Aviso: ' + mensagem);
                }

            } catch (erro) {
                console.error('Falha na comunicação:', erro);
                alert('Erro ao conectar com a API. Verifique se o projeto no NetBeans está rodando na porta 8080.');
            }
        });
    }
});
