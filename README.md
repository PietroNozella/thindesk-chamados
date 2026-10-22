# ThinDesk — Chamados técnicos

Backend da atividade prática: abertura, acompanhamento e resolução de chamados
com Java 21, Spring Boot 3.4.0, Spring Security e MongoDB. Não há interface gráfica.
Os pacotes `com.pfc` e `com.pfc.thindesk` e a organização da base foram preservados.

## Executar e preparar o Postman

Pré-requisitos: Docker Desktop em execução, Docker Compose e PowerShell.
O Docker usa Java 21; não depende do JDK instalado no computador.

```powershell
.\scripts\preparar-ambiente.ps1
docker compose up --build -d
docker compose ps
```

O script cria `.env` com senhas aleatórias somente se esse arquivo ainda não existir.
Se já existir, mantém suas configurações e exige que as duas senhas estejam preenchidas.
Também gera `postman/ThinDesk.local.postman_environment.json` com as credenciais locais.
Para isso, lê a configuração resolvida pelo Compose, usando a mesma interpretação
de aspas e variáveis aplicada ao contêiner.
Esses dois arquivos são ignorados pelo Git e pelo contexto de build do Docker.

No Postman, importe:

1. `postman/ThinDesk.postman_collection.json`.
2. `postman/ThinDesk.local.postman_environment.json`.

Selecione o ambiente **ThinDesk - Local** e execute as requisições na ordem numerada.
A abertura salva `chamadoId` automaticamente. A identificação do solicitante salva
`usuarioId`, usado na listagem. A coleção inclui os cenários de erro.
O modelo público `ThinDesk.postman_environment.json` contém senhas vazias e pode
ser preenchido manualmente. Também é possível importar a coleção no Insomnia,
ajustando as variáveis e a autenticação conforme a ferramenta.

- API: `http://localhost:8082`.
- MongoDB no computador: `mongodb://localhost:27020/thindesk_chamados`.
- MongoDB dentro do Compose: `mongodb://mongo:27017/thindesk_chamados`.
- Banco padrão: `thindesk_chamados`.
- Volume: `thindesk-chamados_mongo-data`.

Para acompanhar a inicialização: `docker compose logs --tail 50 app`.
Ao iniciar pela primeira vez, aguarde a mensagem `Started ThindeskApplication`.

## Autenticação e permissões

A autenticação reutiliza o Spring Security da base, adaptado para HTTP Basic.
As credenciais são enviadas em cada requisição, sem sessão, formulário ou cookie.
CSRF fica desabilitado para este backend usado por Postman/Insomnia, sem cliente de navegador.

| Perfil | Configuração | Permissões |
| --- | --- | --- |
| Solicitante | `THINDESK_USERNAME` e `THINDESK_PASSWORD` | Abrir e consultar seus chamados. |
| Técnico | `THINDESK_TECNICO_USERNAME` e `THINDESK_TECNICO_PASSWORD` | Consultar chamados e registrar resolução. |

Os dois usuários são mantidos em memória pelo Spring Security, com senhas
codificadas com BCrypt. Os chamados ficam no MongoDB. Não foi criado um cadastro
de usuários, pois ele não faz parte da atividade.

O **nome de login é o identificador estável do solicitante**: o campo `usuario`
do chamado e o `{id}` de `/usuario/{id}` usam esse valor. Mantenha o mesmo login
ao reiniciar para continuar acessando os chamados anteriores.
Os nomes precisam ser diferentes, com até 64 caracteres: letras sem acentos,
números, ponto, hífen ou sublinhado; o primeiro deve ser letra ou número.
As senhas não podem ser vazias e devem ter até 72 bytes em UTF-8.

## Endpoints

| Método | Rota | Perfil | Sucesso |
| --- | --- | --- | --- |
| GET | `/api/autenticacao` | Autenticado | `200`, login e perfil atuais. |
| POST | `/api/chamados` | Solicitante | `201`, chamado e cabeçalho `Location`. |
| GET | `/api/chamados/usuario/{id}` | Próprio solicitante ou técnico | `200`, lista por data de abertura decrescente. |
| GET | `/api/chamados/{id}` | Próprio solicitante ou técnico | `200`, detalhes do chamado. |
| PUT | `/api/chamados/{id}/resolver` | Técnico | `200`, chamado resolvido. |

Corpo da abertura:

```json
{
  "titulo": "Impressora sem conexão",
  "descricao": "A impressora do setor não aparece na rede."
}
```

Corpo da resolução:

```json
{
  "solucao": "Configuração de rede corrigida e impressão de teste realizada."
}
```

Título: até 150 caracteres. Descrição e solução: até 5.000 caracteres cada.
Todos são obrigatórios e não podem conter apenas espaços. Espaços nas extremidades
são removidos antes da gravação.

O servidor gera ID, solicitante, status `ABERTO` e data de abertura.
Na resolução, registra solução, técnico autenticado, status `RESOLVIDO` e data de resolução.
As datas são instantes UTC, com precisão de milissegundos.
Campos não previstos no corpo, incluindo ID, usuário, status e datas, são rejeitados.

A resolução é uma atualização atômica condicionada ao status `ABERTO` no MongoDB.
Somente a primeira tentativa pode gravar. Não existe endpoint de edição genérica
ou reabertura. As rotas legadas de clientes, a listagem geral de chamados e
`PUT /api/chamados/{id}` permanecem bloqueadas.

| Erro | Situação |
| --- | --- |
| `400` | Dados inválidos, JSON inválido ou campos não permitidos. |
| `401` | Credenciais ausentes ou inválidas. |
| `403` | Perfil sem permissão, consulta de outro solicitante ou rota bloqueada. |
| `404` | Chamado não encontrado nas operações de consulta ou resolução. |
| `409` | Chamado fora do status `ABERTO`, incluindo tentativa de resolver novamente. |

Esses erros retornam JSON com `status` e `mensagem`. Uma listagem sem resultados
retorna `200` e `[]`. O técnico também pode listar um login sem chamados.

## Persistência

Para inspecionar os documentos sem instalar um cliente MongoDB:

```powershell
docker compose exec mongo mongosh thindesk_chamados --quiet --eval 'db.chamados.find().toArray()'
```

Também é possível usar MongoDB Compass com a URI local indicada acima.
Para encerrar e iniciar os serviços mantendo os dados:

```powershell
docker compose down
docker compose up -d
```

`down` mantém o volume. A opção `-v` remove o volume e seus dados.

## Testes

O comando abaixo cria um MongoDB separado, executa a suíte com Java 21 e encerra
os contêineres de testes ao terminar. Não usa o `.env` da aplicação nem publica
portas do banco de testes:

```powershell
.\scripts\testar-integracao.ps1
```

Os relatórios ficam em `target/surefire-reports`. Os bancos de testes têm nomes
aleatórios com prefixos próprios e são removidos ao final. A suíte cobre o fluxo
HTTP real, persistência, isolamento entre solicitantes, permissões, validações,
bloqueio de alterações e duas resoluções simultâneas.

Com **JDK 21** e Maven Wrapper, `.\mvnw.cmd clean verify` executa os testes sem
MongoDB, pulando os sete testes de integração. Para incluí-los, use `MONGO_E2E=true`
e `MONGO_E2E_URI` apontando para uma instância exclusiva de testes.
A execução local da aplicação exige as variáveis de `.env.example` no processo;
o Maven não carrega `.env` automaticamente. Nesse caso, altere `MONGODB_URI` para
`mongodb://localhost:27020/thindesk_chamados` e execute `.\mvnw.cmd spring-boot:run`.

Para executar os testes da coleção com Node.js, sem adicionar dependências ao projeto:

```powershell
npx.cmd --yes newman run postman/ThinDesk.postman_collection.json -e postman/ThinDesk.local.postman_environment.json --reporters cli,junit --reporter-junit-export target/newman.xml
```

O comando usa o [Newman oficial do Postman](https://github.com/postmanlabs/newman).

## Verificações executadas

- `scripts/testar-integracao.ps1`: 26 testes, sem falhas, erros ou testes ignorados.
- Build e execução da aplicação com Docker Compose e Java 21.
- Coleção executada com Newman: 17 requisições e 25 verificações aprovadas.
- Consulta direta aos documentos no MongoDB.
- Recriação dos contêineres da aplicação e do MongoDB, preservando o volume;
  conferência dos campos do chamado resolvido antes e depois.
- Revisão do backend quanto a permissões, sobrescrita e concorrência, sem bloqueios encontrados.

## Branches e histórico

Cada etapa tem uma branch e um commit próprio. As branches foram encadeadas
localmente para permitir a execução completa sem fazer merge na `main`:

| Branch | Base da comparação | Escopo |
| --- | --- | --- |
| `chore/preparacao-backend` | `main` | Execução independente e ambiente. |
| `feat/autenticacao` | `chore/preparacao-backend` | HTTP Basic e perfis. |
| `feat/abertura-chamados` | `feat/autenticacao` | Abertura validada e dados automáticos. |
| `feat/consulta-chamados` | `feat/abertura-chamados` | Consulta e isolamento por solicitante. |
| `feat/resolucao-chamados` | `feat/consulta-chamados` | Resolução atômica e bloqueio. |
| `test/fluxo-integrado-chamados` | `feat/resolucao-chamados` | E2E com MongoDB e comando de testes. |
| `docs/demonstracao-api` | `test/fluxo-integrado-chamados` | Documentação da API, coleção e preparação local. |

A branch `docs/demonstracao-api` reúne a versão completa. Exemplo para conferir
somente a tarefa de consulta: `git diff feat/abertura-chamados..feat/consulta-chamados`.
Os PRs devem comparar cada etapa com sua base correspondente, ou entrar em
sequência na `main`.

A `main` conserva a base de aula importada do `thindesk.zip`, com WAR, Java 23 e
telas originais. A preparação mudou para Java 21/JAR e removeu as telas.
O SHA-256 do ZIP registrado na importação é:

```text
CECBA3087577848DF5A975217F42AEE4821DB30B61E3D0DA326F90AB3E2E73C7
```
