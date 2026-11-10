import React from 'react'
import { useBrecho } from '../../context/BrechoContext'

export const CadastroProdutoView: React.FC = () => {
  const { go } = useBrecho()
  return <section className="page"><h1>Receber uma peça</h1><div className="panel"><h2>Toda peça começa com uma doação</h2><p>Registre quem doou e os dados da peça. Depois da avaliação e aprovação de um gestor, ela ficará disponível para venda.</p><button className="btn" onClick={()=>go('triagem')}>Ir para doações e triagem →</button></div></section>
}
