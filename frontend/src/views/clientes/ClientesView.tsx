import { Modal } from '../../components/Modal'
import React, { useState, useMemo } from 'react'
import { useBrecho } from '../../context/BrechoContext'
import { Cliente } from '../../types'
import { formatPhone, validatePhone, validateEmail } from '../../utils/validators'

export const ClientesView: React.FC = () => {
  const { clientes, vendas, abrirCliente, cadastrarCliente, atualizarCliente, excluirCliente } = useBrecho()

  const [busca, setBusca] = useState('')
  const [modalFormAberto, setModalFormAberto] = useState(false)
  const [clienteEmEdicao, setClienteEmEdicao] = useState<Cliente | null>(null)
  const [clienteParaExcluir, setClienteParaExcluir] = useState<Cliente | null>(null)

  // Campos estritos da tabela CLIENTE { id, nome, telefone, email, observacoes }
  const [nome, setNome] = useState('')
  const [telefone, setTelefone] = useState('')
  const [email, setEmail] = useState('')
  const [observacoes, setObservacoes] = useState('')
  const [erroForm, setErroForm] = useState('')

  const clientesFiltrados = useMemo(() => {
    const q = busca.toLowerCase().trim()
    return clientes.filter(c => {
      return (
        !q ||
        c.nome.toLowerCase().includes(q) ||
        (q.replace(/\D/g, '').length > 0 && c.telefone.replace(/\D/g, '').includes(q.replace(/\D/g, ''))) ||
        c.email.toLowerCase().includes(q) ||
        c.observacoes.toLowerCase().includes(q)
      )
    })
  }, [clientes, busca])

  const handleNovoCliente = () => {
    setClienteEmEdicao(null)
    setNome('')
    setTelefone('')
    setEmail('')
    setObservacoes('')
    setErroForm('')
    setModalFormAberto(true)
  }

  const handleEditarCliente = (c: Cliente, e: React.MouseEvent) => {
    e.stopPropagation()
    setClienteEmEdicao(c)
    setNome(c.nome)
    setTelefone(c.telefone)
    setEmail(c.email)
    setObservacoes(c.observacoes)
    setErroForm('')
    setModalFormAberto(true)
  }

  const handleSubmitForm = (e: React.FormEvent) => {
    e.preventDefault()
    setErroForm('')

    if (!nome.trim()) {
      setErroForm('Faltou digitar o nome do cliente.')
      return
    }

    if (!telefone.trim() || !validatePhone(telefone)) {
      setErroForm('Faltou digitar o telefone completo com DDD.')
      return
    }

    if (email.trim() && !validateEmail(email)) {
      setErroForm('O e-mail digitado não é válido.')
      return
    }

    const dadosCliente = {
      nome: nome.trim(),
      telefone: telefone.trim(),
      email: email.trim(),
      observacoes: observacoes.trim()
    }

    if (clienteEmEdicao) {
      if (!atualizarCliente(clienteEmEdicao.id, dadosCliente)) { setErroForm('Confira os dados antes de salvar.'); return }
    } else {
      if (!cadastrarCliente(dadosCliente)) { setErroForm('Confira os dados antes de salvar.'); return }
    }

    setModalFormAberto(false)
  }

  return (
    <section className="page active">
      <div className="page-head">
        <div>
          <h1>Clientes</h1>
          <p className="lead">
            Procure um cliente pelo nome, telefone ou e-mail, ou cadastre uma pessoa nova.
          </p>
        </div>
        <div className="actions">
          <button className="btn success" onClick={handleNovoCliente}>
            Cadastrar Novo Cliente
          </button>
        </div>
      </div>

      <div className="toolbar">
        <input
          type="text"
          aria-label="Procurar cliente" placeholder="Digite o nome, telefone ou e-mail para procurar..."
          value={busca}
          onChange={e => setBusca(e.target.value)}
          style={{ maxWidth: '480px' }}
        />
        {busca && (
          <button className="btn secondary" onClick={() => setBusca('')}>
            Limpar Busca
          </button>
        )}
      </div>

      <div className="table-wrap" tabIndex={0} role="region" aria-label="Tabela de registros. Use as setas para ver todas as colunas.">
        <table>
          <thead>
            <tr>
              <th>Nome</th>
              <th>Telefone</th>
              <th>E-mail</th>
              <th>Observações</th>
              <th>Compras</th>
              <th style={{ textAlign: 'right' }}>Opções</th>
            </tr>
          </thead>
          <tbody>
            {clientesFiltrados.length === 0 ? (
              <tr>
                <td colSpan={6} className="empty">
                  Nenhum cliente encontrado.
                </td>
              </tr>
            ) : (
              clientesFiltrados.map(c => {
                // Calculado por relacionamento 1:N com VENDA (VENDA.cliente_id)
                const vendasDoCliente = vendas.filter(v => v.cliente_id === c.id)
                const totalGasto = vendasDoCliente.reduce((acc, v) => acc + v.valor_total, 0)

                return (
                  <tr
                    key={c.id}
                    className="client-row"
                    onClick={() => abrirCliente(c.id)}
                  >
                    <td>
                      <strong style={{ fontSize: '18px' }}>{c.nome}</strong>
                    </td>
                    <td>{c.telefone}</td>
                    <td>{c.email || '—'}</td>
                    <td style={{ color: 'var(--muted)' }}>{c.observacoes || '—'}</td>
                    <td>
                      <strong>{vendasDoCliente.length}</strong> (R$ {totalGasto.toFixed(2).replace('.', ',')})
                    </td>
                    <td style={{ textAlign: 'right' }} onClick={e => e.stopPropagation()}>
                      <div style={{ display: 'inline-flex', gap: '8px' }}>
                        <button
                          className="btn secondary small"
                          onClick={() => abrirCliente(c.id)}
                        >
                          Ver Compras
                        </button>
                        <button
                          className="btn secondary small"
                          onClick={e => handleEditarCliente(c, e)}
                        >
                          Editar
                        </button>
                        <button
                          className="btn danger small"
                          onClick={e => {
                            e.stopPropagation()
                            setClienteParaExcluir(c)
                          }}
                        >
                          Apagar
                        </button>
                      </div>
                    </td>
                  </tr>
                )
              })
            )}
          </tbody>
        </table>
      </div>

      {modalFormAberto && (
        <Modal label="Dados do cliente" onClose={() => setModalFormAberto(false)}>
          <div className="modal-content" style={{ maxWidth: '520px' }}>
            <div className="modal-head">
              <h2>{clienteEmEdicao ? 'Editar Cliente' : 'Cadastrar Novo Cliente'}</h2>
              <button className="btn-close" onClick={() => setModalFormAberto(false)} type="button">
                Fechar
              </button>
            </div>
            <form onSubmit={handleSubmitForm}>
              <div className="modal-body">
                <div className="form-group">
                  <label htmlFor="clientesview-1">Nome *</label>
                  <input id="clientesview-1"
                    type="text"
                    value={nome}
                    onChange={e => setNome(e.target.value)}
                    placeholder="Ex: Maria da Silva"
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="clientesview-2">Telefone *</label>
                  <input id="clientesview-2"
                    type="text"
                    value={telefone}
                    onChange={e => setTelefone(formatPhone(e.target.value))}
                    placeholder="(11) 99999-9999"
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="clientesview-3">E-mail</label>
                  <input id="clientesview-3"
                    type="email"
                    value={email}
                    onChange={e => setEmail(e.target.value)}
                    placeholder="cliente@email.com"
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="clientesview-4">Observações</label>
                  <textarea id="clientesview-4"
                    rows={3}
                    value={observacoes}
                    onChange={e => setObservacoes(e.target.value)}
                    placeholder="Preferências de tamanho, observações do cliente..."
                  />
                </div>

                {erroForm && <div className="field-error" role="alert">{erroForm}</div>}
              </div>
              <div className="modal-footer">
                <button type="button" className="btn secondary" onClick={() => setModalFormAberto(false)}>
                  Cancelar
                </button>
                <button type="submit" className="btn success">
                  Salvar Cliente
                </button>
              </div>
            </form>
          </div>
        </Modal>
      )}

      {clienteParaExcluir && (
        <Modal label="Apagar cliente" onClose={() => setClienteParaExcluir(null)}>
          <div className="modal-content" style={{ maxWidth: '420px' }}>
            <div className="modal-head">
              <h2>Apagar Cliente?</h2>
              <button className="btn-close" onClick={() => setClienteParaExcluir(null)} type="button">
                Fechar
              </button>
            </div>
            <div className="modal-body">
              <p style={{ fontSize: '18px' }}>
                Tem certeza que quer apagar <strong>{clienteParaExcluir.nome}</strong>?
              </p>
            </div>
            <div className="modal-footer">
              <button type="button" className="btn secondary" onClick={() => setClienteParaExcluir(null)}>
                Não, Voltar
              </button>
              <button
                type="button"
                className="btn danger"
                onClick={() => {
                  excluirCliente(clienteParaExcluir.id)
                  setClienteParaExcluir(null)
                }}
              >
                Sim, Apagar
              </button>
            </div>
          </div>
        </Modal>
      )}
    </section>
  )
}
