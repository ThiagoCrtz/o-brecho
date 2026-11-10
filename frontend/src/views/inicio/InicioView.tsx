import React from 'react'
import { useBrecho } from '../../context/BrechoContext'
import { formatCurrency, formatDateBR, todayLocal } from '../../utils/formatters'

export const InicioView: React.FC = () => {
  const { go, usuarioLogado, produtos, triagens, doacoes, vendas, pagamentos } = useBrecho()
  const pendentes = triagens.filter(t => t.status === 'Pendente')
  const disponiveis = produtos.filter(p => p.status === 'disponivel').length
  const hoje = todayLocal()
  const recebidoHoje = pagamentos.filter(p => p.data === hoje).reduce((s, p) => s + p.valor, 0)
  return <section className="page">
    <div className="page-head"><div><p className="eyebrow">O BRECHÓ · NOSSO DIA A DIA</p><h1>Olá, {usuarioLogado?.nome.split(' ')[0]}.</h1><p className="lead">O que vamos fazer agora?</p></div><span className="today-label">{formatDateBR(hoje)}</span></div>
    <div className="home-actions">
      <button className="task-card task-primary" onClick={() => go('nova-venda')}><span className="eyebrow">ATENDIMENTO</span><strong>Fazer uma venda <span aria-hidden="true">→</span></strong><span>Escolha as peças, informe o cliente e registre o pagamento.</span><span className="task-foot">{disponiveis} peças disponíveis na loja</span></button>
      <button className="task-card" onClick={() => go('triagem')}><span className="eyebrow">ENTRADA DE PEÇAS</span><strong>Receber doação <span aria-hidden="true">→</span></strong><span>Anote o que chegou e acompanhe a avaliação de cada peça.</span><span className="task-foot">{pendentes.length ? pendentes.length + ' peças aguardando avaliação' : 'Nenhuma avaliação pendente'}</span></button>
    </div>
    <div className="section-heading"><div><h2>O caminho das peças</h2><p>Da doação ao apoio à nossa associação.</p></div></div>
    <div className="piece-journey" aria-label="Situação das peças">
      <button onClick={() => go('triagem')}><span className="journey-step">01 · Recebimento</span><strong>{doacoes.length}</strong><span>doações registradas</span></button>
      <button onClick={() => go('triagem')}><span className="journey-step">02 · Avaliação</span><strong>{pendentes.length}</strong><span>peças para avaliar</span></button>
      <button onClick={() => go('produtos')}><span className="journey-step">03 · Na loja</span><strong>{disponiveis}</strong><span>peças para vender</span></button>
      <button onClick={() => go('relatorios')}><span className="journey-step">04 · Vendas</span><strong>{vendas.length}</strong><span>vendas registradas</span></button>
    </div>
    <div className="home-bottom">
      <div className="panel"><div className="section-heading"><h2>Próximas peças para avaliar</h2><button className="text-button" onClick={() => go('triagem')}>Ver triagem →</button></div>
        {pendentes.length === 0 ? <p className="empty">Tudo em dia. Novas doações aparecerão aqui.</p> : <ul className="pending-list">{[...pendentes].sort((a,b) => a.data_triagem.localeCompare(b.data_triagem)).slice(0,3).map(t => {const p=produtos.find(p=>p.id===t.produto_id); const d=doacoes.find(d=>d.id===t.doacao_id); return <li key={t.id}><span className="piece-tag" aria-hidden="true">{p?.tamanho || '—'}</span><span><strong>{p?.nome || 'Peça'}</strong><small>{d?.origem} · {formatDateBR(d?.data_recebimento || t.data_triagem)}</small></span><span className="badge-status-pending">Aguardando</span></li>})}</ul>}
      </div>
      <div className="panel today-panel"><p className="eyebrow">VENDAS DE HOJE</p><strong className="hero-value">{formatCurrency(recebidoHoje)}</strong><p>{vendas.filter(v=>v.data===hoje).length} vendas registradas hoje.</p><p className="note">Valor recebido nas vendas. Não representa o saldo físico do caixa.</p><button className="btn secondary" onClick={()=>go('relatorios')}>Consultar vendas e caixa →</button></div>
    </div>
  </section>
}
