# O-BRECHÓ — ANG

Sistema de gestão interna do brechó da Associação Nova Geração, desenvolvido no contexto
do Projeto Integrador de Análise e Desenvolvimento de Sistemas da FATEC Sorocaba.

O sistema acompanha doações, triagem, peças disponíveis, clientes, vendas e receitas.
A interface prioriza pessoas com pouca familiaridade com tecnologia: linguagem direta,
controles legíveis e tarefas do dia a dia visíveis.

**Estado em 10/11/2026:** front-end demonstrativo e backend implementado em pastas independentes.
A interface ainda não está conectada à API. Publicar o código no GitHub não implanta o sistema.

[Estado atual](docs/estado-atual.md) · [Documentação](docs/README.md) ·
[Backend e permissões](backend/README.md) · [Contrato HTTP](backend/API.md) ·
[OpenAPI](backend/openapi.json) · [Contribuir](CONTRIBUTING.md)

## O que está implementado

- Recebimento de doações e avaliação das peças.
- Aprovação de triagem por administradores ou responsáveis pela triagem.
- Catálogo, estoque e localização de peças disponíveis.
- Clientes e histórico de compras.
- Vendas com descontos, registro de pagamento e relatórios por período.
- Backend com autenticação, autorização por perfil, auditoria e transações.
- Proteção contra venda duplicada e reenvio da mesma compra.
- Testes automatizados e documentação da API.

O pagamento é registrado pelo sistema; não há processamento bancário, loja virtual,
entregas, agendamento, consignação ou múltiplas unidades nesta versão.

## Estrutura

```text
o-brecho/
├── frontend/       React, TypeScript, Vite e Tailwind
├── backend/        Java 17, Spring Boot, PostgreSQL e Flyway
├── data/           Scripts MySQL da versão anterior (referência histórica)
├── docs/           Requisitos, casos de uso, arquitetura, estudos e testes
├── CONTRIBUTING.md Comandos de validação e orientações de contribuição
├── SECURITY.md     Orientações de segurança
└── PRODUCT-BACKLOG.md Planejamento original e ligação com as próximas entregas
```

## Baixar e executar o front-end

Requer Node.js 22 e npm.

```sh
git clone https://github.com/ThiagoCrtz/o-brecho.git
cd o-brecho/frontend
npm ci
npm run dev
```

Abra o endereço informado pelo Vite, normalmente http://localhost:5173.
O front-end usa dados fictícios e armazenamento local do navegador.
Contas demonstrativas: `admin`, `atendente` e `clara`, senha `123456`.
Essas contas não são credenciais do backend e não devem ser usadas como acesso de produção.

[Guia do front-end](frontend/README.md) · [Redesenho acessível](docs/prototipos/redesenho-acessivel.md)

## Executar o backend

Requer JDK 17+ e Maven 3.6.3+. Para compilar e testar:

```sh
cd o-brecho/backend
mvn verify
```

Os testes iniciam um PostgreSQL temporário real; não é necessário instalar um serviço de banco
para executá-los. O primeiro uso baixa dependências e os binários de teste.

Para manter a API em execução, configure PostgreSQL e as variáveis de ambiente conforme
o [guia do backend](backend/README.md#executar-para-uso-local), e execute:

```sh
java -jar target/brecho-api-1.0.0-SNAPSHOT.jar
```

O backend não possui senha padrão: o primeiro administrador é criado por configuração explícita.
A API usa UUIDs e exige autenticação própria. A migração atual está em
[backend/src/main/resources/db/migration](backend/src/main/resources/db/migration).

## Regras de acesso

| Operação | Administrador | Atendente | Triagem |
|---|---|---|---|
| Receber doação e consultar peças | Sim | Sim | Sim |
| Aprovar triagem | Sim | Não | Sim |
| Vender e conceder descontos | Sim | Sim | Não |
| Consultar relatórios e histórico | Sim | Sim | Sim |
| Gerenciar equipe e senhas | Sim | Não | Não |

Consulte a [matriz completa e as regras de negócio](backend/README.md).
As verificações seguras são realizadas no backend. A interface demonstrativa não substitui
essas verificações.

## Verificações realizadas

- Front-end: 8 testes de regras aprovados, verificação TypeScript e build.
- Backend: 25 testes aprovados, incluindo PostgreSQL real, concorrência de vendas,
  reversão de transações, permissões, autenticação e HTTP real.
- [Comandos para repetir a validação](CONTRIBUTING.md).
- [Evidências e limitações do backend](backend/README.md#validação-e-limites-desta-entrega).

Ainda faltam integração front-end/API, validação com a equipe da ANG, testes de carga
e preparação da operação em produção. Não foram migrados dados reais.

## Documentação

| Assunto | Referência |
|---|---|
| Situação atual e próximas entregas | [Estado atual](docs/estado-atual.md) |
| Execução e arquitetura do backend | [Guia do backend](backend/README.md) |
| Endpoints e exemplos | [API](backend/API.md), [OpenAPI](backend/openapi.json), [requests.http](backend/requests.http) |
| Decisão de tecnologia e camadas | [Backend Java independente](docs/decisoes/DECISAO-003-backend-java.md) |
| Contexto acadêmico e equipe | [Estudo e equipe](docs/academico/README.md) |
| Requisitos e casos de uso | [Requisitos](docs/requisitos/README.md), [Casos de uso](docs/casos-de-uso/README.md) |
| Histórico e fontes originais | [Evolução](docs/continuidade/README.md), [Fontes](docs/fontes.md) |
| Segurança e contribuição | [Segurança](SECURITY.md), [Contribuição](CONTRIBUTING.md) |

Os documentos acadêmicos anteriores permanecem preservados. Quando descreverem tecnologias
planejadas, regras antigas ou telas anteriores, consulte o estado atual e o contrato do backend.

## Equipe e origem

Arthur Mendes Entsev, Eduardo Kioshi Yasuoka, Gustavo Henrique Oliveira da Silva
e Thiago Pereira Cortez. Cliente/parceiro: Associação Nova Geração (ANG).

Esta cópia reúne a evolução do [projeto original ANG](https://github.com/arthurlynow098/ANG)
com o redesenho da interface e o backend Java. O histórico acadêmico e seus créditos foram preservados.
