# ThinDesk — Chamados técnicos

Projeto da atividade de microserviço de chamados técnicos, iniciado diretamente
do `thindesk.zip` fornecido pelo professor. Este repositório tem histórico próprio
e não herda commits de projetos anteriores.

## Base na main

O primeiro commit registra os 37 arquivos de fontes e recursos do ZIP, incluindo
as 16 classes Java. Arquivos gerados em `target/` foram excluídos.

SHA-256 do ZIP recebido:

```text
CECBA3087577848DF5A975217F42AEE4821DB30B61E3D0DA326F90AB3E2E73C7
```

Os fontes Java e o `pom.xml` foram preservados nesse commit. As únicas adaptações
ao material importado são as variáveis de ambiente na configuração do MongoDB e
do usuário de login. Também foram acrescentados este README, `.gitignore`,
`.gitattributes` e as diretrizes em `AGENTS.md`.

A configuração usa `MONGODB_URI`, `MONGODB_DATABASE`, `THINDESK_USERNAME` e
`THINDESK_PASSWORD`. Credenciais do arquivo original não são versionadas.

A main registra a base de aula, com WAR, Java 23 e telas originais. A branch
`chore/preparacao-backend` prepara a execução com Java 21 e JAR, mantém Spring Boot
3.4.0 e retira as telas e os handlers de formulário. O histórico permite comparar
essas adaptações com a base recebida.

## Estado desta branch

Os documentos, repositórios e serviços originais de chamados, clientes e horários
foram mantidos. Em `Cliente`, foram retiradas as anotações JPA antigas, pois sua
persistência é feita pelo MongoDB. Lombok permanece porque já é utilizado pela
classe `HorarioAtendimento` do ZIP.

As rotas legadas estão bloqueadas até a implementação das permissões de cada
feature. GET sem autenticação retorna HTTP 401, sem redirecionamento para tela.
Não há login ativo pela API nesta etapa; a proteção CSRF está habilitada.
Os campos `spring.security.user.*` da base ficam reservados à próxima feature,
com os valores fornecidos por ambiente.

## Executar com Docker

Pré-requisitos: Docker Desktop em execução e Docker Compose.

```powershell
Copy-Item .env.example .env
# Preencha THINDESK_PASSWORD no .env antes de continuar.
docker compose up --build
```

- Servidor: `http://localhost:8082`.
- MongoDB no computador: `mongodb://localhost:27020/thindesk_chamados`.
- MongoDB dentro do Compose: `mongodb://mongo:27017/thindesk_chamados`.
- Banco: `thindesk_chamados`.
- Volume: `thindesk-chamados_mongo-data`.

O banco e o volume são próprios deste projeto. `docker compose down` encerra os
serviços e mantém os dados do volume.

## Executar localmente

Pré-requisitos: JDK 21 e MongoDB acessível. O Maven Wrapper fornece o Maven.
Para iniciar somente o banco do Compose, prepare `.env` e execute
`docker compose up -d mongo`.

```powershell
$env:MONGODB_URI='mongodb://localhost:27020/thindesk_chamados'
$env:MONGODB_DATABASE='thindesk_chamados'
$env:THINDESK_USERNAME='usuario'
$env:THINDESK_PASSWORD='defina-uma-senha-local'
.\mvnw.cmd spring-boot:run
```

A execução local não carrega `.env` automaticamente. Use variáveis de ambiente
do processo. `SERVER_PORT` pode alterar a porta local, cujo padrão é 8082.
O JAR gerado é `target/thindesk-1.0-alpha.jar`.

## Verificações

```powershell
.\mvnw.cmd clean verify
docker compose config --quiet
```

Os testes padrão verificam as restrições HTTP e não exigem MongoDB. Para validar
também a inicialização real e a persistência dos documentos da base:

```powershell
docker run --rm --detach --name thindesk-preparacao-test --publish 127.0.0.1:27030:27017 mongo:7
$env:MONGO_E2E='true'
.\mvnw.cmd clean verify
Remove-Item Env:MONGO_E2E
docker stop thindesk-preparacao-test
```

O teste cria um banco com prefixo `thindesk_preparacao_test_` e identificador
aleatório e remove somente esse banco ao terminar. `MONGO_E2E_URI` permite apontar
para outra instância de testes; o padrão é `mongodb://localhost:27030`.

## Estrutura

`src/main/java/com/pfc/ThindeskApplication.java` mantém o ponto de entrada da aula.
Os pacotes `com.pfc.thindesk.controller`, `entity`, `repository` e `service`
mantêm a organização original. A configuração fica em
`src/main/resources/application.properties`, e os testes em `src/test/java`.

## Desenvolvimento

Cada entrega parte da main atualizada, usa uma branch própria e entra por PR:

- `chore/preparacao-backend`: build, ambiente e preparação para API — etapa atual.
- `feat/autenticacao`: acesso pela API e perfis de solicitante e técnico.
- `feat/abertura-chamados`: abertura com dados e status automáticos.
- `feat/consulta-chamados`: listagem por usuário e consulta individual.
- `feat/resolucao-chamados`: solução, data e bloqueio após resolução.
- `docs/demonstracao-api`: requisições e roteiro do vídeo de até cinco minutos.

Os endpoints e as regras exigidos na atividade ainda precisam ser implementados.
