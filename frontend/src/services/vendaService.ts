import { Produto, Venda, ItemVenda, Pagamento } from '../models'
import { todayLocal } from '../utils/formatters'

export const VendaService = {
  calcularTotais(
    pecasCarrinho: Produto[],
    precosNegociados: Record<number, number>,
    valorFinalNegociado: number | null
  ) {
    const somaOriginal = pecasCarrinho.reduce((s, p) => s + p.preco, 0)
    const somaItens = pecasCarrinho.reduce(
      (s, p) => s + (precosNegociados[p.id] !== undefined ? precosNegociados[p.id] : p.preco),
      0
    )
    const total =
      valorFinalNegociado !== null && Number.isFinite(valorFinalNegociado) && valorFinalNegociado >= 0
        ? valorFinalNegociado
        : somaItens

    return { somaOriginal, somaItens, total }
  },

  gerarEntidadesVenda(params: {
    clienteId: number
    funcionarioId: number
    formaPagamento: string
    pecasCarrinho: Produto[]
    precosNegociados: Record<number, number>
    total: number
  }): {
    novaVenda: Venda
    novosItensVenda: ItemVenda[]
    novoPagamento: Pagamento
    data: string
    vendaId: number
  } {
    if (!params.pecasCarrinho.length || new Set(params.pecasCarrinho.map(p => p.id)).size !== params.pecasCarrinho.length || params.pecasCarrinho.some(p => p.status !== 'disponivel')) {
      throw new Error('Confira as peças: todas precisam estar disponíveis e sem repetição.')
    }
    if (!['Pix', 'Dinheiro', 'Cartão'].includes(params.formaPagamento)) throw new Error('Escolha a forma de pagamento.')
    const precos = params.pecasCarrinho.map(p => Math.round((params.precosNegociados[p.id] ?? p.preco) * 100))
    const totalCentavos = Math.round(params.total * 100)
    if (!Number.isSafeInteger(totalCentavos) || totalCentavos <= 0 || precos.some(p => !Number.isSafeInteger(p) || p < 0)) throw new Error('Informe valores válidos. O total da venda deve ser maior que zero.')
    const somaCentavos = precos.reduce((s, p) => s + p, 0)
    if (!somaCentavos) throw new Error('Informe o preço de pelo menos uma peça.')
    const data = todayLocal()
    const vendaId = Date.now()

    const novaVenda: Venda = {
      id: vendaId,
      cliente_id: params.clienteId,
      funcionario_id: params.funcionarioId,
      data,
      valor_total: totalCentavos / 100
    }

    let acumulado = 0
    let distribuido = 0
    const novosItensVenda: ItemVenda[] = params.pecasCarrinho.map((p, idx) => {
      acumulado += precos[idx]
      // Distribuir em centavos mantém itens, comprovante e pagamento com o mesmo total.
      const parcelaAcumulada = Math.round(totalCentavos * (acumulado / somaCentavos))
      const precoUnit = (parcelaAcumulada - distribuido) / 100
      distribuido = parcelaAcumulada
      return {
        id: vendaId + idx + 1,
        venda_id: vendaId,
        produto_id: p.id,
        quantidade: 1,
        preco_unitario: precoUnit,
        subtotal: precoUnit
      }
    })

    const novoPagamento: Pagamento = {
      id: vendaId + 500,
      venda_id: vendaId,
      metodo: params.formaPagamento,
      valor: totalCentavos / 100,
      data
    }

    return { novaVenda, novosItensVenda, novoPagamento, data, vendaId }
  }
}
