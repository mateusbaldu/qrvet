import type { Field } from '../components/ClinicUI'
import { request } from './api'
import type { Paciente } from './clinic'

export const tutorFields: Field[] = [
  { name: 'nome', label: 'Nome completo do tutor', maxLength: 150, wide: true, autoComplete: 'name' },
  { name: 'cpf', label: 'CPF', maxLength: 14 }, { name: 'telefone', label: 'Telefone', type: 'tel', maxLength: 20, autoComplete: 'tel' },
  { name: 'email', label: 'E-mail', type: 'email', maxLength: 150, autoComplete: 'email' },
  { name: 'endereco', label: 'Endereço', maxLength: 255, wide: true, autoComplete: 'street-address' },
]

export async function uploadPhoto(id: number, file: File) {
  const body = new FormData(); body.append('foto', file)
  return request<Paciente>('/pacientes/' + id + '/foto', { method: 'POST', body })
}

