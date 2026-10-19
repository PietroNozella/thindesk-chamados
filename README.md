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

Esta main registra a base de aula, com WAR, Java 23 e telas originais.
A preparação da execução e do backend será feita em `chore/preparacao-backend`.
Ainda não há validação de build ou de execução desta base original.

## Desenvolvimento

Cada entrega parte da main atualizada, usa uma branch própria e entra por PR:

- `chore/preparacao-backend`: build, ambiente e preparação para API.
- `feat/autenticacao`: acesso pela API e perfis de solicitante e técnico.
- `feat/abertura-chamados`: abertura com dados e status automáticos.
- `feat/consulta-chamados`: listagem por usuário e consulta individual.
- `feat/resolucao-chamados`: solução, data e bloqueio após resolução.
- `docs/demonstracao-api`: requisições e roteiro do vídeo de até cinco minutos.

Os endpoints e as regras exigidos na atividade ainda precisam ser implementados.
