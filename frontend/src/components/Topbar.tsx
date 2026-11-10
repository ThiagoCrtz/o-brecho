import React from 'react'
import { useBrecho } from '../context/BrechoContext'
import { PaginaApp } from '../types'

export const Topbar: React.FC = () => {
  const { usuarioLogado, logout, paginaAtual, go, triagens, carrinhoIds } = useBrecho()
  if (!usuarioLogado) return null
  const links: { pagina: PaginaApp; nome: string; numero: string; extra?: number }[] = [
    { pagina: 'inicio', nome: 'Início', numero: '⌂' },
    { pagina: 'triagem', nome: 'Doações e triagem', numero: '01', extra: triagens.filter(t => t.status === 'Pendente').length },
    { pagina: 'produtos', nome: 'Peças para vender', numero: '02' },
    { pagina: 'nova-venda', nome: 'Fazer venda', numero: '03', extra: carrinhoIds.length },
    { pagina: 'estoque', nome: 'Consultar estoque', numero: '04' },
    { pagina: 'clientes', nome: 'Clientes', numero: '05' },
    { pagina: 'relatorios', nome: 'Vendas e caixa', numero: '06' },
    { pagina: 'atendentes', nome: 'Equipe', numero: '07' }
  ]
  return <header className="topbar">
    <button className="logo-wrap" onClick={() => go('inicio')} aria-label="O Brechó — ir para o início">
      <span className="logo-badge">ANG</span>
      <span><span className="logo">O Brechó</span><span className="logo-sub">Associação Nova Geração</span></span>
    </button>
    <p className="nav-caption">Dia a dia do brechó</p>
    <nav className="nav" aria-label="Menu principal">
      {links.map(link => {
        const ativo = paginaAtual === link.pagina || (link.pagina === 'clientes' && paginaAtual === 'cliente-detalhe') || (link.pagina === 'triagem' && paginaAtual === 'cadastro-produto')
        return <button key={link.pagina} className={ativo ? 'active' : ''} onClick={() => go(link.pagina)} aria-current={ativo ? 'page' : undefined}>
          <span className="nav-number" aria-hidden="true">{link.numero}</span><span>{link.nome}</span>
          {!!link.extra && <span className="nav-count">{link.extra}</span>}
        </button>
      })}
    </nav>
    <div className="sidebar-note"><strong>Cada peça faz diferença.</strong><span>As vendas apoiam o trabalho da ANG.</span></div>
    <div className="user-area"><div className="user-pill"><span className="user-avatar" aria-hidden="true">{usuarioLogado.nome.charAt(0)}</span><span><strong>{usuarioLogado.nome.split(' ')[0]}</strong><span className="user-role">{usuarioLogado.cargo}</span></span></div><button className="btn secondary small" onClick={logout}>Sair</button></div>
  </header>
}
