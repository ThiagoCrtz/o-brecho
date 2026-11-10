import React from 'react'
import { useBrecho } from '../../context/BrechoContext'
import { formatCurrency } from '../../utils/formatters'

export const ProdutosView: React.FC = () => {
  const { produtos,categorias,obterNomeCategoria,go,adicionarItemVenda,carrinhoIds,filtroProdutoTexto,setFiltroProdutoTexto,filtroProdutoCategoriaId,setFiltroProdutoCategoriaId }=useBrecho()
  const disponiveis=produtos.filter(p=>p.status==='disponivel')
  const filtrados=disponiveis.filter(p=>{
    const q=filtroProdutoTexto.trim().toLocaleLowerCase('pt-BR')
    return (!q || [p.nome,p.marca,p.tamanho,p.localizacao].some(v=>v.toLocaleLowerCase('pt-BR').includes(q))) && (filtroProdutoCategoriaId===null || p.categoria_id===filtroProdutoCategoriaId)
  })
  return <section className="page">
    <div className="page-head"><div><p className="eyebrow">PEÇAS APROVADAS · PRONTAS PARA VENDA</p><h1>Peças para vender</h1><p className="lead">Encontre a peça e adicione à venda. Você pode escolher mais de uma.</p></div><button className="btn" onClick={()=>go('nova-venda')}>{carrinhoIds.length ? 'Continuar venda ('+carrinhoIds.length+' peças)' : 'Fazer venda'} →</button></div>
    <div className="toolbar"><label htmlFor="busca-pecas">Procurar peça<input id="busca-pecas" type="search" placeholder="Nome, marca, tamanho ou localização" value={filtroProdutoTexto} onChange={e=>setFiltroProdutoTexto(e.target.value)}/></label><label htmlFor="categoria-pecas">Categoria<select id="categoria-pecas" value={filtroProdutoCategoriaId??''} onChange={e=>setFiltroProdutoCategoriaId(e.target.value?Number(e.target.value):null)}><option value="">Todas as categorias</option>{categorias.map(c=><option key={c.id} value={c.id}>{c.nome}</option>)}</select></label>{(filtroProdutoTexto || filtroProdutoCategoriaId!==null)&&<button className="btn secondary" onClick={()=>{setFiltroProdutoTexto('');setFiltroProdutoCategoriaId(null)}}>Limpar busca</button>}</div>
    <div className="section-heading"><p className="note" role="status">{filtrados.length} de {disponiveis.length} peças disponíveis</p><button className="text-button" onClick={()=>go('triagem')}>Chegou uma peça? Registrar doação →</button></div>
    <div className="product-grid">{filtrados.length===0?<div className="empty full"><h2>{disponiveis.length?'Nenhuma peça encontrada':'Ainda não há peças para vender'}</h2><p>{disponiveis.length?'Tente outro nome ou limpe os filtros.':'Registre uma doação e aguarde a aprovação na triagem.'}</p></div>:filtrados.map(p=><article key={p.id} className="product-card">
      <div className="product-card-top"><span className="piece-tag" aria-label={'Tamanho '+p.tamanho}>{p.tamanho}</span><div><span className="product-meta">{obterNomeCategoria(p.categoria_id)}</span><h2 className="product-name">{p.nome}</h2></div></div>
      <div className="product-specs"><span className="spec-chip">{p.marca}</span><span className="spec-chip">Estado: {p.estado}</span></div>
      {p.descricao&&<p className="product-meta">{p.descricao}</p>}
      <div className="product-location">Onde encontrar: <strong>{p.localizacao}</strong></div>
      <div className="price-box"><span className="note">Preço da peça</span><strong className="price">{formatCurrency(p.preco)}</strong></div>
      <button className={'btn '+(carrinhoIds.includes(p.id)?'secondary':'success')} onClick={()=>adicionarItemVenda(p.id)} disabled={carrinhoIds.includes(p.id)} aria-label={(carrinhoIds.includes(p.id)?'Já adicionada: ':'Adicionar à venda: ')+p.nome}>{carrinhoIds.includes(p.id)?'✓ Adicionada à venda':'+ Adicionar à venda'}</button>
    </article>)}</div>
  </section>
}
