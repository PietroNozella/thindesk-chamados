# Thindesk

Sistema de controle de chamados técnicos desenvolvido com Java 21, Spring Boot 3.4.0, Thymeleaf, Bootstrap e MongoDB.

## Funcionalidades

- Login e logout, com aceite dos documentos legais no primeiro acesso.
- Cadastro, consulta e edição de chamados pela interface.
- Status: Aberto, Em andamento, Concluído e Cancelado.
- Cancelamento que mantém o chamado para consulta.
- Detecção de conflitos entre edição e cancelamento de chamados.
- Cadastro, edição e exclusão de clientes.
- Cadastro e exclusão de horários de atendimento por setor.
- Dashboard com total de chamados e contagem por status, calculados a partir do banco.
- Validação dos campos, proteção CSRF e controle de acesso por perfil.
- Gestão de usuários e redefinição de senha pelo administrador, com auditoria.
- Encerramento de sessões antigas após troca ou redefinição de senha.

| Perfil | Acesso aos chamados |
| --- | --- |
| Administrador | Consultar, cadastrar, editar e cancelar; também cadastra usuários. |
| Operador | Consultar, cadastrar, editar e cancelar. |
| Professor | Consultar a lista e o dashboard. |

Administradores e operadores também podem cadastrar, editar e excluir clientes e cadastrar ou excluir horários. Professores podem consultar esses registros. A gestão de usuários e a auditoria são restritas ao administrador.

## Recuperar acesso e trocar senha

A opção **Esqueceu sua senha?** orienta a procurar o administrador da instituição. Não há envio de e-mail nem geração pública de links de recuperação.

Para redefinir a senha de outra conta, o administrador deve abrir **Usuários → Redefinir senha**, confirmar a identidade da pessoa, informar sua própria senha e definir uma nova senha para ela. A nova senha deve ser entregue por um canal privado; ela não é exibida depois de salvar.

Depois de entrar, o usuário pode escolher sua senha em **Trocar senha**, informando a senha atual. O administrador também usa essa opção para a própria conta. A alteração exige ao menos oito caracteres e respeita o limite de entrada do BCrypt, sem truncar a senha.

Trocas e redefinições invalidam as sessões anteriores na próxima requisição, inclusive acessos à API. Uma operação que já estava em execução pode terminar. Os antigos endereços de recuperação por token retornam HTTP 410 e não alteram senhas; não é necessário apagar coleções antigas para desativá-los.

Se o único administrador perder o acesso, será necessária uma intervenção do responsável pelo ambiente no banco. A aplicação não oferece recuperação pública dessa conta.

## Alterações simultâneas em chamados

Cada chamado possui uma `revisao`. A interface envia esse valor ao editar ou cancelar. Se outra operação já alterou o chamado, a aplicação retorna HTTP 409, mantém o texto digitado e pede que os dados sejam recarregados antes de salvar.

O `PUT /api/chamados/{id}` também exige a `revisao` recebida na consulta, junto dos campos do chamado. Omitir a revisão retorna HTTP 400; usar uma revisão antiga retorna HTTP 409. Registros anteriores sem esse campo são lidos como revisão zero e recebem a primeira revisão ao serem alterados, sem migração prévia.

Clientes não possuem detecção de conflito entre duas edições. Uma edição, porém, não recria um cliente que tenha sido excluído por outra operação.

## Versão e semântica de versionamento

Versão atual: **`1.0.0-beta`**, definida no `pom.xml`.

O projeto adota o formato `MAJOR.MINOR.PATCH`, com um sufixo opcional de pré-lançamento:

- **MAJOR:** alterações incompatíveis com a versão anterior.
- **MINOR:** novas funcionalidades compatíveis.
- **PATCH:** correções compatíveis.
- **`-beta`:** versão de pré-lançamento, ainda em preparação para a entrega.

Nesta versão, `1` identifica a primeira versão principal; os dois zeros indicam que ainda não houve incrementos de funcionalidades ou correções após essa base. A versão final poderá ser identificada como `1.0.0` depois da validação da entrega e do deploy.

## Dashboard

Captura da aplicação executando localmente, com dados fictícios de validação persistidos no MongoDB. As quantidades variam conforme os chamados cadastrados.

![Dashboard do Thindesk com total de chamados e contagens por status](docs/images/dashboard.png)

## Executar localmente

Requisitos: JDK 21 ou superior e MongoDB disponível. O Maven Wrapper acompanha o projeto; não é necessário instalar Maven separadamente.

Por padrão, a aplicação conecta em `mongodb://localhost:27017/thindesk` e abre em <http://localhost:8080>.

No PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

No Linux ou macOS:

```sh
sh mvnw spring-boot:run
```

Em um banco novo, a conta administradora local padrão é `admin`, com senha `thindesk123`. Essas credenciais são apenas para desenvolvimento local. As variáveis abaixo permitem definir outro acesso antes da primeira inicialização.

| Variável | Finalidade | Padrão local |
| --- | --- | --- |
| `MONGODB_URI` | Endereço de conexão do MongoDB | `mongodb://localhost:27017/thindesk` |
| `MONGODB_DATABASE` | Nome do banco | `thindesk` |
| `PORT` | Porta HTTP | `8080` |
| `APP_ADMIN_USERNAME` | Nome da conta administradora inicial | `admin` |
| `APP_ADMIN_PASSWORD` | Senha da conta administradora inicial | `thindesk123` |
| `APP_ADMIN_EMAIL` | E-mail da conta administradora inicial | `admin@thindesk.local` |
| `APP_ADMIN_NOME` | Nome da conta administradora inicial | `Administrador` |

A conta inicial só é criada se o nome de usuário ainda não existir. Alterar as variáveis depois disso não muda a senha de uma conta já cadastrada. Na primeira entrada, aceite os documentos legais para acessar o dashboard.

As variáveis devem estar no ambiente do processo; a aplicação não carrega arquivos `.env` automaticamente.

## Gerar o executável

```powershell
.\mvnw.cmd package
java -jar target/thindesk-1.0.0-beta.jar
```

O JAR contém as dependências e os arquivos da interface. O MongoDB permanece como um serviço externo ao JAR.

## Verificar o fluxo integrado

O comando abaixo usa Node.js 22 ou superior, sem pacotes adicionais. Ele acessa uma aplicação real e cria chamados e usuários de teste; execute em uma instância local com banco separado dos dados de uso.

Exemplo para iniciar uma instância de testes com um MongoDB local já disponível:

```powershell
$env:MONGODB_URI='mongodb://localhost:27017/thindesk_validacao'
$env:MONGODB_DATABASE='thindesk_validacao'
$env:PORT='8081'
.\mvnw.cmd spring-boot:run
```

Em outro terminal, informe o endereço e a senha do administrador dessa instância:

```powershell
$env:THINDESK_TEST_URL='http://127.0.0.1:8081'
$env:THINDESK_TEST_ADMIN='admin'
$env:THINDESK_TEST_PASSWORD='thindesk123'
node scripts/verificar-fluxo.mjs
```

O script verifica chamados, conflitos entre operações, clientes, horários, cadastro de usuários, recuperação administrada de acesso, troca de senha, bloqueio após tentativas incorretas, revogação de sessões, auditoria, CSRF, permissões e contadores do dashboard. Os dados gerados permanecem no banco de testes. Esse comando valida o fluxo por HTTP; a inspeção visual deve ser feita no navegador.

## Deploy e entrega acadêmica

**Deploy público: pendente.** O endereço local não substitui o link público exigido na atividade.

### Hospedagem gratuita preparada

A configuração proposta usa um **Web Service Free do Render** para Java 21 e um **cluster M0 gratuito do MongoDB Atlas** para os dados. O `render.yaml` define `plan: free` explicitamente, sem discos, bancos ou outros serviços pagos. Os deploys seguintes são manuais.

O plano gratuito do Render pausa o serviço após 15 minutos sem acesso. A primeira abertura pode demorar enquanto a aplicação inicia. As 750 horas mensais são compartilhadas pelos serviços gratuitos do workspace. Excedentes de banda ou build podem gerar cobrança se houver método de pagamento cadastrado; sem método de pagamento, os limites resultam em suspensão do serviço ou de novos builds. A configuração do repositório não controla a cobrança da conta. Confira as [condições atuais do Render Free](https://render.com/docs/free).

O [MongoDB Atlas M0](https://www.mongodb.com/docs/atlas/tutorial/deploy-free-tier-cluster/) é um cluster gratuito. Na criação, escolha **M0 / Free**, mantendo desativados recursos e upgrades pagos.

Para publicar:

1. No Atlas, crie o cluster M0 e um usuário de banco com acesso de leitura e escrita ao banco `thindesk`. Copie a URI de conexão, substituindo os campos de usuário e senha. Essas credenciais são diferentes do login da aplicação.
2. No Render, crie um **Blueprint** usando o repositório e a branch que contenham `render.yaml` e `Dockerfile`. Confira que será criado somente um Web Service com plano **Free** antes de concluir.
3. Preencha os valores solicitados para `MONGODB_URI`, `APP_ADMIN_USERNAME`, `APP_ADMIN_PASSWORD` e `APP_ADMIN_EMAIL`. Use uma senha própria para o administrador. Esses valores ficam nas configurações do Render, fora do Git e da imagem.
4. Na página do serviço do Render, consulte os intervalos de IP de saída e adicione-os à lista de acesso do Atlas. A conexão ao banco só funciona quando a rede e o usuário estão autorizados. Veja [Outbound IP Addresses](https://render.com/docs/outbound-ip-addresses) e [IP Access List do Atlas](https://www.mongodb.com/docs/atlas/security/ip-access-list/).
5. Aguarde o deploy. Valide o login, o aceite, a criação e a edição de chamados pelo endereço HTTPS recebido. O healthcheck em `/esqueci-senha` verifica apenas a resposta HTTP, sem criar sessões; ele não substitui o teste do banco.
6. Cadastre uma conta de demonstração com perfil Operador e registre o endereço e as credenciais de demonstração abaixo. Guarde as credenciais de administrador em local privado.

Os segredos com `sync: false` são solicitados na criação do Blueprint. Para adicionar ou alterar segredos depois, use as configurações de ambiente do serviço no Render. A conta administradora inicial não tem a senha alterada automaticamente por um novo deploy.

O contêiner executa como usuário sem privilégios, limita o heap da JVM e exige conexão ao banco e credenciais explícitas. A senha de desenvolvimento `thindesk123` é recusada no contêiner. No Render, o Blueprint ativa cookies HTTPS, tratamento dos cabeçalhos do proxy e cache dos templates. Reinícios e suspensão encerram as sessões; os chamados e usuários permanecem no Atlas.

### Testar a imagem localmente

Com Docker disponível e um MongoDB de testes acessível pelo contêiner:

```powershell
docker build -t thindesk:local .
```

Inicie a imagem com `MONGODB_URI`, `MONGODB_DATABASE`, `APP_ADMIN_USERNAME` e `APP_ADMIN_PASSWORD` no ambiente, publicando a porta HTTP desejada. Para verificar o limite de memória, use `--memory=512m --memory-swap=512m`. A aplicação já lê a variável `PORT` fornecida pela hospedagem.

```powershell
docker run --rm -p 8080:8080 --memory=512m --memory-swap=512m --env MONGODB_URI --env MONGODB_DATABASE --env APP_ADMIN_USERNAME --env APP_ADMIN_PASSWORD thindesk:local
```

As opções de proxy e cookie seguro do `render.yaml` se aplicam ao HTTPS do Render; não são necessárias no teste local por HTTP. O comando `node scripts/verificar-fluxo.mjs`, descrito acima, pode validar a instância local em contêiner.

Na validação local da imagem, 63 verificações integradas passaram sob limite de 512 MiB e 0,1 CPU. O pico de memória foi de aproximadamente 214 MiB, sem encerramento por falta de memória. A inicialização levou cerca de 95 segundos sob essa cota de CPU. Esses resultados não substituem a validação da rede, das credenciais e do login no Render com o Atlas.

Repositório individual: [PietroNozella/thindesk-chamados](https://github.com/PietroNozella/thindesk-chamados).
A `main` contém a aplicação com interface. A configuração desta etapa está na [branch chore/deploy-gratuito](https://github.com/PietroNozella/thindesk-chamados/tree/chore/deploy-gratuito).

Para concluir a entrega:

- Hospedar o JAR em um ambiente com Java 21 ou superior e configurar a conexão ao MongoDB e a conta administradora inicial.
- Criar uma conta de demonstração com perfil Operador em **Usuários → Cadastrar usuário**.
- Registrar aqui o endereço público e as credenciais de demonstração, sem expor a conta administradora ou as credenciais do banco.
- Testar o login e o fluxo de chamados pelo endereço público em janela anônima.
- Conferir que o código do GitHub, a aplicação publicada e o ZIP solicitado correspondem à mesma versão.

| Item da entrega | Situação |
| --- | --- |
| GitHub individual | Aplicação com interface na `main`; configuração de hospedagem na branch `chore/deploy-gratuito` |
| Versão e explicação | Documentadas neste README |
| Print do dashboard | Incluído acima |
| Link do deploy | Pendente |
| Credenciais de demonstração do deploy | Pendentes |
