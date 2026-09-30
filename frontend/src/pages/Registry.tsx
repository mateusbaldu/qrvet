import { useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { ArrowRight, PawPrint, Plus, Search, UserRound } from 'lucide-react'
import { useAuth } from '../auth/useAuth'
import { useResource } from '../hooks/useResource'
import { Editor, EmptyState, Notice, Page, Pagination, ResourceState, StatusBadge, type Field } from '../components/ClinicUI'
import { request } from '../services/api'
import { allPages, clinicApi, dateOnly, dateTime, paginate, searchText, type Baia, type Internacao, type Paciente, type Tutor } from '../services/clinic'

export function Registry({ kind }: { kind: 'tutores' | 'pacientes' | 'baias' }) {
  const { user } = useAuth()
  const [params] = useSearchParams()
  const navigate = useNavigate()
  const [page, setPage] = useState(0)
  const [filter, setFilter] = useState('')
  const [search, setSearch] = useState('')
  const [success, setSuccess] = useState('')
  const [editing, setEditing] = useState<Baia | 'new' | null>(null)
  const [statusEdit, setStatusEdit] = useState<Baia | null>(null)
  const tutorId = params.get('tutor')
  const resource = useResource(async () => {
    const items = await allPages<Tutor | Paciente | Baia>('/' + kind)
    const [tutors, admissions] = kind === 'pacientes' ? await Promise.all([allPages<Tutor>('/tutores'), allPages<Internacao>('/internacoes/ativas')]) : [[], []]
    return { items, tutors, admissions }
  }, kind)
  const title = { tutores: 'Tutores', pacientes: 'Pacientes', baias: 'Baias' }[kind]
  const editable = kind !== 'baias' || user?.role === 'ADMIN'
  const [today] = useState(() => new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 10))
  const fields: Field[] = kind === 'tutores' ? [
    { name: 'nome', label: 'Nome completo', maxLength: 150, wide: true, autoComplete: 'name' },
    { name: 'cpf', label: 'CPF', maxLength: 14 }, { name: 'telefone', label: 'Telefone', type: 'tel', maxLength: 20, autoComplete: 'tel' },
    { name: 'email', label: 'E-mail', type: 'email', maxLength: 150, autoComplete: 'email' }, { name: 'endereco', label: 'Endereço', maxLength: 255, wide: true, autoComplete: 'street-address' },
  ] : kind === 'pacientes' ? [
    { name: 'nome', label: 'Nome do paciente', maxLength: 100 },
    { name: 'tutorId', label: 'Tutor responsável', type: 'number', value: tutorId ?? '', options: (resource.data?.tutors ?? []).map(t => ({ value: t.id, label: t.nome + ' · ' + t.cpf })) },
    { name: 'especie', label: 'Espécie', maxLength: 50 }, { name: 'raca', label: 'Raça', maxLength: 50 },
    { name: 'sexo', label: 'Sexo', options: [{ value: 'Macho', label: 'Macho' }, { value: 'Fêmea', label: 'Fêmea' }, { value: 'Não identificado', label: 'Não identificado' }] },
    { name: 'dataNascimento', label: 'Data de nascimento', type: 'date', max: today },
    { name: 'peso', label: 'Peso (kg)', type: 'number', min: '0.01', max: '9999.99', step: '0.01' },
    { name: 'observacoes', label: 'Observações', type: 'textarea', required: false },
  ] : [
    { name: 'identificacao', label: 'Identificação', maxLength: 50, value: editing && editing !== 'new' ? editing.identificacao : '' },
    { name: 'observacao', label: 'Observação', required: false, maxLength: 255, value: editing && editing !== 'new' ? editing.observacao ?? '' : '' },
    ...(editing === 'new' ? [{ name: 'status', label: 'Situação inicial', value: 'DISPONIVEL', options: [{ value: 'DISPONIVEL', label: 'Disponível' }, { value: 'MANUTENCAO', label: 'Manutenção' }] }] : []),
  ]
  const filtered = resource.data?.items.filter(item => {
    const term = searchText(search.trim())
    if ('identificacao' in item) return (!filter || item.status === filter) && searchText(item.identificacao + ' ' + (item.observacao ?? '')).includes(term)
    if ('especie' in item) {
      const admitted = resource.data!.admissions.some(i => i.pacienteId === item.id)
      const tutor = resource.data!.tutors.find(t => t.id === item.tutorId)
      return (!tutorId || item.tutorId === Number(tutorId)) && (!filter || (filter === 'ATIVA' ? admitted : !admitted)) && searchText([item.nome, item.especie, item.raca, tutor?.nome].join(' ')).includes(term)
    }
    return searchText([item.nome, item.cpf, item.email, item.telefone].join(' ')).includes(term)
  }) ?? []
  const result = paginate(filtered, page)
  const canCreate = editable && !!resource.data && (kind !== 'pacientes' || !!resource.data.tutors.length)
  return <Page title={title} description={{ tutores: 'Os responsáveis que confiam seu cuidado à nossa clínica.', pacientes: 'Cada paciente, uma história. Encontre todas elas aqui.', baias: 'Organize os espaços e acompanhe a ocupação da clínica.' }[kind]} action={editable && <button className="clinic-primary" disabled={!canCreate} onClick={() => { setSuccess(''); setEditing('new') }}><Plus size={17} />{kind === 'baias' ? 'Nova baia' : kind === 'tutores' ? 'Novo tutor' : 'Cadastrar paciente'}</button>}>
    {success && <Notice>{success}</Notice>}
    {editing && <Editor key={typeof editing === 'string' ? editing : editing.id} title={editing === 'new' ? kind === 'pacientes' ? 'Cadastrar paciente' : kind === 'tutores' ? 'Cadastrar tutor' : 'Nova baia' : 'Editar baia'} description="Preencha os dados abaixo. Os campos com * são obrigatórios." fields={fields} onCancel={() => setEditing(null)} onSave={async data => {
      const created = await clinicApi.save<Paciente | Tutor | Baia>(editing === 'new' ? '/' + kind : '/baias/' + editing.id, data, editing === 'new' ? 'POST' : 'PUT')
      if (kind === 'pacientes' && created.id) navigate('/pacientes/' + created.id)
      else { setSuccess('Cadastro salvo com sucesso.'); resource.reload() }
    }} />}
    {statusEdit && <Editor title={'Alterar situação · ' + statusEdit.identificacao} fields={[{ name: 'status', label: 'Situação', value: statusEdit.status, options: [{ value: 'DISPONIVEL', label: 'Disponível' }, { value: 'MANUTENCAO', label: 'Manutenção' }] }]} onCancel={() => setStatusEdit(null)} onSave={async data => { await clinicApi.save('/baias/' + statusEdit.id + '/status', data, 'PATCH'); setSuccess('Situação da baia atualizada.'); resource.reload() }} />}
    <div className="clinic-toolbar clinic-panel"><label className="clinic-search"><span>Buscar</span><span className="clinic-search-input"><Search size={18} /><input type="search" value={search} onChange={e => { setSearch(e.target.value); setPage(0) }} placeholder={kind === 'pacientes' ? 'Buscar por paciente ou tutor' : kind === 'tutores' ? 'Nome, CPF, telefone ou e-mail' : 'Identificação da baia'} /></span></label>
      {kind !== 'tutores' && <div className="clinic-filters" role="group" aria-label="Filtrar por situação">{(kind === 'pacientes' ? [['', 'Todos'], ['ATIVA', 'Internados'], ['SEM', 'Sem internação ativa']] : [['', 'Todas'], ['DISPONIVEL', 'Disponíveis'], ['OCUPADA', 'Ocupadas'], ['MANUTENCAO', 'Manutenção']]).map(([value, label]) => <button key={value} className={filter === value ? 'selected' : ''} aria-pressed={filter === value} onClick={() => { setFilter(value); setPage(0) }}>{label}</button>)}</div>}
      {tutorId && <Link to="/pacientes">Ver todos os pacientes</Link>}
    </div>
    <ResourceState {...resource} />
    {resource.data && <>
      {kind === 'pacientes' && !resource.data.tutors.length && <p className="clinic-notice">Cadastre um tutor antes de adicionar o primeiro paciente. <Link to="/tutores">Ir para tutores →</Link></p>}
      {kind === 'pacientes' ? <section className="clinic-table-panel"><div className="clinic-table-scroll"><table className="clinic-table"><thead><tr><th>Paciente</th><th>Tutor</th><th>Situação</th><th>Peso</th><th><span className="visually-hidden">Ações</span></th></tr></thead><tbody>{result.items.map(item => {
        const p = item as Paciente
        const admission = resource.data!.admissions.find(i => i.pacienteId === p.id)
        return <tr key={p.id}><td><Link className="clinic-identity" to={'/pacientes/' + p.id}><span className="clinic-avatar"><PawPrint size={20} /></span><span><strong>{p.nome}</strong><small>{p.especie} · {p.raca}</small></span></Link></td><td>{resource.data!.tutors.find(t => t.id === p.tutorId)?.nome ?? '—'}</td><td><StatusBadge status={admission ? 'ATIVA' : 'NEUTRAL'}>{admission ? 'Internado' : 'Sem internação ativa'}</StatusBadge></td><td>{p.peso.toLocaleString('pt-BR')} kg</td><td><Link className="clinic-row-link" aria-label={'Ver detalhes de ' + p.nome} to={'/pacientes/' + p.id}><ArrowRight size={18} /></Link></td></tr>
      })}</tbody></table></div>{!result.total && <EmptyState><p>Tente outro nome ou filtro para encontrar o paciente.</p></EmptyState>}</section> : <div className="clinic-grid">{result.items.map(item => <article className="clinic-card" key={item.id}>{'identificacao' in item ? <><StatusBadge status={item.status} /><h2>{item.identificacao}</h2><p>{item.observacao || 'Sem observações.'}</p>{editable && <div className="clinic-actions"><button onClick={() => setEditing(item)}>Editar</button><button disabled={item.status === 'OCUPADA'} title={item.status === 'OCUPADA' ? 'A baia será liberada ao encerrar a internação.' : undefined} onClick={() => setStatusEdit(item)}>Alterar situação</button></div>}</> : 'cpf' in item ? <><span className="clinic-avatar"><UserRound size={22} /></span><h2>{item.nome}</h2><dl className="clinic-details"><div><dt>CPF</dt><dd>{item.cpf}</dd></div><div><dt>Telefone</dt><dd>{item.telefone}</dd></div></dl><p>{item.email}<br />{item.endereco}</p><Link to={'/pacientes?tutor=' + item.id}>Ver pacientes →</Link></> : null}</article>)}</div>}
      {kind !== 'pacientes' && !result.total && <EmptyState><p>Os novos cadastros aparecerão aqui.</p></EmptyState>}
      <Pagination {...result} onChange={setPage} />
    </>}
  </Page>
}

export function PatientDetail() {
  const { id = '' } = useParams()
  const resource = useResource(async () => {
    const [patient, tutors, history] = await Promise.all([request<Paciente>('/pacientes/' + encodeURIComponent(id)), allPages<Tutor>('/tutores'), allPages<Internacao>('/internacoes?pacienteId=' + encodeURIComponent(id))])
    return { patient, tutor: tutors.find(t => t.id === patient.tutorId), history }
  }, id)
  const data = resource.data
  const active = data?.history.find(i => i.status === 'ATIVA')
  return <Page title={data?.patient.nome ?? 'Detalhes do paciente'} description="Identificação, responsável e histórico de internações." action={<Link className="clinic-button" to="/pacientes">Voltar aos pacientes</Link>}>
    <ResourceState {...resource} />
    {data && <><section className="clinic-patient-banner"><span className="clinic-avatar large"><PawPrint size={30} /></span><div><h2>{data.patient.nome}</h2><p>{data.patient.especie} · {data.patient.raca} · {data.patient.sexo}</p></div><StatusBadge status={active ? 'ATIVA' : 'NEUTRAL'}>{active ? 'Internado' : 'Sem internação ativa'}</StatusBadge><Link className="clinic-button clinic-primary" to={active ? '/internacoes/' + active.id : '/internacoes?paciente=' + id}>{active ? 'Acompanhar internação' : 'Abrir internação'}<ArrowRight size={16} /></Link></section>
      <div className="clinic-detail-grid"><div><section className="clinic-panel"><h2>Informações do paciente</h2><dl className="clinic-details"><div><dt>Data de nascimento</dt><dd>{dateOnly(data.patient.dataNascimento)}</dd></div><div><dt>Peso</dt><dd>{data.patient.peso.toLocaleString('pt-BR')} kg</dd></div><div><dt>Identificação</dt><dd>#{data.patient.id}</dd></div></dl><h3>Observações</h3><p className="clinic-prewrap">{data.patient.observacoes || 'Nenhuma observação registrada.'}</p></section>
        <section className="clinic-panel"><div className="clinic-section-heading"><h2>Histórico de internações</h2><span className="clinic-badge">{data.history.length} registros</span></div>{data.history.length ? data.history.map(i => <article className="clinic-history" key={i.id}><StatusBadge status={i.status} /><h3>{i.motivo}</h3><p>Entrada: {dateTime(i.entradaInternacao)}{i.saidaInternacao && <> · Saída: {dateTime(i.saidaInternacao)}</>}</p><Link to={'/internacoes/' + i.id}>Ver internação #{i.id} →</Link></article>) : <EmptyState title="Uma nova história começa aqui"><p>Este paciente ainda não possui internações.</p></EmptyState>}</section>
      </div><aside><section className="clinic-panel"><span className="clinic-avatar"><UserRound size={22} /></span><h2>Tutor responsável</h2>{data.tutor ? <><h3>{data.tutor.nome}</h3><dl className="clinic-details single"><div><dt>Telefone</dt><dd>{data.tutor.telefone}</dd></div><div><dt>E-mail</dt><dd>{data.tutor.email}</dd></div><div><dt>Endereço</dt><dd>{data.tutor.endereco}</dd></div></dl><Link to={'/pacientes?tutor=' + data.tutor.id}>Ver pacientes deste tutor →</Link></> : <p>Tutor não encontrado.</p>}</section></aside></div>
    </>}
  </Page>
}

