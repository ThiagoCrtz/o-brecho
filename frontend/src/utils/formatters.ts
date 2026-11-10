export function formatCurrency(valor: number): string {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(Number.isFinite(valor) ? valor : 0)
}

export function todayLocal(): string {
  const parts = new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Sao_Paulo', year: 'numeric', month: '2-digit', day: '2-digit' }).formatToParts(new Date())
  const value = (type: string) => parts.find(p => p.type === type)?.value
  return [value('year'), value('month'), value('day')].join('-')
}

export function formatDateBR(isoDate: string): string {
  if (!isoDate || !isoDate.includes('-')) return isoDate
  return isoDate.slice(0,10).split('-').reverse().join('/')
}
