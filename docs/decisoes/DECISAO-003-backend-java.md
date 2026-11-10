# DECISÃO 003 — Backend Java independente

Data: 09/11/2026. Estado: implementado e testado.

## Contexto

O front-end era um protótipo local. Era necessário verificar identidade, perfil e regras
de negócio no servidor, com persistência compartilhada. O responsável pelo projeto escolheu
Java e pediu uma entrega independente, sem conectar a interface nesta etapa.

## Decisão

Usar Java 17, Spring Boot, Spring Security, JDBC, PostgreSQL e Flyway na pasta `backend/`.
Separar domínio, casos de uso, interfaces de persistência, implementações JDBC e HTTP.
Usar uma aplicação única, com injeção de dependências e limites transacionais explícitos.

A autenticação usa senha BCrypt e sessões opacas persistidas por hash. Contas desativadas
e sessões revogadas deixam de autorizar novas requisições. O perfil é obtido no servidor.
O primeiro administrador é criado por variáveis de ambiente, sem credenciais padrão.

Transações, bloqueios de linhas e unicidade de peça vendida impedem vendas duplicadas.
A chave de idempotência permite repetir uma solicitação sem registrar outra compra.
Valores monetários usam BigDecimal e NUMERIC, com rateio de descontos preservando centavos.

## Consequências

- O PostgreSQL é a referência da API; os scripts MySQL antigos não são executados.
- Os identificadores da API são UUIDs, exigindo adaptação na futura integração.
- O backend pode ser testado sem instalar um banco permanente: os testes iniciam PostgreSQL temporário.
- A interface mantém o armazenamento demonstrativo até a integração ser solicitada.
- A instalação em produção exige decisões de hospedagem, HTTPS, backups e segredos.
- A autorização de triagem é por perfil de administrador ou triagem, não por pessoa específica.

[Implementação e instruções](../../backend/README.md) · [Contrato](../../backend/API.md)
