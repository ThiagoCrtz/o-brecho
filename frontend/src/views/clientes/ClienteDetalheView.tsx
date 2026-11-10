import React from 'react'
import { useBrecho } from '../../context/BrechoContext'
import { formatCurrency, formatDateBR } from '../../utils/formatters'

export const ClienteDetalheView: React.FC=()=>{
  const {clienteSelecionado:c,vendas,itensVenda,produtos,pagamentos,atendentes,obterNomeCategoria,go}=useBrecho()
  if(!c)return <section className="page"><h1>Selecione um cliente</h1><button className="btn" onClick={()=>go('clientes')}>Voltar para clientes</button></section>
  const compras=vendas.filter(v=>v.cliente_id===c.id).sort((a,b)=>b.data.localeCompare(a.data)||b.id-a.id)
  const ids=new Set(compras.map(v=>v.id))
  const itens=itensVenda.filter(i=>ids.has(i.venda_id))
  const grupos=new Map<string,typeof itens>()
  itens.forEach(i=>{const categoria=obterNomeCategoria(produtos.find(p=>p.id===i.produto_id)?.categoria_id??null);grupos.set(categoria,[...(grupos.get(categoria)||[]),i])})
  return <section className="page"><button className="text-button" onClick={()=>go('clientes')}>← Voltar para clientes</button><div className="page-head"><div><h1>{c.nome}</h1><p className="lead">{c.telefone}</p></div></div>
    <div className="stats"><div className="stat"><span className="stat-label">Compras realizadas</span><div className="stat-value">{compras.length}</div></div><div className="stat"><span className="stat-label">Total em compras</span><div className="stat-value">{formatCurrency(compras.reduce((s,v)=>s+v.valor_total,0))}</div></div><div className="stat"><span className="stat-label">Peças compradas</span><div className="stat-value">{itens.reduce((s,i)=>s+i.quantidade,0)}</div></div></div>
    <div className="panel" style={{marginBottom:24}}><h2>Dados do cliente</h2><div className="form-grid"><div><span className="stat-label">Telefone</span><div>{c.telefone}</div></div><div><span className="stat-label">E-mail</span><div>{c.email||'Não informado'}</div></div>{c.observacoes&&<div className="full"><span className="stat-label">Observações</span><p>{c.observacoes}</p></div>}</div></div>
    <h2>Peças compradas por categoria</h2>{!itens.length?<p className="empty">Ainda não há peças compradas por este cliente.</p>:<div className="grid grid-2" style={{marginBottom:32}}>{[...grupos].map(([categoria,lista])=><div className="panel" key={categoria}><div className="section-heading"><h3>{categoria}</h3><span className="badge">{lista.reduce((s,i)=>s+i.quantidade,0)} peças</span></div><ul style={{margin:0,paddingLeft:20}}>{lista.map(i=><li key={i.id}>{produtos.find(p=>p.id===i.produto_id)?.nome||'Peça #'+i.produto_id}</li>)}</ul></div>)}</div>}
    <h2>Histórico de compras</h2><div className="table-wrap" tabIndex={0} role="region" aria-label="Tabela de registros. Use as setas para ver todas as colunas."><table><thead><tr><th>Data</th><th>Peças</th><th>Atendente</th><th>Pagamento</th><th>Total pago</th></tr></thead><tbody>{!compras.length?<tr><td colSpan={5} className="empty">Nenhuma compra registrada.</td></tr>:compras.map(v=><tr key={v.id}><td>{formatDateBR(v.data)}</td><td>{itens.filter(i=>i.venda_id===v.id).map(i=>produtos.find(p=>p.id===i.produto_id)?.nome||'Peça #'+i.produto_id).join(', ')}</td><td>{atendentes.find(a=>a.id===v.funcionario_id)?.nome||'Cadastro indisponível'}</td><td>{pagamentos.find(p=>p.venda_id===v.id)?.metodo||'Não registrado'}</td><td><strong>{formatCurrency(v.valor_total)}</strong></td></tr>)}</tbody></table></div>
  </section>
}
