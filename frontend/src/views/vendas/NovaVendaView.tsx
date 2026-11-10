import React, { useState } from 'react'
import { useBrecho } from '../../context/BrechoContext'
import { formatCurrency } from '../../utils/formatters'
import { formatPhone, validatePhone } from '../../utils/validators'
import { Modal } from '../../components/Modal'

export const NovaVendaView: React.FC = () => {
  const {produtos,categorias,obterNomeCategoria,carrinhoIds,precosNegociados,valorFinalNegociado,setValorFinalNegociado,alterarPrecoItemCarrinho,clientes,adicionarItemVenda,removerItemCarrinho,modoClienteAtual,setModoClienteAtual,clienteSelecionadoId,setClienteSelecionadoId,clienteAvulsoNome,setClienteAvulsoNome,clienteAvulsoNumero,setClienteAvulsoNumero,formaPagamento,setFormaPagamento,finalizarVenda}=useBrecho()
  const [busca,setBusca]=useState('')
  const [categoria,setCategoria]=useState('')
  const [erro,setErro]=useState('')
  const [revisar,setRevisar]=useState(false)
  const disponiveis=produtos.filter(p=>p.status==='disponivel')
  const pecas=produtos.filter(p=>carrinhoIds.includes(p.id))
  const soma=pecas.reduce((s,p)=>s+(precosNegociados[p.id]??p.preco),0)
  const total=valorFinalNegociado??soma
  const cliente=modoClienteAtual==='avulso'?clienteAvulsoNome:clientes.find(c=>c.id===clienteSelecionadoId)?.nome
  const filtradas=disponiveis.filter(p=>(!categoria||p.categoria_id===Number(categoria))&&[p.nome,p.marca,p.tamanho].some(v=>v.toLowerCase().includes(busca.trim().toLowerCase())))
  const conferir=()=>{
    setErro('')
    if(!pecas.length){setErro('Escolha pelo menos uma peça para continuar.');return}
    if(pecas.length!==carrinhoIds.length||pecas.some(p=>p.status!=='disponivel')){setErro('Uma peça não está disponível. Retire-a da venda para continuar.');return}
    if(!Number.isFinite(total)||total<=0||soma<=0){setErro('O total da venda deve ser maior que zero. Confira os preços.');return}
    if(!cliente?.trim()){setErro('Selecione um cliente ou informe o nome do novo cliente.');return}
    if(modoClienteAtual==='avulso'&&!validatePhone(clienteAvulsoNumero)){setErro('Informe o telefone completo do cliente, com DDD.');return}
    setRevisar(true)
  }
  return <section className="page">
    <div className="page-head"><div><p className="eyebrow">ATENDIMENTO NO BALCÃO</p><h1>Fazer venda</h1><p className="lead">Escolha as peças, informe o cliente e confira antes de registrar.</p></div><span className="badge">{pecas.length} peças na venda</span></div>
    <div className="sale-layout">
      <div className="panel"><div className="step-heading"><span className="step-badge">1</span><h2 style={{margin:0}}>Escolha as peças</h2></div>
        <div className="toolbar"><label htmlFor="venda-busca">Procurar peça<input id="venda-busca" type="search" placeholder="Nome, marca ou tamanho" value={busca} onChange={e=>setBusca(e.target.value)}/></label><label htmlFor="venda-categoria">Categoria<select id="venda-categoria" value={categoria} onChange={e=>setCategoria(e.target.value)}><option value="">Todas</option>{categorias.map(c=><option key={c.id} value={c.id}>{c.nome}</option>)}</select></label></div>
        <div className="sale-products">{filtradas.length===0?<div className="empty full">Nenhuma peça encontrada. Tente outra busca ou categoria.</div>:filtradas.map(p=><div key={p.id} className={'sale-item '+(carrinhoIds.includes(p.id)?'selected':'')}><div><strong>{p.nome}</strong><div className="product-meta">{obterNomeCategoria(p.categoria_id)} · Tam. {p.tamanho}<br/>{p.localizacao}</div><strong>{formatCurrency(p.preco)}</strong></div><button className="btn secondary small" disabled={carrinhoIds.includes(p.id)} onClick={()=>adicionarItemVenda(p.id)} aria-label={'Adicionar '+p.nome}>{carrinhoIds.includes(p.id)?'Adicionada':'+ Adicionar'}</button></div>)}</div>
      </div>
      <aside className="cart" aria-label="Resumo da venda">
        <h2>Peças escolhidas</h2>
        {!pecas.length?<p className="empty">As peças que você adicionar aparecerão aqui.</p>:pecas.map(p=><div className="cart-row" key={p.id} style={{alignItems:'flex-start',flexWrap:'wrap'}}><div style={{flex:'1 1 160px'}}><strong>{p.nome}</strong><p className="note" style={{margin:'4px 0'}}>Preço original: {formatCurrency(p.preco)}</p>{p.status!=='disponivel'&&<p className="field-error">Peça indisponível</p>}<label htmlFor={'preco-'+p.id}>Valor da peça (R$)<input id={'preco-'+p.id} type="number" inputMode="decimal" min="0" step="0.01" value={precosNegociados[p.id]??p.preco} onChange={e=>alterarPrecoItemCarrinho(p.id,Number(e.target.value))}/></label></div><button className="text-button" onClick={()=>removerItemCarrinho(p.id)} aria-label={'Retirar '+p.nome}>Retirar</button></div>)}
        <details style={{marginTop:16}}><summary style={{minHeight:44,cursor:'pointer'}}>Ajustar o total da compra</summary><p className="note">Use quando houver um valor combinado para todas as peças.</p><label htmlFor="total-negociado">Total combinado (R$)<input id="total-negociado" type="number" inputMode="decimal" min="0.01" step="0.01" value={valorFinalNegociado??''} placeholder={soma.toFixed(2)} onChange={e=>setValorFinalNegociado(e.target.value===''?null:Number(e.target.value))}/></label>{valorFinalNegociado!==null&&<button className="text-button" onClick={()=>setValorFinalNegociado(null)}>Voltar à soma das peças</button>}</details>
        <div className="cart-total"><span>Total da venda</span><strong>{formatCurrency(total)}</strong></div>
        <div className="customer-box"><div className="step-heading"><span className="step-badge">2</span><h2 style={{margin:0}}>Quem está comprando?</h2></div><div className="customer-mode">{(['cadastrado','avulso'] as const).map(m=><button key={m} className={'btn small '+(modoClienteAtual===m?'':'secondary')} aria-pressed={modoClienteAtual===m} onClick={()=>{setModoClienteAtual(m);setErro('')}}>{m==='cadastrado'?'Já tem cadastro':'Cliente novo'}</button>)}</div>
          {modoClienteAtual==='cadastrado'?<label htmlFor="venda-cliente">Selecione o cliente<select id="venda-cliente" value={clienteSelecionadoId??''} onChange={e=>setClienteSelecionadoId(e.target.value?Number(e.target.value):null)}><option value="">Escolha o cliente</option>{clientes.map(c=><option key={c.id} value={c.id}>{c.nome} · {c.telefone}</option>)}</select></label>:<div className="customer-choice"><label htmlFor="cliente-nome">Nome do cliente<input id="cliente-nome" autoComplete="name" value={clienteAvulsoNome} onChange={e=>setClienteAvulsoNome(e.target.value)}/></label><label htmlFor="cliente-telefone">Telefone com DDD<input id="cliente-telefone" type="tel" autoComplete="tel" placeholder="(15) 99999-9999" value={clienteAvulsoNumero} onChange={e=>setClienteAvulsoNumero(formatPhone(e.target.value))}/></label><p className="note">O novo cliente será cadastrado junto com a venda.</p></div>}
        </div>
        <div className="step-heading"><span className="step-badge">3</span><h2 style={{margin:0}}>Forma de pagamento</h2></div>
        <div className="actions" aria-label="Forma de pagamento">{['Pix','Dinheiro','Cartão'].map(m=><button key={m} className={'btn '+(formaPagamento===m?'':'secondary')} aria-pressed={formaPagamento===m} onClick={()=>setFormaPagamento(m)}>{m}</button>)}</div>
        {erro&&<p className="field-error" role="alert">{erro}</p>}
        <button className="btn success" style={{width:'100%',marginTop:24}} disabled={!pecas.length} onClick={conferir}>Conferir venda →</button>
      </aside>
    </div>
    {revisar&&<Modal label="Conferir venda antes de registrar" onClose={()=>setRevisar(false)}><div className="modal-content"><div className="modal-head"><h2>Está tudo certo?</h2><button className="btn-close" onClick={()=>setRevisar(false)}>Voltar</button></div><div className="modal-body"><p>Confira o recebimento antes de registrar. A venda dará saída nas peças do estoque.</p><div className="receipt-box"><div className="receipt-row"><span>Cliente</span><strong>{cliente}</strong></div><div className="receipt-row"><span>Pagamento</span><strong>{formaPagamento}</strong></div><div className="receipt-row"><span>Peças</span><strong>{pecas.length}</strong></div><ul>{pecas.map(p=><li key={p.id}>{p.nome}</li>)}</ul><div className="receipt-total-box"><span>Total a receber</span><strong>{formatCurrency(total)}</strong></div></div><p className="note">O sistema registra o pagamento; ele não cobra o cliente.</p></div><div className="modal-footer"><button className="btn secondary" onClick={()=>setRevisar(false)}>Voltar e corrigir</button><button className="btn success" onClick={()=>{if(finalizarVenda())setRevisar(false);else{setRevisar(false);setErro('A venda não foi registrada. Confira os dados e tente novamente.')}}}>Recebimento conferido. Registrar venda</button></div></div></Modal>}
  </section>
}
