import React, { useState } from 'react'
import { useBrecho } from '../../context/BrechoContext'
import { formatCurrency, formatDateBR, todayLocal } from '../../utils/formatters'

export const RelatoriosView: React.FC = () => {
  const { vendas,pagamentos,clientes,atendentes,itensVenda,produtos }=useBrecho()
  const [inicio,setInicio]=useState('')
  const [fim,setFim]=useState('')
  const invalido=!!(inicio&&fim&&inicio>fim)
  const lista=invalido?[]:vendas.filter(v=>(!inicio||v.data>=inicio)&&(!fim||v.data<=fim)).sort((a,b)=>b.data.localeCompare(a.data)||b.id-a.id)
  const ids=new Set(lista.map(v=>v.id))
  const recebimentos=pagamentos.filter(p=>ids.has(p.venda_id))
  const total=recebimentos.reduce((s,p)=>s+p.valor,0)
  const pecas=itensVenda.filter(i=>ids.has(i.venda_id)).reduce((s,i)=>s+i.quantidade,0)
  return <section className="page">
    <div className="page-head"><div><p className="eyebrow">RECEBIMENTOS DA ANG</p><h1>Vendas e caixa</h1><p className="lead">Escolha um período para conferir as vendas e os pagamentos registrados.</p></div></div>
    <div className="toolbar"><label htmlFor="relatorio-inicio">De<input id="relatorio-inicio" type="date" value={inicio} onChange={e=>setInicio(e.target.value)} aria-invalid={invalido}/></label><label htmlFor="relatorio-fim">Até<input id="relatorio-fim" type="date" value={fim} onChange={e=>setFim(e.target.value)} aria-invalid={invalido}/></label><button className="btn secondary" onClick={()=>{setInicio(todayLocal());setFim(todayLocal())}}>Hoje</button><button className="btn secondary" onClick={()=>{setInicio('');setFim('')}}>Todo o período</button></div>
    {invalido&&<p className="field-error" role="alert">A data final deve ser igual ou posterior à data inicial.</p>}
    <p className="note" role="status">{!inicio&&!fim?'Mostrando todo o período registrado.':'Período: '+(inicio?formatDateBR(inicio):'primeiro registro')+' a '+(fim?formatDateBR(fim):'último registro')+'.'}</p>
    <div className="stats"><div className="stat"><div className="stat-label">Total recebido no período</div><div className="stat-value">{formatCurrency(total)}</div></div><div className="stat"><div className="stat-label">Vendas realizadas</div><div className="stat-value">{lista.length}</div></div><div className="stat"><div className="stat-label">Peças vendidas</div><div className="stat-value">{pecas}</div></div></div>
    <div className="panel" style={{marginBottom:24}}><h2>Como os clientes pagaram</h2><div className="grid grid-3">{['Pix','Dinheiro','Cartão'].map(m=><div key={m}><span className="stat-label">{m}</span><strong style={{display:'block',fontSize:22}}>{formatCurrency(recebimentos.filter(p=>p.metodo===m).reduce((s,p)=>s+p.valor,0))}</strong></div>)}</div><p className="note">Estes valores são recebimentos de vendas. O sistema não registra despesas, retiradas ou saldo físico do caixa.</p></div>
    <h2>Vendas do período</h2>
    <div className="table-wrap" tabIndex={0} role="region" aria-label="Tabela de registros. Use as setas para ver todas as colunas."><table><thead><tr><th>Data</th><th>Cliente</th><th>Peças vendidas</th><th>Atendente</th><th>Pagamento</th><th>Total</th></tr></thead><tbody>{lista.length===0?<tr><td colSpan={6} className="empty">{invalido?'Corrija o período para consultar.':'Nenhuma venda nesse período. Escolha outras datas ou consulte todo o período.'}</td></tr>:lista.map(v=><tr key={v.id}><td>{formatDateBR(v.data)}</td><td><strong>{clientes.find(c=>c.id===v.cliente_id)?.nome||'Cadastro indisponível'}</strong></td><td>{itensVenda.filter(i=>i.venda_id===v.id).map(i=>produtos.find(p=>p.id===i.produto_id)?.nome||'Peça #'+i.produto_id).join(', ')||'Sem itens registrados'}</td><td>{atendentes.find(a=>a.id===v.funcionario_id)?.nome||'Cadastro indisponível'}</td><td>{pagamentos.find(p=>p.venda_id===v.id)?.metodo||'Não registrado'}</td><td><strong>{formatCurrency(v.valor_total)}</strong></td></tr>)}</tbody></table></div>
  </section>
}
