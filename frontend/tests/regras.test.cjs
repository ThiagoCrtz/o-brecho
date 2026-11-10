const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')

// Executa os serviços TypeScript com as ferramentas já instaladas no projeto.
require.extensions['.ts'] = (module, filename) => {
  const source = ts.transpileModule(fs.readFileSync(filename, 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 }
  }).outputText
  module._compile(source, filename)
}
const { VendaService } = require('../src/services/vendaService.ts')
const { AuthService } = require('../src/services/authService.ts')
const { TriagemService } = require('../src/services/triagemService.ts')
const { gerarSenhaHash } = require('../src/repositories/BrechoDatabase.ts')
const { isGestor, podeAprovarTriagem } = require('../src/utils/permissions.ts')
const { todayLocal, formatCurrency, formatDateBR } = require('../src/utils/formatters.ts')

const produto = (id, preco) => ({ id, preco, status: 'disponivel', nome: 'Peça ' + id })
const venda = (pecas, total, extra = {}) => VendaService.gerarEntidadesVenda({ clienteId: 1, funcionarioId: 2, formaPagamento: 'Pix', pecasCarrinho: pecas, precosNegociados: {}, total, ...extra })

test('desconto final mantém pagamento, itens e venda iguais até o centavo', () => {
  for (const total of [59.9, 0.01, 0.02, 1, 64.99, 65]) {
    const resultado = venda([produto(1,30),produto(2,35)], total)
    assert.equal(resultado.novosItensVenda.reduce((s,i)=>s+Math.round(i.subtotal*100),0), Math.round(total*100))
    assert.equal(resultado.novaVenda.valor_total, resultado.novoPagamento.valor)
    assert.ok(resultado.novosItensVenda.every(i=>i.subtotal>=0))
  }
  assert.deepEqual(venda([produto(1,30),produto(2,35)],59.9).novosItensVenda.map(i=>i.subtotal),[27.65,32.25])
})

test('arredondamento distribui centavos sem criar ou perder dinheiro', () => {
  for(let quantidade=1; quantidade<=30; quantidade++) {
    const pecas=Array.from({length:quantidade},(_,i)=>produto(i+1,(i+1)*0.37))
    for(const total of [0.01,0.99,10.01,27.31]) {
      const r=venda(pecas,total)
      assert.equal(r.novosItensVenda.reduce((s,i)=>s+Math.round(i.subtotal*100),0),Math.round(total*100))
    }
  }
})

test('preço negociado de cada peça é usado na distribuição', () => {
  const r=venda([produto(1,30),produto(2,35)],40,{precosNegociados:{1:20,2:30}})
  assert.deepEqual(r.novosItensVenda.map(i=>i.subtotal),[16,24])
})

test('venda recusa peças indisponíveis, repetidas, valores inválidos e pagamento ausente', () => {
  assert.throws(()=>venda([],10))
  assert.throws(()=>venda([produto(1,10),produto(1,10)],20))
  assert.throws(()=>venda([{...produto(1,10),status:'vendido'}],10))
  assert.throws(()=>venda([{...produto(1,10),status:'aguardando_triagem'}],10))
  for(const total of [0,-1,NaN,Infinity]) assert.throws(()=>venda([produto(1,10)],total))
  assert.throws(()=>venda([produto(1,10)],10,{formaPagamento:''}))
  assert.throws(()=>venda([produto(1,10)],10,{precosNegociados:{1:-1}}))
})

test('doações criam peças pendentes, sem aprovação ou preço automático', () => {
  const r=TriagemService.criarRegistrosDoacao({data_recebimento:'2026-11-08',origem:'Teste',observacao:'',funcionario_id:2,pecas:[{nome:'Camisa',descricao:'',tamanho:'M',marca:'',estado:'Bom'}]})
  assert.equal(r.novosProdutos[0].status,'aguardando_triagem')
  assert.equal(r.novosProdutos[0].preco,0)
  assert.equal(r.novasTriagens[0].status,'Pendente')
  assert.equal(r.novasTriagens[0].doacao_id,r.novaDoacao.id)
  assert.equal(r.novasTriagens[0].produto_id,r.novosProdutos[0].id)
})

test('responsável pela triagem pode aprovar sem receber permissão de gestão', () => {
  for(const cargo of ['Administrador','Gestor','Gestora','Responsável pela Triagem']) assert.equal(podeAprovarTriagem({cargo}),true)
  assert.equal(podeAprovarTriagem({cargo:'Atendente'}),false)
  assert.equal(podeAprovarTriagem(null),false)
  for(const cargo of ['Administrador','Gestor','Gestora']) assert.equal(isGestor({cargo}),true)
  for(const cargo of ['Atendente','Responsável pela Triagem']) assert.equal(isGestor({cargo}),false)
  assert.equal(isGestor(null),false)
})

test('senha padrão não permite entrar em conta com outra senha', () => {
  const funcionarios=[{id:1,usuario:'teste',senha_hash:gerarSenhaHash('senha-de-teste')}]
  assert.equal(AuthService.autenticar(funcionarios,'teste','123456').success,false)
  assert.equal(AuthService.autenticar(funcionarios,'teste','senha-de-teste').success,true)
  assert.equal(AuthService.autenticar(funcionarios,'inexistente','senha-de-teste').success,false)
})

test('datas e valores são apresentados no padrão brasileiro', () => {
  assert.equal(formatDateBR('2026-11-08'),'08/11/2026')
  assert.equal(formatCurrency(1234.5).replace(/\s/g,' '),'R$ 1.234,50')
  assert.match(todayLocal(),/^\d{4}-\d{2}-\d{2}$/)
  const RealDate=global.Date
  global.Date=class extends RealDate { constructor(...args){super(...(args.length?args:['2026-11-09T01:30:00Z']))} }
  try { assert.equal(todayLocal(),'2026-11-08') } finally { global.Date=RealDate }
})
