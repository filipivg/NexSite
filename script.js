document.addEventListener('DOMContentLoaded', () => {
    // 1. Lógica de Busca (Passar o texto da index para a página de busca)
    const urlParams = new URLSearchParams(window.location.search);
    const query = urlParams.get('q');
    
    // Procura o input de busca na página atual
    const searchInput = document.querySelector('.search-bar input[type="text"]');

    if (query && searchInput) {
        searchInput.value = query; // Preenche com o que foi digitado na index
    }

    // 2. Interações na página de Busca (busca.html)
    // Simular o "Favoritar"
    const favButtons = document.querySelectorAll('.co-fav');
    favButtons.forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.preventDefault();
            const isFav = btn.classList.toggle('favorited');
            btn.innerHTML = isFav ? '♥' : '♡';
            btn.style.color = isFav ? '#F2A93B' : ''; // Cor da variável --beacon
            btn.style.borderColor = isFav ? '#F2A93B' : '';
        });
    });

    // Simular a Paginação
    const pageButtons = document.querySelectorAll('.page-btn');
    pageButtons.forEach(btn => {
        if (btn.textContent !== '→') {
            btn.addEventListener('click', () => {
                pageButtons.forEach(p => p.classList.remove('active'));
                btn.classList.add('active');
                window.scrollTo({ top: 0, behavior: 'smooth' }); // Sobe a tela
            });
        }
    });

    // Limpar Filtros
    const clearFiltersBtn = document.querySelector('.clear-filters');
    if (clearFiltersBtn) {
        clearFiltersBtn.addEventListener('click', () => {
            const checkboxes = document.querySelectorAll('.filters input[type="checkbox"]');
            const textInputs = document.querySelectorAll('.filters input[type="text"]');
            checkboxes.forEach(cb => cb.checked = false);
            textInputs.forEach(ti => ti.value = '');
        });
    }

    // 3. Simulação de Formulários (Login e Cadastro)
    const loginForm = document.querySelector('.form-panel form');
    if (loginForm) {
        loginForm.addEventListener('submit', (e) => {
            e.preventDefault();
            alert('Login realizado com sucesso! Redirecionando...');
            window.location.href = 'index.html'; // Volta para a home logado
        });
    }

    // A página de cadastro possui [data-step="2"] para o formulário final
    const cadastroForm = document.querySelector('[data-step="2"] form');
    if (cadastroForm) {
        cadastroForm.addEventListener('submit', (e) => {
            alert('Conta criada com sucesso! Redirecionando...');
            window.location.href = 'index.html';
        });
    }
});