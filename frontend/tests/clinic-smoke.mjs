import { createRequire } from 'node:module'
import assert from 'node:assert/strict'
const require = createRequire(import.meta.url)
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright')
const browser = await chromium.launch({channel:'msedge',headless:true})
const page = await browser.newPage()
const errors = []
page.on('pageerror', error => errors.push(error.message))
let role = 'ADMIN'
let fasting = false
let status = 'ATIVA'
const writes = []
const token = '12345678-1234-4123-8123-123456789012'
const patient = {id:1,tutorId:1,nome:'Luna',especie:'Canina',raca:'SRD',sexo:'Fêmea',peso:12,dataNascimento:'2020-01-01',observacoes:''}
const tutor = {id:1,nome:'Ana Silva',cpf:'12345678900',telefone:'11999999999',email:'ana@example.com',endereco:'Rua A'}
const bay = {id:1,identificacao:'B01',status:'DISPONIVEL',observacao:''}
const admission = () => ({id:1,pacienteId:1,baiaId:1,veterinarioId:2,uuidToken:token,entradaInternacao:'2026-09-09T12:00:00Z',saidaInternacao:null,motivo:'Observação',diagnosticoInicial:'Avaliação',status,observacoes:''})
const user = () => ({id:2,name:'Equipe Teste',email:'test@example.com',role,active:true,confirmed:true,createdAt:'2026-09-09',updatedAt:'2026-09-09'})
const paged = items => ({items,page:0,size:12,total:items.length,pages:1})
await page.route('**/qrvet/api/v1/**',async route => {
  const req = route.request()
  const url = new URL(req.url())
  const path = url.pathname.split('/v1')[1]
  const method = req.method()
  let data
  if (method !== 'GET') {
    writes.push({path,method,body:req.postDataJSON()})
    if (path.endsWith('/jejum')) fasting = true
    if (path.endsWith('/jejum/encerrar')) fasting = false
    if (path === '/internacoes/1/encerrar') status = req.postDataJSON().statusEncerramento
    data = path === '/auth/refresh' ? {accessToken:'test'} : path === '/internacoes' || path.endsWith('/encerrar') ? admission() : {}
  } else if (path === '/auth/csrf') data = {token:'csrf',headerName:'X-XSRF-TOKEN'}
  else if (path === '/auth/me') data = user()
  else if (path === '/users') data = paged([{...user(),role:'VETERINARIO'}])
  else if (path.includes('/sessions')) data = paged([{jti:'session-1',createdAt:'2026-09-09T12:00:00Z',expiresAt:'2026-09-10T12:00:00Z'}])
  else if (path === '/tutores') data = paged([tutor])
  else if (path.startsWith('/pacientes')) data = paged([patient])
  else if (path === '/baias') data = paged([bay])
  else if (path === '/baias/1') data = bay
  else if (path === '/internacoes/ativas') data = paged([admission()])
  else if (path.endsWith('/qrcode')) data = {uuidToken:token,url:`http://localhost:5173/public/internacoes/qr/${token}`,base64:''}
  else if (path.endsWith('/jejum')) data = fasting ? [{id:1,motivo:'Exame',ativo:true,dataHoraInicio:'2026-09-09T12:00:00Z',dataHoraFim:null}] : []
  else if (path.endsWith('/alimentacao')) data = []
  else if (path.includes('/public/')) data = {pacienteNome:'Luna',especie:'Canina',raca:'SRD',sexo:'Fêmea',baiaIdentificacao:'B01',motivo:'Observação',status,jejumAtivo:fasting,entradaInternacao:'2026-09-09T12:00:00Z'}
  else if (path === '/internacoes/1') data = admission()
  else throw new Error(`Unhandled API: ${method} ${path}`)
  await route.fulfill({status:200,contentType:'application/json',body:JSON.stringify(data)})
})
const visit = path => page.goto(`http://127.0.0.1:5173${path}`)
try {
  await visit('/tutores')
  await page.getByRole('heading',{name:'Ana Silva'}).waitFor()
  await page.getByRole('button',{name:'+ Novo tutor'}).click()
  for (const [name,value] of Object.entries({nome:'Maria',cpf:'12345678900',telefone:'11999999999',email:'maria@example.com',endereco:'Rua B'})) await page.locator(`[name="${name}"]`).fill(value)
  await page.getByRole('button',{name:'Salvar',exact:true}).click()
  await page.getByRole('heading',{name:'Novo cadastro'}).waitFor({state:'detached'})
  assert(writes.some(w=>w.path==='/tutores' && w.body.nome==='Maria'))
  await visit('/pacientes')
  await page.getByRole('heading',{name:'Luna'}).waitFor()
  await page.getByRole('link',{name:'Abrir internação →'}).click()
  await page.locator('[name="baiaId"]').selectOption('1')
  await page.locator('[name="veterinarioId"]').selectOption('2')
  await page.locator('[name="motivo"]').fill('Observação')
  await page.locator('[name="diagnosticoInicial"]').fill('Avaliação')
  await page.getByRole('button',{name:'Salvar',exact:true}).click()
  await page.waitForURL('**/internacoes/1')
  assert(writes.some(w=>w.path==='/internacoes' && w.body.pacienteId===1 && w.body.baiaId===1))
  await page.getByRole('button',{name:'Iniciar jejum',exact:true}).click()
  await page.locator('[name="motivo"]').fill('Exame')
  await page.getByRole('button',{name:'Salvar',exact:true}).click()
  await page.getByText('Jejum ativo. Não oferecer alimento até o encerramento do jejum.').waitFor()
  assert(await page.getByRole('button',{name:'Registrar alimentação',exact:true}).isDisabled())
  await page.getByRole('button',{name:'Encerrar jejum',exact:true}).click()
  await page.getByRole('button',{name:'Salvar',exact:true}).click()
  await page.getByRole('button',{name:'Iniciar jejum',exact:true}).waitFor()
  await page.getByRole('button',{name:'Registrar alimentação',exact:true}).click()
  await page.locator('[name="alimento"]').fill('Ração')
  await page.locator('[name="quantidade"]').fill('100 g')
  await page.getByRole('button',{name:'Salvar',exact:true}).click()
  await page.getByRole('heading',{name:'Registrar alimentação',exact:true}).waitFor({state:'detached'})
  assert(writes.some(w=>w.path==='/internacoes/1/alimentacao' && w.body.quantidade==='100 g'))
  await page.getByRole('button',{name:'Encerrar internação',exact:true}).click()
  await page.locator('[name="statusEncerramento"]').selectOption('ALTA')
  await page.getByRole('button',{name:'Confirmar encerramento'}).click()
  await page.getByText('Alta · #1').waitFor()
  assert.equal(await page.getByRole('button',{name:'Registrar alimentação',exact:true}).count(),0)
  await visit('/baias')
  await page.getByRole('button',{name:'Alterar situação'}).click()
  await page.locator('[name="status"]').selectOption('MANUTENCAO')
  await page.getByRole('button',{name:'Salvar',exact:true}).click()
  await page.getByRole('heading',{name:'Alterar situação · B01'}).waitFor({state:'detached'})
  assert(writes.some(w=>w.path==='/baias/1/status' && w.method==='PATCH'))
  await visit('/sessoes')
  await page.getByLabel('Membro da equipe').selectOption('2')
  await page.getByRole('button',{name:'Encerrar sessão',exact:true}).click()
  await page.getByRole('button',{name:'Confirmar encerramento'}).click()
  await page.getByRole('heading',{name:'Encerrar esta sessão?'}).waitFor({state:'detached'})
  assert(writes.some(w=>w.path==='/users/2/sessions/session-1' && w.method==='DELETE'))
  role = 'RECEPCIONISTA'
  await visit('/internacoes/1')
  await page.getByRole('heading',{name:'Luna',exact:true}).waitFor()
  assert.equal(await page.getByRole('heading',{name:'Alimentação e jejum'}).count(),0)
  await visit('/sessoes')
  await page.waitForURL('**/meu-perfil')
  role = 'AUXILIAR_TECNICO'
  await visit('/cuidados')
  await page.getByLabel('Código da internação').fill('1')
  await page.getByRole('button',{name:'Consultar',exact:true}).click()
  await page.getByRole('heading',{name:'Histórico de alimentação'}).waitFor()
  await page.setViewportSize({width:390,height:844})
  await visit(`/public/internacoes/qr/${token}`)
  await page.getByRole('heading',{name:'Luna',exact:true}).waitFor()
  assert(await page.evaluate(()=>document.documentElement.scrollWidth <= innerWidth))
  await page.screenshot({path:'tests/public-mobile.png',fullPage:true})
  assert.deepEqual(errors,[])
  console.log('PASS: tutor creation, admission payload, fasting/feeding, discharge, bay status, session revocation, role guards, public mobile layout; no browser errors.')
} finally { await browser.close() }
