# Contribuindo

Consulte primeiro o [estado atual](docs/estado-atual.md) e as [regras de negócio](backend/README.md).
Front-end e backend são executados separadamente.

## Verificar uma alteração

Na pasta `frontend`:

```sh
npm ci
npm test
npm run typecheck
npm run build
```

Na pasta `backend`, com Java 17+ e Maven:

```sh
mvn verify
```

Os testes do backend iniciam PostgreSQL temporário e encerram a instância no final.
Para aplicar a formatação Java: `mvn spotless:apply`.

## Organização

- Regras de domínio não dependem da camada HTTP nem de SQL.
- Controladores validam entradas e delegam aos casos de uso.
- Permissões devem ser verificadas no servidor, mesmo que a interface esconda o botão.
- Operações financeiras precisam preservar valores, atomicidade e comportamento em concorrência.
- Não adicione senhas, tokens, dados de clientes reais, arquivos .env ou cópias de banco.
- Mantenha README, contrato da API e testes coerentes com alterações nas regras.
- Preserve os documentos históricos; registre decisões novas indicando o que foi substituído.

Ao propor uma alteração, descreva o problema, o comportamento resultante e os testes executados.
