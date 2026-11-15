import assert from 'node:assert/strict';
import { randomUUID } from 'node:crypto';

const baseUrl = process.env.THINDESK_TEST_URL;
assert.ok(baseUrl, 'Defina THINDESK_TEST_URL para uma instância local com banco de testes.');
assert.ok(['localhost', '127.0.0.1'].includes(new URL(baseUrl).hostname), 'Use somente uma instância local de testes.');
const senha = process.env.THINDESK_TEST_PASSWORD;
assert.ok(senha, 'Defina THINDESK_TEST_PASSWORD com a senha do administrador de testes.');
const identificador = randomUUID().slice(0, 8);
let verificacoes = 0;

function verificar(descricao, validacao) {
    validacao();
    verificacoes++;
    console.log(`OK: ${descricao}`);
}

function csrf(html) {
    for (const input of html.matchAll(/<input\b[^>]*>/g)) {
        const atributos = Object.fromEntries([...input[0].matchAll(/([\w-]+)="([^"]*)"/g)]
            .map((atributo) => [atributo[1], atributo[2]]));
        if (atributos.name === '_csrf') return atributos.value;
    }
    throw new Error('Token CSRF não encontrado na página.');
}

function sessao() {
    const cookies = new Map();
    return async (caminho, campos, opcoes = {}) => {
        const headers = { Cookie: [...cookies].map(([nome, valor]) => `${nome}=${valor}`).join('; ') };
        let body;
        if (campos !== undefined) {
            headers['Content-Type'] = opcoes.json ? 'application/json' : 'application/x-www-form-urlencoded';
            body = opcoes.json ? JSON.stringify(campos) : new URLSearchParams(campos);
        }
        if (opcoes.token) headers['X-CSRF-TOKEN'] = opcoes.token;
        const resposta = await fetch(new URL(caminho, baseUrl), {
            method: opcoes.method ?? (campos === undefined ? 'GET' : 'POST'),
            headers, body, redirect: 'manual', signal: AbortSignal.timeout(15000),
        });
        for (const cookie of resposta.headers.getSetCookie()) {
            const [nome, ...valor] = cookie.split(';')[0].split('=');
            cookies.set(nome, valor.join('='));
        }
        return { status: resposta.status, local: resposta.headers.get('location'), html: await resposta.text() };
    };
}

function redirecionou(resposta, caminho) {
    assert.equal(resposta.status, 302);
    assert.equal(new URL(resposta.local, baseUrl).pathname, caminho);
}

async function login(requisicao, username, password) {
    const tela = await requisicao('/login');
    const resposta = await requisicao('/login', { username, password, _csrf: csrf(tela.html) });
    redirecionou(resposta, '/');
    const inicio = await requisicao('/');
    if (inicio.local?.endsWith('/aceite')) {
        const aceite = await requisicao('/aceite');
        redirecionou(await requisicao('/aceite', { _csrf: csrf(aceite.html) }), '/');
    }
}

const admin = sessao();
const anonimo = await admin('/chamados');
verificar('chamados exigem login', () => redirecionou(anonimo, '/login'));
await login(admin, process.env.THINDESK_TEST_ADMIN ?? 'admin', senha);
const dashboard = await admin('/');
verificar('login, aceite e dashboard renderizam', () => {
    assert.equal(dashboard.status, 200);
    assert.match(dashboard.html, /<h1>Dashboard<\/h1>/);
});

const formulario = await admin('/chamados/novo');
assert.equal(formulario.status, 200);
const token = csrf(formulario.html);
const dados = { descricao: `Teste de chamado ${identificador}`, status: 'ABERTO', tipo: 'Suporte', tecnico: 'Técnico de teste', usuario: 'Solicitante de teste' };
const semCsrf = await admin('/chamados/novo', dados);
verificar('criação sem CSRF é rejeitada', () => redirecionou(semCsrf, '/403'));

const invalido = await admin('/chamados/novo', { ...dados, descricao: '   ', status: 'INVALIDO', _csrf: token });
verificar('formulário inválido mostra erros e mantém os campos', () => {
    assert.equal(invalido.status, 200);
    assert.match(invalido.html, /Informe a descrição/);
    assert.match(invalido.html, /Selecione um status válido/);
    assert.match(invalido.html, /Solicitante de teste/);
});

redirecionou(await admin('/chamados/novo', { ...dados, _csrf: token }), '/chamados');
let registros = JSON.parse((await admin('/api/chamados')).html);
const criado = registros.find((item) => item.descricao === dados.descricao);
verificar('chamado criado permanece na consulta ao banco', () => assert.ok(criado?.id));

const edicao = await admin(`/chamados/atualizar/${criado.id}`);
verificar('edição carrega o status atual', () => {
    assert.equal(edicao.status, 200);
    assert.match(edicao.html, /value="ABERTO" selected="selected"/);
});
const dadosEditados = { ...dados, descricao: `${dados.descricao} atualizado`, status: 'EM_ANDAMENTO', revisao: criado.revisao };
const edicaoInvalida = await admin(`/chamados/atualizar/${criado.id}`, { ...dadosEditados, descricao: '', _csrf: csrf(edicao.html) });
verificar('edição inválida preserva a rota do chamado', () => {
    assert.equal(edicaoInvalida.status, 200);
    assert.ok(edicaoInvalida.html.includes(`action="/chamados/atualizar/${criado.id}"`));
});
redirecionou(await admin(`/chamados/atualizar/${criado.id}`, { ...dadosEditados, _csrf: csrf(edicao.html) }), '/chamados');
registros = JSON.parse((await admin('/api/chamados')).html);
verificar('edição persiste descrição e status', () => {
    const atualizado = registros.find((item) => item.id === criado.id);
    assert.equal(atualizado.descricao, dadosEditados.descricao);
    assert.equal(atualizado.status, 'EM_ANDAMENTO');
});

redirecionou(await admin('/chamados/novo', { ...dados, descricao: `Tentativa de ID ${identificador}`, id: criado.id, _csrf: token }), '/chamados');
const apiCriacao = await admin('/api/chamados', { ...dados, descricao: `API ${identificador}`, id: criado.id }, { json: true, token });
verificar('IDs enviados na criação MVC e API não sobrescrevem registros', () => {
    assert.equal(apiCriacao.status, 200);
    assert.notEqual(JSON.parse(apiCriacao.html).id, criado.id);
});
registros = JSON.parse((await admin('/api/chamados')).html);
assert.equal(registros.find((item) => item.id === criado.id).descricao, dadosEditados.descricao);
assert.ok(registros.some((item) => item.descricao === `Tentativa de ID ${identificador}` && item.id !== criado.id));
const apiInvalida = await admin('/api/chamados', { ...dados, descricao: '' }, { json: true, token });
verificar('API rejeita descrição vazia', () => assert.equal(apiInvalida.status, 400));

const getCancelar = await admin(`/chamados/cancelar/${criado.id}`);
verificar('cancelamento não aceita GET', () => assert.equal(getCancelar.status, 405));
redirecionou(await admin(`/chamados/cancelar/${criado.id}`, { revisao: 1, _csrf: token }), '/chamados');
registros = JSON.parse((await admin('/api/chamados')).html);
const cancelado = registros.find((item) => item.id === criado.id);
verificar('cancelamento preserva o registro e os outros campos', () => {
    assert.equal(cancelado.status, 'CANCELADO');
    for (const campo of ['descricao', 'tipo', 'tecnico', 'usuario']) assert.equal(cancelado[campo], dadosEditados[campo]);
});
const inexistente = await admin('/chamados/atualizar/000000000000000000000000');
verificar('chamado inexistente responde 404', () => assert.equal(inexistente.status, 404));
const cancelarInexistente = await admin('/chamados/cancelar/000000000000000000000000', { revisao: 0, _csrf: token });
verificar('cancelamento de chamado inexistente responde 404', () => assert.equal(cancelarInexistente.status, 404));

const sessoesPerfis = {};
const usuariosTeste = {};
for (const perfil of ['PROFESSOR', 'OPERADOR']) {
    const cadastro = await admin('/usuarios/novo');
    const username = `teste_${perfil.toLowerCase()}_${identificador}`;
    redirecionou(await admin('/usuarios/novo', {
        nome: 'Teste', sobrenome: perfil, email: `${username}@example.test`, username, senha, perfil, _csrf: csrf(cadastro.html),
    }), '/usuarios/novo');
    const usuario = sessao();
    await login(usuario, username, senha);
    sessoesPerfis[perfil] = usuario;
    usuariosTeste[perfil] = username;
    const lista = await usuario('/chamados');
    verificar(`${perfil} consegue consultar chamados`, () => assert.equal(lista.status, 200));
    const resultado = await usuario('/chamados/novo', { ...dados, descricao: `${perfil} ${identificador}`, _csrf: csrf(lista.html) });
    if (perfil === 'PROFESSOR') {
        const formularioRestrito = await usuario('/chamados/novo');
        verificar('professor não vê ações de escrita e POST direto é bloqueado', () => {
            assert.ok(!lista.html.includes('Novo chamado'));
            redirecionou(resultado, '/403');
            redirecionou(formularioRestrito, '/403');
        });
        const editar = await usuario(`/chamados/atualizar/${criado.id}`, { ...dados, _csrf: csrf(lista.html) });
        const cancelar = await usuario(`/chamados/cancelar/${criado.id}`, { revisao: 0, _csrf: csrf(lista.html) });
        verificar('professor não edita nem cancela por POST direto', () => {
            redirecionou(editar, '/403');
            redirecionou(cancelar, '/403');
        });
    } else {
        verificar('operador consegue criar chamados', () => redirecionou(resultado, '/chamados'));
        const listaOperador = JSON.parse((await usuario('/api/chamados')).html);
        const doOperador = listaOperador.find((item) => item.descricao === `OPERADOR ${identificador}`);
        const editar = await usuario(`/chamados/atualizar/${doOperador.id}`, { ...dados, status: 'CONCLUIDO', revisao: doOperador.revisao, _csrf: csrf(lista.html) });
        const cancelar = await usuario(`/chamados/cancelar/${doOperador.id}`, { revisao: 1, _csrf: csrf(lista.html) });
        verificar('operador consegue editar e cancelar chamados', () => {
            redirecionou(editar, '/chamados');
            redirecionou(cancelar, '/chamados');
        });
    }
}
const rotas = ['/clientes', '/clientes/novo', '/ajustes-horarios', '/usuarios', '/usuarios/novo', '/auditoria', '/minha-senha'];
for (const rota of rotas) {
    const pagina = await admin(rota);
    verificar(`página ${rota} renderiza com navegação`, () => {
        assert.equal(pagina.status, 200);
        assert.ok(pagina.html.includes('Menu principal'));
    });
}

const clienteDados = { nome: `Cliente ${identificador}`, telefone: '(11) 99999-0000', setor: 'Secretaria' };
const clienteToken = csrf((await admin('/clientes/novo')).html);
const clienteInvalido = await admin('/clientes/novo', { ...clienteDados, nome: ' ', _csrf: clienteToken });
verificar('cliente inválido mantém os dados e mostra erro', () => {
    assert.equal(clienteInvalido.status, 200);
    assert.match(clienteInvalido.html, /Informe o nome do cliente/);
    assert.match(clienteInvalido.html, /Secretaria/);
});
redirecionou(await admin('/clientes/novo', { ...clienteDados, _csrf: clienteToken }), '/clientes');
let clientes = JSON.parse((await admin('/api/clientes')).html);
const cliente = clientes.find((item) => item.nome === clienteDados.nome);
verificar('cliente cadastrado aparece no banco', () => assert.ok(cliente?.id));
redirecionou(await admin(`/clientes/editar/${cliente.id}`, { ...clienteDados, setor: 'Laboratório', _csrf: clienteToken }), '/clientes');
clientes = JSON.parse((await admin('/api/clientes')).html);
verificar('edição de cliente persiste', () => assert.equal(clientes.find((item) => item.id === cliente.id).setor, 'Laboratório'));
const excluirGet = await admin(`/clientes/deletar/${cliente.id}`);
const excluirSemToken = await admin(`/clientes/deletar/${cliente.id}`, {});
verificar('exclusão de cliente exige POST e CSRF', () => {
    assert.equal(excluirGet.status, 405);
    redirecionou(excluirSemToken, '/403');
});
redirecionou(await admin(`/clientes/deletar/${cliente.id}`, { _csrf: clienteToken }), '/clientes');
clientes = JSON.parse((await admin('/api/clientes')).html);
verificar('cliente excluído deixa de aparecer na consulta', () => assert.ok(!clientes.some((item) => item.id === cliente.id)));
redirecionou(await admin('/clientes/novo', { ...clienteDados, nome: `Concorrência cliente ${identificador}`, _csrf: clienteToken }), '/clientes');
const clienteConcorrente = JSON.parse((await admin('/api/clientes')).html).find((item) => item.nome === `Concorrência cliente ${identificador}`);
const disputaCliente = await Promise.all([
    admin(`/clientes/editar/${clienteConcorrente.id}`, { ...clienteDados, _csrf: clienteToken }),
    admin(`/clientes/deletar/${clienteConcorrente.id}`, { _csrf: clienteToken }),
]);
const aposDisputaCliente = JSON.parse((await admin('/api/clientes')).html);
verificar('edição concorrente não recria um cliente excluído', () => {
    assert.ok([302, 404].includes(disputaCliente[0].status));
    redirecionou(disputaCliente[1], '/clientes');
    assert.ok(!aposDisputaCliente.some((item) => item.id === clienteConcorrente.id));
});

const horarioDados = { setor: `Setor ${identificador}`, diaSemana: 'Segunda-feira', horarioInicio: '08:00', horarioFim: '18:00' };
const horarioToken = csrf((await admin('/ajustes-horarios')).html);
const horarioInvalido = await admin('/ajustes-horarios/salvar', { ...horarioDados, horarioFim: '07:00', _csrf: horarioToken });
verificar('horário invertido é rejeitado sem perder os dados', () => {
    assert.equal(horarioInvalido.status, 200);
    assert.match(horarioInvalido.html, /posterior ao início/);
    assert.ok(horarioInvalido.html.includes(horarioDados.setor));
});
redirecionou(await admin('/ajustes-horarios/salvar', { ...horarioDados, _csrf: horarioToken }), '/ajustes-horarios');
let horarios = JSON.parse((await admin('/api/ajustes-horarios')).html);
const horario = horarios.find((item) => item.setor === horarioDados.setor);
verificar('horário cadastrado persiste', () => assert.equal(horario?.horarioInicio, '08:00'));
const horarioExcluirGet = await admin(`/ajustes-horarios/deletar/${horario.id}`);
verificar('exclusão de horário não aceita GET', () => assert.equal(horarioExcluirGet.status, 405));
redirecionou(await admin(`/ajustes-horarios/deletar/${horario.id}`, { _csrf: horarioToken }), '/ajustes-horarios');
horarios = JSON.parse((await admin('/api/ajustes-horarios')).html);
verificar('horário excluído deixa de aparecer na consulta', () => assert.ok(!horarios.some((item) => item.id === horario.id)));

const professor = sessoesPerfis.PROFESSOR;
const professorToken = csrf((await professor('/')).html);
const professorCliente = await professor('/clientes/novo', { ...clienteDados, _csrf: professorToken });
const professorHorario = await professor('/ajustes-horarios/salvar', { ...horarioDados, _csrf: professorToken });
verificar('professor não altera clientes nem horários por POST direto', () => {
    redirecionou(professorCliente, '/403');
    redirecionou(professorHorario, '/403');
});

const snapshot = JSON.parse((await admin('/api/chamados')).html).find((item) => item.id === criado.id);
const disputas = await Promise.all(['A', 'B'].map((sufixo) => admin(`/api/chamados/${criado.id}`,
    { ...snapshot, descricao: `Concorrência ${sufixo} ${identificador}` }, { json: true, token, method: 'PUT' })));
const vencedor = JSON.parse(disputas.find((item) => item.status === 200).html);
const persistido = JSON.parse((await admin('/api/chamados')).html).find((item) => item.id === criado.id);
verificar('duas edições da mesma revisão produzem um sucesso e um conflito', () => {
    assert.deepEqual(disputas.map((item) => item.status).sort(), [200, 409]);
    assert.equal(persistido.descricao, vencedor.descricao);
    assert.equal(persistido.revisao, snapshot.revisao + 1);
});
const conflitoForm = await admin(`/chamados/atualizar/${criado.id}`, { ...dados, descricao: 'Texto preservado no conflito', revisao: snapshot.revisao, _csrf: token });
verificar('conflito no formulário preserva o texto e exige recarregamento', () => {
    assert.equal(conflitoForm.status, 409);
    assert.match(conflitoForm.html, /Texto preservado no conflito/);
    assert.match(conflitoForm.html, /Recarregar chamado/);
    assert.match(conflitoForm.html, /disabled="disabled"/);
});
const editarCancelar = await Promise.all([
    admin(`/chamados/atualizar/${criado.id}`, { ...dados, descricao: `Edição concorrente ${identificador}`, revisao: persistido.revisao, _csrf: token }),
    admin(`/chamados/cancelar/${criado.id}`, { revisao: persistido.revisao, _csrf: token }),
]);
verificar('edição e cancelamento concorrentes não sobrescrevem a operação vencedora', () => {
    assert.deepEqual(editarCancelar.map((item) => item.status).sort(), [302, 409]);
});
const semRevisao = await admin(`/api/chamados/${criado.id}`, dados, { json: true, token, method: 'PUT' });
verificar('API exige revisão para atualizar um chamado', () => assert.equal(semRevisao.status, 400));

if (process.env.THINDESK_TEST_LEGACY_ID) {
    const id = process.env.THINDESK_TEST_LEGACY_ID;
    const legado = JSON.parse((await admin('/api/chamados')).html).find((item) => item.id === id);
    const atualizado = await admin(`/api/chamados/${id}`, { ...legado, status: 'CONCLUIDO' }, { json: true, token, method: 'PUT' });
    verificar('chamado legado sem revisão aceita a primeira edição e normaliza o status', () => {
        assert.equal(legado.revisao, 0);
        assert.equal(legado.status, 'ABERTO');
        assert.equal(atualizado.status, 200);
        assert.equal(JSON.parse(atualizado.html).revisao, 1);
    });
}

const cadastroToken = csrf((await admin('/usuarios/novo')).html);
const cadastroInvalido = await admin('/usuarios/novo', {
    nome: ' ', sobrenome: 'Teste', email: 'email-invalido', username: 'usuario inválido', senha: 'curta', perfil: 'OPERADOR', _csrf: cadastroToken,
});
verificar('cadastro de usuário valida os campos no servidor', () => {
    assert.equal(cadastroInvalido.status, 200);
    assert.match(cadastroInvalido.html, /Informe o nome/);
    assert.match(cadastroInvalido.html, /e-mail válido/);
    assert.ok(!cadastroInvalido.html.includes('value="curta"'));
});
const duplicado = await admin('/usuarios/novo', {
    nome: 'Teste', sobrenome: 'Duplicado', email: `outro_${identificador}@example.test`,
    username: usuariosTeste.OPERADOR, senha, perfil: 'OPERADOR', _csrf: cadastroToken,
});
verificar('cadastro duplicado mostra erro controlado', () => {
    assert.equal(duplicado.status, 200);
    assert.match(duplicado.html, /Nome de usuário já existe/);
});

const publica = sessao();
const orientacao = await publica('/esqueci-senha');
const publicoToken = csrf((await publica('/login')).html);
const pedidoAntigo = await publica('/esqueci-senha', { email: 'admin@thindesk.local', _csrf: publicoToken });
const tokenAntigo = await publica('/redefinir-senha/token-legado-validacao');
const consumoAntigo = await publica('/redefinir-senha/token-legado-validacao', { senhaNova: 'SenhaNaoAutorizada123', _csrf: publicoToken });
verificar('recuperação pública orienta o usuário e não gera nem consome tokens', () => {
    assert.equal(orientacao.status, 200);
    assert.ok(!orientacao.html.includes('<form'));
    assert.equal(pedidoAntigo.status, 405);
    assert.equal(tokenAntigo.status, 410);
    assert.equal(consumoAntigo.status, 410);
});

function idUsuario(html, username) {
    const linha = [...html.matchAll(/<tr>[\s\S]*?<\/tr>/g)].find((item) => item[0].includes(`>${username}</td>`));
    assert.ok(linha, `Usuário ${username} não encontrado na lista.`);
    const caminho = linha[0].match(/\/usuarios\/([^/"?]+)\/senha/);
    assert.ok(caminho, 'Link de recuperação administrada não encontrado.');
    return caminho[1];
}

const listaUsuarios = await admin('/usuarios');
const operadorId = idUsuario(listaUsuarios.html, usuariosTeste.OPERADOR);
const resetToken = csrf(listaUsuarios.html);
for (const perfil of ['PROFESSOR', 'OPERADOR']) {
    const usuario = sessoesPerfis[perfil];
    const acesso = await usuario('/usuarios');
    const reset = await usuario(`/usuarios/${operadorId}/senha`, { senhaAtual: senha, senhaNova: 'SenhaNaoAutorizada123', _csrf: csrf((await usuario('/')).html) });
    verificar(`${perfil} não acessa gestão de usuários nem redefine senhas`, () => {
        redirecionou(acesso, '/403');
        redirecionou(reset, '/403');
    });
}

const operadorSegundaSessao = sessao();
await login(operadorSegundaSessao, usuariosTeste.OPERADOR, senha);
const senhaNova = `Nova_${identificador}!`;
const senhaFinal = `Final_${identificador}!`;
const resetErrado = await admin(`/usuarios/${operadorId}/senha`, { senhaAtual: 'senha-incorreta', senhaNova, _csrf: resetToken });
verificar('reset exige a senha atual do administrador e não a devolve no HTML', () => {
    assert.equal(resetErrado.status, 200);
    assert.match(resetErrado.html, /Senha do administrador incorreta/);
    assert.ok(!resetErrado.html.includes(`value="${senhaNova}"`));
});
const resetSemToken = await admin(`/usuarios/${operadorId}/senha`, { senhaAtual: senha, senhaNova });
verificar('reset exige CSRF', () => redirecionou(resetSemToken, '/403'));
const resetCurto = await admin(`/usuarios/${operadorId}/senha`, { senhaAtual: senha, senhaNova: 'curta', _csrf: resetToken });
verificar('reset rejeita senha curta antes de modificar a conta', () => {
    assert.equal(resetCurto.status, 200);
    assert.match(resetCurto.html, /ao menos 8 caracteres/);
});
redirecionou(await admin(`/usuarios/${operadorId}/senha`, { senhaAtual: senha, senhaNova, _csrf: resetToken }), '/usuarios');
const sessaoAntiga = await sessoesPerfis.OPERADOR('/');
const apiSessaoAntiga = await operadorSegundaSessao('/api/chamados');
verificar('reset invalida as duas sessões anteriores, incluindo acesso à API', () => {
    redirecionou(sessaoAntiga, '/login');
    redirecionou(apiSessaoAntiga, '/login');
    assert.ok(sessaoAntiga.local.includes('expirada'));
});
const senhaAntiga = sessao();
const loginAntigo = await senhaAntiga('/login');
const tentativaAntiga = await senhaAntiga('/login', { username: usuariosTeste.OPERADOR, password: senha, _csrf: csrf(loginAntigo.html) });
verificar('senha anterior não autentica depois do reset', () => {
    redirecionou(tentativaAntiga, '/login');
    assert.ok(tentativaAntiga.local.includes('error'));
});
const operadorNovo = sessao();
const operadorOutraSessao = sessao();
await login(operadorNovo, usuariosTeste.OPERADOR, senhaNova);
await login(operadorOutraSessao, usuariosTeste.OPERADOR, senhaNova);
const trocaToken = csrf((await operadorNovo('/minha-senha')).html);
const trocaErrada = await operadorNovo('/minha-senha', { senhaAtual: 'incorreta', senhaNova: senhaFinal, _csrf: trocaToken });
const trocaLonga = await operadorNovo('/minha-senha', { senhaAtual: senhaNova, senhaNova: 'á'.repeat(40), _csrf: trocaToken });
verificar('troca própria valida senha atual e limite do BCrypt sem truncar', () => {
    assert.equal(trocaErrada.status, 200);
    assert.match(trocaErrada.html, /Senha atual incorreta/);
    assert.equal(trocaLonga.status, 200);
    assert.match(trocaLonga.html, /senha é muito longa/);
});
const troca = await operadorNovo('/minha-senha', { senhaAtual: senhaNova, senhaNova: senhaFinal, _csrf: trocaToken });
const outraAposTroca = await operadorOutraSessao('/');
verificar('troca própria encerra a sessão atual e as demais', () => {
    redirecionou(troca, '/login');
    assert.ok(troca.local.includes('redefinida'));
    redirecionou(outraAposTroca, '/login');
});
const acessoFinal = sessao();
await login(acessoFinal, usuariosTeste.OPERADOR, senhaFinal);
const aposNovaSenha = await acessoFinal('/');
verificar('usuário entra com a senha que escolheu após o reset', () => assert.equal(aposNovaSenha.status, 200));
const auditoria = await admin('/auditoria');
verificar('auditoria identifica a conta alvo do reset sem registrar senhas', () => {
    assert.ok(auditoria.html.includes(operadorId));
    assert.match(auditoria.html, /SENHA_REDEFINIDA/);
    assert.ok(!auditoria.html.includes(senhaNova));
    assert.ok(!auditoria.html.includes(senhaFinal));
});

const adminUsername = `teste_admin_${identificador}`;
redirecionou(await admin('/usuarios/novo', {
    nome: 'Administrador', sobrenome: 'Teste', email: `${adminUsername}@example.test`, username: adminUsername,
    senha, perfil: 'ADMINISTRADOR', _csrf: resetToken,
}), '/usuarios/novo');
const segundoAdminId = idUsuario((await admin('/usuarios')).html, adminUsername);
const segundoAdmin = sessao();
await login(segundoAdmin, adminUsername, senha);
const resetProprio = await segundoAdmin(`/usuarios/${segundoAdminId}/senha`, {
    senhaAtual: senha, senhaNova, _csrf: csrf((await segundoAdmin('/')).html),
});
verificar('administrador usa a troca própria em vez de redefinir a própria conta', () => {
    assert.equal(resetProprio.status, 200);
    assert.match(resetProprio.html, /Use a opção Trocar senha/);
});

const falhas = sessao();
let bloqueio;
for (let tentativa = 0; tentativa < 5; tentativa++) {
    const tela = await falhas('/login');
    bloqueio = await falhas('/login', { username: usuariosTeste.PROFESSOR, password: 'senha-errada', _csrf: csrf(tela.html) });
}
verificar('cinco senhas incorretas bloqueiam novas tentativas de login', () => {
    redirecionou(bloqueio, '/login');
    assert.ok(bloqueio.local.includes('bloqueado'));
});
const contaBloqueada = sessao();
const telaBloqueada = await contaBloqueada('/login');
const senhaCertaBloqueada = await contaBloqueada('/login', { username: usuariosTeste.PROFESSOR, password: senha, _csrf: csrf(telaBloqueada.html) });
verificar('conta bloqueada recusa login mesmo com a senha correta', () => assert.ok(senhaCertaBloqueada.local.includes('bloqueado')));
const professorId = idUsuario((await admin('/usuarios')).html, usuariosTeste.PROFESSOR);
redirecionou(await admin(`/usuarios/${professorId}/senha`, { senhaAtual: senha, senhaNova, _csrf: resetToken }), '/usuarios');
const professorRecuperado = sessao();
await login(professorRecuperado, usuariosTeste.PROFESSOR, senhaNova);
const aposDesbloqueio = await professorRecuperado('/');
verificar('reset administrativo remove o bloqueio e permite novo login', () => assert.equal(aposDesbloqueio.status, 200));

if (process.env.THINDESK_TEST_LEGACY_USERNAME) {
    const username = process.env.THINDESK_TEST_LEGACY_USERNAME;
    const usuarioLegado = sessao();
    await login(usuarioLegado, username, senha);
    const lista = await admin('/usuarios');
    const id = idUsuario(lista.html, username);
    redirecionou(await admin(`/usuarios/${id}/senha`, { senhaAtual: senha, senhaNova, _csrf: csrf(lista.html) }), '/usuarios');
    const expirou = await usuarioLegado('/');
    verificar('usuário legado sem marcador de credencial tem a sessão revogada após reset', () => redirecionou(expirou, '/login'));
}

const final = await admin('/');
registros = JSON.parse((await admin('/api/chamados')).html);
verificar('dashboard continua renderizando com chamados persistidos', () => {
    assert.equal(final.status, 200);
    assert.match(final.html, /Visão dos chamados/);
});
verificar('contadores do dashboard correspondem aos registros do banco', () => {
    const valores = [...final.html.matchAll(/class="display-6 mb-0">(\d+)<\/p>/g)].map((item) => Number(item[1]));
    assert.deepEqual(valores, [registros.length, ...['ABERTO', 'EM_ANDAMENTO', 'CONCLUIDO', 'CANCELADO']
        .map((status) => registros.filter((item) => item.status === status).length)]);
});
const logout = await admin('/logout', { _csrf: csrf(final.html) });
const aposLogout = await admin('/chamados');
verificar('logout encerra o acesso', () => {
    redirecionou(logout, '/login');
    redirecionou(aposLogout, '/login');
});
console.log(`${verificacoes} verificações aprovadas. Dados ${identificador} mantidos no banco local de testes.`);
