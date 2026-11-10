import { Funcionario } from '../models'

export function isGestor(usuario: Funcionario | null): boolean {
  return !!usuario && ['administrador', 'gestor', 'gestora'].includes(usuario.cargo.trim().toLowerCase())
}

export function podeAprovarTriagem(usuario: Funcionario | null): boolean {
  return isGestor(usuario) || usuario?.cargo.trim().toLowerCase() === 'responsável pela triagem'
}
