document.addEventListener('DOMContentLoaded', () => {
    // 1. Lógica de Busca (Passar o texto da index para a página de busca)
    const urlParams = new URLSearchParams(window.location.search);
    const query = urlParams.get('q');
    
    const searchInput = document.querySelector('.search-bar input[type="text"]');
    if (query && searchInput) {
        searchInput.value = query;
    }

    // 2. Interações na página de Busca
    const favButtons = document.querySelectorAll('.co-fav');
    favButtons.forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.preventDefault();
            const isFav = btn.classList.toggle('favorited');
            btn.innerHTML = isFav ? '♥' : '♡';
            btn.style.color = isFav ? '#F2A93B' : ''; 
            btn.style.borderColor = isFav ? '#F2A93B' : '';
        });
    });

    const pageButtons = document.querySelectorAll('.page-btn');
    pageButtons.forEach(btn => {
        if (btn.textContent !== '→') {
            btn.addEventListener('click', () => {
                pageButtons.forEach(p => p.classList.remove('active'));
                btn.classList.add('active');
                window.scrollTo({ top: 0, behavior: 'smooth' }); 
            });
        }
    });

    const clearFiltersBtn = document.querySelector('.clear-filters');
    if (clearFiltersBtn) {
        clearFiltersBtn.addEventListener('click', () => {
            const checkboxes = document.querySelectorAll('.filters input[type="checkbox"]');
            const textInputs = document.querySelectorAll('.filters input[type="text"]');
            checkboxes.forEach(cb => cb.checked = false);
            textInputs.forEach(ti => ti.value = '');
        });
    }

    // 3. Simulação do Login (mantida apenas visualmente para o protótipo)
    const loginForm = document.querySelector('.form-panel form');
    if (loginForm) {
        loginForm.addEventListener('submit', (e) => {
            e.preventDefault();
            alert('Login realizado com sucesso! Redirecionando...');
            window.location.href = 'index_2.html'; 
        });
    }

    // =========================================================
    // 4. INTEGRAÇÃO REAL COM O BACK-END EM JAVA (SPRING BOOT)
    // =========================================================
    const cadastroForm = document.querySelector('[data-step="2"] form');
    if (cadastroForm) {
        cadastroForm.addEventListener('submit', async (e) => {
            e.preventDefault(); // Impede a página de recarregar
            
            // Captura os valores digitados no HTML
            const nome = document.getElementById('nome').value;
            const email = document.getElementById('email').value;
            const senha = document.getElementById('senha').value;
            const confirmar = document.getElementById('confirmar').value;
            const cnpj = document.getElementById('cnpj').value;
            const tipoPerfil = document.getElementById('typeLabel').textContent;

            // Validação básica de senha no front-end
            if (senha !== confirmar) {
                alert('As senhas não coincidem!');
                return;
            }

            // Monta o objeto DTO exatamente como o Java espera
            const dados = {
                nome: nome,
                email: email,
                senha: senha,
                tipoPerfil: tipoPerfil,
                cnpj: cnpj
            };

            try {
                // Envia os dados para a porta 8080 (onde o NetBeans está rodando)
                const resposta = await fetch('http://localhost:8080/api/auth/cadastro', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(dados)
                });

                // Lê a resposta do Java (sucesso ou erro)
                const resultado = await resposta.json();

                if (resposta.ok) {
                    alert('Sucesso: ' + resultado.mensagem);
                    window.location.href = 'index_2.html'; // Redireciona
                } else {
                    // Exibe a mensagem de erro (ex: E-mail duplicado) gerada pela sua Regra de Negócio
                    alert('Aviso: ' + (resultado.erro || 'Erro ao processar o cadastro. Verifique os dados.'));
                }

            } catch (erro) {
                console.error('Falha na comunicação:', erro);
                alert('Erro ao conectar com a API. Verifique se o projeto no NetBeans está rodando na porta 8080.');
            }
        });
    }
});