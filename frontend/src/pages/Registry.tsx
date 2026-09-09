import { useResource } from '../hooks/useResource'
import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { Editor, Page, Pagination, ResourceState, type Field } from '../components/ClinicUI'
import { allPages, clinicApi, labels, type Baia, type Paciente, type Tutor } from '../services/clinic'

export function Registry({ kind }: { kind: 'tutores' | 'pacientes' | 'baias' }) {
  const { user } = useAuth()
  const [params] = useSearchParams()
  const [page, setPage] = useState(0)
  const [today] = useState(() => new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0,10))
  const [filter, setFilter] = useState('')
  const [editing, setEditing] = useState<Baia | 'new' | null>(null)
  const [statusEdit, setStatusEdit] = useState<Baia | null>(null)
  const tutorId = params.get('tutor')
  const path = kind === 'pacientes' && tutorId ? `/pacientes/tutor/${encodeURIComponent(tutorId)}` : `/${kind}`
  const filters = kind === 'tutores' ? `&nome=${encodeURIComponent(filter)}` : kind === 'baias' && filter ? `&status=${filter}` : ''
  const resource = useResource(() => clinicApi.list<Tutor | Paciente | Baia>(path, page, filters), `${path}:${page}:${filters}`)
  const tutors = useResource(() => kind === 'pacientes' ? allPages<Tutor>('/tutores') : Promise.resolve([]), kind)
  const title = { tutores: 'Tutores', pacientes: 'Pacientes', baias: 'Baias' }[kind]
  const editable = kind !== 'baias' || user?.role === 'ADMIN'
  const fields: Field[] = kind === 'tutores' ? [
    { name:'nome', label:'Nome completo', maxLength:150 }, { name:'cpf', label:'CPF', maxLength:14 },
    { name:'telefone', label:'Telefone', type:'tel', maxLength:20 }, { name:'email', label:'E-mail', type:'email', maxLength:150 }, { name:'endereco', label:'Endereço', maxLength:255 },
  ] : kind === 'pacientes' ? [
    { name:'tutorId', label:'Tutor responsável', type:'number', value:tutorId ?? '', options:(tutors.data ?? []).map(t => ({ value:t.id, label:`${t.nome} · ${t.cpf}` })) },
    { name:'nome', label:'Nome do paciente', maxLength:100 }, { name:'especie', label:'Espécie', maxLength:50 }, { name:'raca', label:'Raça', maxLength:50 },
    { name:'sexo', label:'Sexo', options:[{value:'Macho',label:'Macho'},{value:'Fêmea',label:'Fêmea'}] },
    { name:'dataNascimento', label:'Data de nascimento', type:'date', max:today },
    { name:'peso', label:'Peso (kg)', type:'number', min:'0.01', max:'9999.99', step:'0.01' }, { name:'observacoes', label:'Observações', type:'textarea', required:false },
  ] : [ { name:'identificacao', label:'Identificação', maxLength:50, value:editing && editing !== 'new' ? editing.identificacao : '' }, { name:'observacao', label:'Observação', required:false, maxLength:255, value:editing && editing !== 'new' ? editing.observacao ?? '' : '' }, ...(editing === 'new' ? [{ name:'status', label:'Situação inicial', value:'DISPONIVEL', options:[{value:'DISPONIVEL',label:'Disponível'},{value:'MANUTENCAO',label:'Manutenção'}] }] : []) ]
  return <Page title={title} description={{tutores:'Cadastre os responsáveis e encontre seus pacientes.',pacientes:'Informações e identificação dos animais atendidos.',baias:'Acompanhe a disponibilidade dos espaços da clínica.'}[kind]} action={editable && <button className="clinic-primary" onClick={() => setEditing('new')}>+ {kind === 'baias' ? 'Nova baia' : kind === 'tutores' ? 'Novo tutor' : 'Novo paciente'}</button>}>
    {kind === 'pacientes' && <ResourceState {...tutors} />}
    {editing && <Editor key={typeof editing === 'string' ? editing : editing.id} title={editing === 'new' ? 'Novo cadastro' : 'Editar baia'} fields={fields} onCancel={() => setEditing(null)} onSave={async data => { await clinicApi.save(editing === 'new' ? `/${kind}` : `/baias/${editing.id}`, data, editing === 'new' ? 'POST' : 'PUT'); resource.reload() }} />}
    {statusEdit && <Editor title={`Alterar situação · ${statusEdit.identificacao}`} fields={[{name:'status',label:'Situação',value:statusEdit.status,options:[{value:'DISPONIVEL',label:'Disponível'},{value:'MANUTENCAO',label:'Manutenção'}]}]} onCancel={() => setStatusEdit(null)} onSave={async data => { await clinicApi.save(`/baias/${statusEdit.id}/status`,data,'PATCH'); resource.reload() }} />}
    <div className="clinic-toolbar">{kind === 'tutores' && <label>Buscar por nome<input type="search" value={filter} onChange={e => {setFilter(e.target.value);setPage(0)}} placeholder="Nome do tutor" /></label>}{kind === 'baias' && <label>Situação<select value={filter} onChange={e => {setFilter(e.target.value);setPage(0)}}><option value="">Todas</option>{['DISPONIVEL','OCUPADA','MANUTENCAO'].map(s => <option key={s} value={s}>{labels[s]}</option>)}</select></label>}{tutorId && <Link to="/pacientes">Ver todos os pacientes</Link>}</div>
    <ResourceState {...resource} />
    {resource.data && <><div className="clinic-grid">{resource.data.items.map(item => <article className="clinic-card" key={item.id}>{'identificacao' in item ? <><span className="clinic-badge">{labels[item.status]}</span><h2>{item.identificacao}</h2><p>{item.observacao || 'Sem observações.'}</p>{editable && <div className="clinic-actions"><button onClick={() => setEditing(item)}>Editar</button><button disabled={item.status === 'OCUPADA'} onClick={() => setStatusEdit(item)}>Alterar situação</button></div>}</> : 'especie' in item ? <><span className="clinic-badge">{item.especie}</span><h2>{item.nome}</h2><p>{item.raca} · {item.sexo} · {item.peso} kg</p><p>Tutor: {tutors.data?.find(t => t.id === item.tutorId)?.nome ?? `#${item.tutorId}`}</p><p>Nascimento: {item.dataNascimento.split('-').reverse().join('/')}</p><p>{item.observacoes || 'Sem observações.'}</p><Link to={`/internacoes?paciente=${item.id}`}>Abrir internação →</Link></> : <><span className="clinic-badge">Tutor #{item.id}</span><h2>{item.nome}</h2><p>CPF: {item.cpf}</p><p>{item.telefone}<br />{item.email}<br />{item.endereco}</p><Link to={`/pacientes?tutor=${item.id}`}>Ver pacientes →</Link></>}</article>)}</div>{resource.data.items.length === 0 && <p className="clinic-state">Nenhum registro encontrado.</p>}<Pagination {...resource.data} onChange={setPage} /></>}
  </Page>
}


