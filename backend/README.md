# Backend do O-BRECHÓ

API Java 17 + Spring Boot 3.5.16, Spring Security, JDBC e PostgreSQL.
Pasta independente do front-end. O front-end continua demonstrativo, sem conexão com esta API.
Não há importação automática de dados ou senhas do navegador.

## Executar os testes

Requisitos: JDK 17+ e Maven 3.6.3+. Na pasta `backend`:

```powershell
mvn verify
```

Os testes iniciam e encerram **PostgreSQL 18.3 real** em uma porta aleatória, exclusivamente para testes.
O primeiro comando baixa dependências e os binários do PostgreSQL. Não é necessário instalar Docker
para testar. O banco temporário não usa o banco configurado para a aplicação.

A suíte cobre regras monetárias, permissões, validação, autenticação, revogação, transações,
concorrência entre vendedores, reenvios de venda e uma chamada HTTP real. O build também verifica
a formatação Java. Para formatar alterações: `mvn spotless:apply`.

O executável é produzido em `target/brecho-api-1.0.0-SNAPSHOT.jar`.

## Executar para uso local

1. Disponibilize um PostgreSQL 18 com um banco vazio chamado `brecho`.
   Se tiver Docker, copie `.env.example` para `.env`, defina uma senha própria em
   `DB_PASSWORD` e execute `docker compose up -d`.
   A porta do banco fica vinculada somente a `127.0.0.1`.
2. Configure as variáveis de ambiente no terminal que executará a API.
   **Spring Boot não carrega o arquivo .env automaticamente.**
3. No primeiro início, configure o administrador inicial. Não existe senha padrão.

Exemplo em PowerShell:

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/brecho'
$env:DB_USER = 'brecho'
$senhaBanco = Read-Host 'Senha do banco' -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $senhaBanco).Password
$env:BOOTSTRAP_USERNAME = 'administrador'
$senhaInicial = Read-Host 'Senha inicial: mínimo de 12 caracteres' -AsSecureString
$env:BOOTSTRAP_PASSWORD = [System.Net.NetworkCredential]::new('', $senhaInicial).Password
$env:FRONTEND_ORIGIN = 'http://localhost:5174'
java -jar target/brecho-api-1.0.0-SNAPSHOT.jar
```

O endereço padrão é `http://localhost:8080`. `GET /health` verifica se o servidor responde;
não é uma verificação de disponibilidade do banco.

O Flyway aplica as migrações versionadas em `src/main/resources/db/migration`.
O administrador inicial é criado somente se não houver nenhum funcionário.
Após a primeira inicialização, retire `BOOTSTRAP_USERNAME` e `BOOTSTRAP_PASSWORD` do ambiente.
Alterar essas variáveis depois **não redefine** a senha existente.

Não há categorias nem dados fictícios pré-cadastrados. O administrador cadastra as categorias
e a equipe pela API antes de receber e aprovar as primeiras peças.

## Permissões verificadas no servidor

| Operação | Administrador | Atendente | Triagem |
|---|---|---|---|
| Consultar doações, peças, categorias e clientes | Sim | Sim | Sim |
| Receber doação | Sim | Sim | Sim |
| Descartar peça pendente e reabrir peça descartada | Sim | Sim | Sim |
| Aprovar triagem e definir categoria/preço | Sim | Não | Sim |
| Alterar localização de peça disponível | Sim | Não | Sim |
| Registrar venda e conceder desconto | Sim | Sim | Não |
| Cadastrar/editar cliente | Sim | Sim | Sim |
| Consultar vendas, relatórios e histórico de clientes | Sim | Sim | Sim |
| Arquivar cliente | Sim | Não | Não |
| Cadastrar categorias | Sim | Não | Não |
| Gerenciar equipe, perfis e senhas | Sim | Não | Não |
| Consultar auditoria | Sim | Não | Não |

Perfis no contrato: `ADMINISTRADOR`, `ATENDENTE`, `TRIAGEM`.
O perfil de gestor da interface corresponde a `ADMINISTRADOR`; Clara corresponde a `TRIAGEM`.
A permissão vem da conta consultada no banco, nunca de um perfil enviado pelo cliente.

As regras confirmadas pelo responsável pelo projeto são: administradores e triagem podem aprovar;
atendentes podem conceder descontos; toda a equipe pode consultar relatórios e histórico.
As demais restrições da tabela são a política inicial implementada e estão centralizadas em
`domain/Actor.java` e nos serviços de aplicação.

## Regras de negócio

- Cada produto representa uma peça física: não existe quantidade variável na venda.
- Produtos nascem de uma doação, na situação `PENDENTE`. Não há cadastro direto de produto à venda.
- Aprovar exige peça pendente, categoria existente, preço positivo e localização.
- Transições: `PENDENTE → DISPONIVEL → VENDIDO`; `PENDENTE → DESCARTADO → PENDENTE`.
- Peça vendida não pode ser aprovada, descartada, reaberta, movida ou vendida novamente.
- Datas, autoria da operação e preço original vêm do servidor.
- Toda venda exige cliente ativo existente **ou** os dados de um novo cliente.
  O novo cliente é criado dentro da mesma transação.
- O preço negociado de um item não pode superar seu preço cadastrado.
  O total final não pode superar a soma dos preços negociados e precisa ser positivo.
- Não foi definido teto percentual de desconto: qualquer desconto que mantenha o total positivo
  é permitido aos vendedores autorizados. Não há acréscimo de preço na venda.
- Valores usam `BigDecimal` e `NUMERIC`, com duas casas decimais.
  O desconto final é distribuído proporcionalmente e o último centavo é preservado.
  Descontos muito altos podem zerar uma linha; a venda inteira não pode ser gratuita.
- Métodos: `PIX`, `DINHEIRO`, `CARTAO`. O backend registra o recebimento;
  não processa pagamentos em instituições financeiras.
- Venda, itens, recebimento, estoque, eventual novo cliente e auditoria são confirmados juntos.
  Qualquer erro desfaz toda a operação.
- As peças são bloqueadas em ordem estável durante a venda. Há também uma restrição única
  no banco para impedir que uma mesma peça apareça em duas vendas.
- O cabeçalho `Idempotency-Key` é obrigatório na venda: gere um UUID por tentativa de compra.
  Reenviar o mesmo corpo com a mesma chave e vendedor devolve a mesma venda.
  Reutilizar a chave com outro corpo ou vendedor retorna conflito.
- Não há exclusão física de funcionários, vendas, doações ou histórico.
  Contas são desativadas; clientes podem ser arquivados.
- Um administrador não pode desativar a própria conta nem retirar seu próprio perfil.
  Alterações de equipe são serializadas e verificam novamente a conta do administrador,
  impedindo que alterações simultâneas removam todos os administradores.
- Relatórios interpretam datas em `America/Sao_Paulo`, incluindo o dia final.
- Estornos, trocas, despesas, divisão de pagamento e conciliação bancária não fazem parte
  das regras atuais e não possuem rotas que permitam contornar o fluxo.

## Autenticação e proteção

`POST /api/auth/login` retorna um token opaco aleatório válido por oito horas.
Envie `Authorization: Bearer TOKEN` nas demais chamadas. Somente o hash SHA-256
do token é persistido; senhas usam BCrypt com custo 12. Senhas não são devolvidas nas consultas.

Login é limitado por usuário e endereço remoto, com contadores compartilhados no banco:
10 tentativas por conta e 50 por endereço em janelas de 15 minutos, incluindo tentativas bem-sucedidas.
Cabeçalhos de IP enviados pelo cliente não são usados.
Em implantação atrás de proxy, configure o tratamento de IP confiável na infraestrutura:
por padrão, o limite por endereço enxergará o IP do proxy.

Logout remove a sessão. Alterar conta, perfil ou senha revoga todas as sessões daquela conta.
A emissão de sessão verifica novamente a senha e a conta sob bloqueio, para não aceitar
uma senha antiga durante uma redefinição simultânea.

A API não usa cookies nem HTTP Basic. Por isso não usa proteção CSRF baseada em cookie.
Ao integrar o front-end, mantenha o token em memória e solicite login novamente quando expirar.
Se o transporte for alterado para cookies, a proteção CSRF precisa ser habilitada.

CORS aceita somente a origem configurada em `FRONTEND_ORIGIN` (inclusive protocolo e porta).
JSON com campos desconhecidos é rejeitado. Corpo máximo: 64 KiB, inclusive sem Content-Length.
As listas paginadas aceitam até 100 registros por página. Consultas SQL são parametrizadas.
Respostas privadas usam `Cache-Control: no-store`; erros não expõem SQL, senhas ou rastros internos.

Antes de disponibilizar na internet, configure HTTPS, credenciais próprias, backups com teste
de restauração e monitoramento. O Compose fornecido é para desenvolvimento local.
O usuário do banco usado por Flyway precisa de permissões de migração; em produção,
separe a execução de migrações do usuário operacional com privilégios mínimos.

## Arquitetura

```text
src/main/java/br/org/ang/brecho/
  domain/                 Regras e tipos independentes de HTTP, Spring e SQL
  application/            Casos de uso, limites transacionais e contratos de entrada
    port/                  Interfaces de persistência usadas pelos casos de uso
  infrastructure/         Implementações JDBC, controle de tentativas e bootstrap
  web/                    Rotas, autenticação HTTP, validação e respostas de erro
src/main/resources/db/    Migrações Flyway
src/test/                 Testes de domínio e integração com PostgreSQL
```

A API é uma aplicação única organizada por responsabilidades. Os controladores não executam SQL.
Os serviços dependem de interfaces de repositório; as implementações JDBC são injetadas pelo Spring.
As regras monetárias e de perfil ficam no domínio. As consultas de leitura retornam projeções
dos dados, sem expor as entidades de autenticação.

JDBC foi escolhido para tornar explícitos os bloqueios, consultas e limites transacionais.
Não há ORM nem necessidade de sessões de persistência fora do caso de uso.
Novas integrações podem implementar as interfaces existentes sem alterar as regras do domínio.
Sessões e limites de login ficam no banco, permitindo mais de uma instância da aplicação.

## Contrato e exemplos

Veja [API.md](API.md), [openapi.json](openapi.json) e [requests.http](requests.http).
A API usa UUIDs; os identificadores numéricos do protótipo não são reutilizados.
A futura integração precisará adaptar os nomes dos campos e substituir o armazenamento local.

## Validação e limites desta entrega

A suíte executada em 09/11/2026 possui 25 testes: 23 de integração e 2 de valores monetários.
Foram executados com PostgreSQL 18.3 e servidor Spring Boot real, além dos testes via MockMvc.
O comando `mvn verify` produz o JAR e verifica a formatação.

Não houve integração com o front-end, migração de dados reais, implantação em servidor,
teste de carga ou auditoria externa de segurança. Nenhum teste comprova ausência de todos os defeitos.
