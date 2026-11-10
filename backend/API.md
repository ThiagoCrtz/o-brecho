# Contrato HTTP

Base: `http://localhost:8080`. Corpos JSON. Rotas privadas exigem `Authorization: Bearer TOKEN`.

O [openapi.json](openapi.json) pode ser importado em ferramentas de API.
[requests.http](requests.http) contém uma sequência de chamadas para começar.

## Rotas

| Método | Rota | Finalidade |
|---|---|---|
| GET | /health | Disponibilidade do processo |
| POST | /api/auth/login | Iniciar sessão |
| GET | /api/auth/me | Consultar conta autenticada |
| POST | /api/auth/logout | Encerrar sessão atual |
| GET / POST | /api/equipe | Consultar / cadastrar funcionários |
| PUT | /api/equipe/{id} | Editar dados, perfil e situação |
| POST | /api/equipe/{id}/senha | Redefinir senha |
| GET / POST | /api/categorias | Consultar / cadastrar categorias |
| GET / POST | /api/clientes | Consultar / cadastrar clientes |
| PUT | /api/clientes/{id} | Editar cliente ativo |
| POST | /api/clientes/{id}/arquivar | Impedir novas compras, mantendo histórico |
| GET / POST | /api/doacoes | Consultar / receber doações |
| GET | /api/produtos | Consultar estoque / fila de triagem |
| POST | /api/triagens/{id}/aprovar | Aprovar e precificar a peça |
| POST | /api/triagens/{id}/descartar | Descartar peça pendente, com justificativa |
| POST | /api/triagens/{id}/reavaliar | Reabrir peça descartada, com justificativa |
| PATCH | /api/produtos/{id}/localizacao | Mover peça disponível |
| POST | /api/vendas | Concluir venda de forma atômica |
| GET | /api/vendas | Consultar vendas / histórico por cliente |
| GET | /api/vendas/{id} | Detalhar venda e valores de seus itens |
| GET | /api/relatorios | Receita, vendas e peças por período, método e categoria |
| GET | /api/auditoria | Histórico administrativo de operações |

Em `/triagens/{id}`, o identificador é **o UUID do produto**, retornado por `GET /api/produtos`.
Cada peça tem uma situação atual e suas transições ficam na auditoria.

Listas de equipe, clientes, doações, produtos, vendas e auditoria:
`?pagina=0&tamanho=50`, com página iniciando em zero e tamanho entre 1 e 100.
Retornam arrays; uma página vazia indica o fim. Categorias não são paginadas.

Produtos aceitam `status=PENDENTE|DISPONIVEL|DESCARTADO|VENDIDO`.
Vendas e relatórios exigem `inicio=AAAA-MM-DD&fim=AAAA-MM-DD` (período máximo de dez anos).
Vendas também aceitam `clienteId=UUID`.

## Entradas

Os campos são definidos no OpenAPI. Campos obrigatórios de texto opcional devem ser enviados
como `""`, não `null`. Todos os campos de `Staff`, incluindo `ativo`, são obrigatórios.
`PUT` substitui todos os campos editáveis. IDs, autor e datas não são aceitos no corpo.

Cadastros retornam `201 {"id":"UUID"}`. Alterações e logout retornam `204`, sem corpo.
Venda retorna `201` com os detalhes; o reenvio idempotente retorna o mesmo identificador.

Uma venda exige exatamente um dos campos `clienteId` ou `novoCliente`.
O campo `precoNegociado` em cada item é opcional: ausente, usa o preço do banco.
O `total` também é opcional: ausente, usa a soma dos preços negociados.
Gere uma chave UUID no cabeçalho `Idempotency-Key` e mantenha-a ao repetir a mesma solicitação
depois de uma falha de conexão.

## Saídas

Os cadastros usam nomes de entrada em português; as projeções de consulta usam os nomes
do banco em inglês, com palavras separadas por sublinhado. Essa distinção está documentada
no OpenAPI para a futura adaptação do front-end.

Exemplo de consulta de peça:

```json
{
  "id": "d352c5dc-d488-4aa7-8208-adca76c43e10",
  "donation_id": "27326b73-a738-479a-b86f-81540c90fef7",
  "name": "Camisa",
  "description": "",
  "size": "M",
  "brand": "Sem marca",
  "condition": "BOM",
  "category_id": null,
  "price": 0.00,
  "status": "PENDENTE",
  "location": "Mesa de Triagem",
  "notes": ""
}
```

Datas de gravação vêm do servidor. IDs são UUIDs, nunca índices de lista.
A sessão retorna `token`, `expiresAt` e `usuario: {id, nome, role}`.
O token não aparece nas consultas de funcionário.

Relatório retorna `vendas`, `recebido`, `pecas`, `porPagamento` e `porCategoria`.
Os valores por categoria usam o valor efetivamente pago, após o rateio do desconto.

## Erros

Formato `application/problem+json`, com pelo menos `status` e `detail`.

| HTTP | Significado |
|---|---|
| 400 | JSON, campo, cabeçalho ou parâmetro inválido |
| 401 | Login inválido, sessão ausente, expirada ou revogada |
| 403 | Perfil sem permissão |
| 404 | Registro inexistente |
| 409 | Estado mudou, duplicidade ou chave de reenvio em conflito |
| 413 | Corpo maior que 64 KiB |
| 422 | Regra de negócio não atendida |
| 429 | Limite de tentativas de login |
| 500 | Falha interna; guarde a referência retornada |

Ao receber 409 na venda, consulte novamente a disponibilidade das peças.
Não troque a chave de reenvio automaticamente: a primeira solicitação pode ter sido concluída.
