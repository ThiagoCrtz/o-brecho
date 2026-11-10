import React, { useEffect, useRef } from 'react'
import { BrechoProvider, useBrecho } from './context/BrechoContext'
import { Login, Topbar, FeedbackUI } from './components'
import {
  InicioView,
  ClientesView,
  ClienteDetalheView,
  ProdutosView,
  CadastroProdutoView,
  NovaVendaView,
  EstoqueView,
  RelatoriosView,
  AtendentesView,
  TriagemView
} from './views'

const AppMain: React.FC = () => {
  const { usuarioLogado, paginaAtual } = useBrecho()

  const main = useRef<HTMLElement>(null)
  useEffect(() => {
    const heading = main.current?.querySelector('h1')
    if (heading) { heading.tabIndex = -1; heading.focus({ preventScroll: true }) }
  }, [paginaAtual, usuarioLogado?.id])

  if (!usuarioLogado) {
    return <Login />
  }

  return (
    <div className="app">
      <a href="#conteudo" className="skip-link">Pular para o conteúdo</a>
      <Topbar />
      <FeedbackUI />
      <main id="conteudo" ref={main} className="main" tabIndex={-1}>
        {paginaAtual === 'inicio' && <InicioView />}
        {paginaAtual === 'triagem' && <TriagemView />}
        {paginaAtual === 'clientes' && <ClientesView />}
        {paginaAtual === 'cliente-detalhe' && <ClienteDetalheView />}
        {paginaAtual === 'produtos' && <ProdutosView />}
        {paginaAtual === 'cadastro-produto' && <CadastroProdutoView />}
        {paginaAtual === 'nova-venda' && <NovaVendaView />}
        {paginaAtual === 'estoque' && <EstoqueView />}
        {paginaAtual === 'relatorios' && <RelatoriosView />}
        {paginaAtual === 'atendentes' && <AtendentesView />}
        <p className="note" style={{marginTop:40,borderTop:'1px solid var(--line)',paddingTop:16}}>Versão local · Os registros ficam salvos neste navegador.</p>
      </main>
    </div>
  )
}

export function App() {
  return (
    <BrechoProvider>
      <AppMain />
    </BrechoProvider>
  )
}

export default App
