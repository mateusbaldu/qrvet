import { createRequire } from 'node:module'
import assert from 'node:assert/strict'
import { mkdir } from 'node:fs/promises'
const require = createRequire(import.meta.url)
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright')
const browser = await chromium.launch({ ...(process.env.PLAYWRIGHT_CHANNEL ? { channel: process.env.PLAYWRIGHT_CHANNEL } : {}), headless: true })
const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } })
page.setDefaultTimeout(10000)
const errors = []
const unexpected = []
page.on('pageerror', error => errors.push(error.message))
let role = 'ADMIN'
let loggedIn = true
let fasting = false
let created = false
let status = 'ATIVA'
let sessionActive = true
let failure = ''
let expireNextRead = false
let invalidRefresh = false
const writes = []
const token = '12345678-1234-4123-8123-123456789012'
const patient = { id: 1, tutorId: 1, nome: 'Luna', especie: 'Felina', raca: 'SRD', sexo: 'Fêmea', peso: 4.5, dataNascimento: '2021-04-12', observacoes: 'Sensível a ruídos.' }
const patients = [patient, { ...patient, id: 2, nome: 'Thor', especie: 'Canina', raca: 'Golden Retriever', peso: 28 }]
const tutor = { id: 1, nome: 'Ana Silva', cpf: '12345678900', telefone: '11999999999', email: 'ana@example.com', endereco: 'Rua das Flores, 120 · São Paulo' }
const tutors = [tutor]
const bay = { id: 1, identificacao: 'A-01', status: 'DISPONIVEL', observacao: 'Ala de observação' }
const foods = []
const fasts = []
const user = () => ({ id: 2, name: 'Marina Silva', email: 'marina@example.com', role, active: true, confirmed: true, createdAt: '2026-01-14T12:00:00Z', updatedAt: '2026-09-21T12:00:00Z', lastActivityAt: '2026-09-21T12:00:00Z' })
const members = [{ ...user(), id: 2, role: 'VETERINARIO' }, ...Array.from({ length: 104 }, (_, i) => ({ ...user(), id: i + 3, name: 'Profissional ' + (i + 1), email: 'profissional' + i + '@example.com', role: i % 2 ? 'AUXILIAR_TECNICO' : 'RECEPCIONISTA' }))]
const admission = () => ({ id: 1, pacienteId: 1, pacienteNome: 'Luna', baiaId: 1, baiaIdentificacao: 'A-01', veterinarioId: 2, veterinarioNome: 'Marina Silva', uuidToken: token, entradaInternacao: '2026-09-21T12:00:00Z', saidaInternacao: status === 'ATIVA' ? null : '2026-09-21T17:00:00Z', motivo: 'Observação após procedimento', diagnosticoInicial: 'Recuperação pós-operatória', status, observacoes: 'Manter ambiente tranquilo.' })
const summary = () => ({ id: 1, pacienteNome: 'Luna', baiaIdentificacao: 'A-01', status })
const paged = (items, url) => {
  const current = Number(url.searchParams.get('page') || 0)
  const size = Number(url.searchParams.get('size') || 12)
  return { items: items.slice(current * size, (current + 1) * size), page: current, size, total: items.length, pages: Math.ceil(items.length / size) }
}
await page.route('**/qrvet/api/v1/**', async route => {
  const req = route.request()
  const url = new URL(req.url())
  const path = url.pathname.split('/v1')[1]
  const method = req.method()
  const send = (data, status = 200) => route.fulfill({ status, contentType: 'application/json', ...(status === 204 ? {} : { body: JSON.stringify(data) }) })
  if (expireNextRead && path === '/tutores') { expireNextRead = false; return send({ message: 'Invalid session' }, 401) }
  if (failure && method === 'POST' && path === '/tutores') return send({ message: failure }, 409)
  if (method !== 'GET') {
    assert.equal(req.headers()['x-xsrf-token'], 'csrf')
    const body = req.postData() ? req.postDataJSON() : undefined
    writes.push({ path, method, body })
    if (path === '/auth/refresh') {
      assert(!req.headers().authorization, 'Refresh must not send an expired bearer token')
      return loggedIn && !invalidRefresh ? send({ accessToken: 'test' }) : send({ message: 'Invalid session' }, 401)
    }
    if (path === '/auth/login') {
      if (body.password !== 'Valid-123!') return send({ message: 'Invalid credentials' }, 401)
      loggedIn = true; return send({ accessToken: 'test' })
    }
    if (path === '/auth/logout') { loggedIn = false; return send(null, 204) }
    if (path === '/auth/forgot-password') return send({ message: 'Sent' }, 202)
    if (path === '/auth/heartbeat' || path === '/auth/confirm-invitation' || path === '/auth/reset-password') return send(null, 204)
    if (path === '/auth/password') {
      if (body.currentPassword !== 'Valid-123!') return send({ message: 'Invalid current password' }, 401)
      loggedIn = false; return send(null, 204)
    }
    if (path === '/bootstrap/admin') return route.fulfill({ status: 201, body: '' })
    if (path === '/tutores') { const saved = { ...body, id: tutors.length + 1 }; tutors.push(saved); return send(saved, 201) }
    if (path === '/pacientes') { const saved = { ...body, id: patients.length + 1 }; patients.push(saved); return send(saved, 201) }
    if (path === '/users') { const saved = { ...body, id: 200, active: false, confirmed: false, createdAt: '2026-09-21T12:00:00Z' }; members.push(saved); return send(saved, 201) }
    if (path.endsWith('/invitation')) return send(null, 204)
    if (path === '/internacoes') { created = true; bay.status = 'OCUPADA'; return send(admission(), 201) }
    if (path === '/internacoes/1/jejum') { fasting = true; const item = { id: fasts.length + 1, motivo: body.motivo, ativo: true, dataHoraInicio: '2026-09-21T14:00:00Z', dataHoraFim: null }; fasts.unshift(item); return send(item, 201) }
    if (path === '/internacoes/1/jejum/encerrar') { fasting = false; fasts[0].ativo = false; fasts[0].dataHoraFim = '2026-09-21T14:30:00Z'; return send(fasts[0]) }
    if (path === '/internacoes/1/alimentacao') { const item = { ...body, id: foods.length + 1, dataHoraRegistro: '2026-09-21T15:00:00Z', usuarioId: 2 }; foods.unshift(item); return send(item, 201) }
    if (path === '/internacoes/1/encerrar') { status = body.statusEncerramento; bay.status = 'DISPONIVEL'; fasting = false; return send(admission()) }
    if (path === '/baias/1/status' || path === '/baias/1') { Object.assign(bay, body); return send(bay) }
    if (path.includes('/sessions')) { sessionActive = false; return send(null, 204) }
  } else {
    if (path === '/auth/csrf') return send({ token: 'csrf', headerName: 'X-XSRF-TOKEN' })
    if (path === '/auth/me') return loggedIn ? send(user()) : send({}, 401)
    if (path === '/users') return send(paged(members, url))
    if (path.includes('/sessions')) return send(paged(sessionActive ? [{ jti: 'session-1', createdAt: '2026-09-21T12:00:00Z', expiresAt: '2026-09-26T12:00:00Z' }] : [], url))
    if (path === '/tutores') return send(paged(tutors, url))
    if (path === '/pacientes') return send(paged(patients, url))
    if (/^\/pacientes\/\d+$/.test(path)) return send(patients.find(p => p.id === Number(path.split('/').pop())) ?? {}, patients.some(p => p.id === Number(path.split('/').pop())) ? 200 : 404)
    if (path === '/baias') return send(paged([bay], url))
    if (path === '/baias/1') return send(bay)
    if (path === '/internacoes/veterinarios') return send([{ id: 2, name: 'Marina Silva' }])
    if (path === '/internacoes/cuidados') return send(paged(created && status === 'ATIVA' ? [summary()] : [], url))
    if (path === '/internacoes/1/cuidados') return send(summary())
    if (path === '/internacoes/ativas') return send(paged(created && status === 'ATIVA' ? [admission()] : [], url))
    if (path === '/internacoes') return send(paged(created && (!url.searchParams.has('pacienteId') || url.searchParams.get('pacienteId') === '1') ? [admission()] : [], url))
    if (path.endsWith('/qrcode')) return send({ uuidToken: token, url: 'http://localhost:5173/public/internacoes/qr/' + token, base64: 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=' })
    if (path.endsWith('/jejum')) return send(fasts)
    if (path.endsWith('/alimentacao')) return send(foods)
    if (path.includes('/public/')) {
      assert(!req.headers().authorization, 'Public QR must not send bearer tokens')
      return send({ pacienteNome: 'Luna', especie: 'Felina', raca: 'SRD', sexo: 'Fêmea', baiaIdentificacao: 'A-01', motivo: 'Observação após procedimento', status, jejumAtivo: fasting, entradaInternacao: '2026-09-21T12:00:00Z' })
    }
    if (path === '/internacoes/1') return send(admission())
  }
  unexpected.push(method + ' ' + path)
  return send({ message: 'Unexpected request' }, 500)
})
const base = process.env.TEST_BASE_URL || 'http://127.0.0.1:5173'
const visit = path => page.goto(base + path, { waitUntil: 'domcontentloaded' })
const fill = async values => { for (const [name, value] of Object.entries(values)) await page.locator('[name="' + name + '"]').fill(value) }
const dialog = () => page.getByRole('dialog')
const save = async (name = 'Salvar') => { await dialog().getByRole('button', { name, exact: true }).click(); await dialog().waitFor({ state: 'detached' }) }
const shot = name => page.screenshot({ path: 'tests/artifacts/' + name + '.png', fullPage: true })
await mkdir('tests/artifacts', { recursive: true })
try {
  await visit('/inicio')
  await page.getByText('Nenhum paciente internado no momento.').waitFor()
  await shot('dashboard-empty')
  await visit('/tutores')
  await page.getByRole('button', { name: 'Novo tutor', exact: true }).click()
  await fill({ nome: 'Maria Costa', cpf: '98765432100', telefone: '11988888888', email: 'maria@example.com', endereco: 'Rua B' })
  failure = 'User with this email already exists.'
  await dialog().getByRole('button', { name: 'Salvar', exact: true }).click()
  await dialog().getByRole('alert').waitFor()
  assert.equal(await page.locator('[name="nome"]').inputValue(), 'Maria Costa')
  failure = ''
  await save()
  await page.getByRole('heading', { name: 'Maria Costa' }).waitFor()
  await visit('/pacientes')
  await page.getByRole('button', { name: 'Cadastrar paciente', exact: true }).click()
  await fill({ nome: 'Mel', especie: 'Canina', raca: 'Shih-tzu', dataNascimento: '2022-01-01', peso: '6.4', observacoes: 'Acompanhamento anual' })
  await page.locator('[name="tutorId"]').selectOption('1')
  await page.locator('[name="sexo"]').selectOption('Fêmea')
  await shot('patient-form')
  await save()
  await page.waitForURL('**/pacientes/3')
  assert(writes.some(w => w.path === '/pacientes' && w.body.peso === 6.4 && w.body.tutorId === 1))
  await visit('/pacientes/1')
  await page.getByRole('link', { name: 'Abrir internação', exact: true }).click()
  await dialog().waitFor()
  await page.locator('[name="baiaId"]').selectOption('1')
  await page.locator('[name="veterinarioId"]').selectOption('2')
  await fill({ motivo: 'Observação após procedimento', diagnosticoInicial: 'Recuperação pós-operatória' })
  await save('Abrir internação')
  await page.waitForURL('**/internacoes/1')
  assert(writes.some(w => w.path === '/internacoes' && w.body.pacienteId === 1))
  await page.getByRole('button', { name: 'Iniciar jejum', exact: true }).click()
  await fill({ motivo: 'Exame' })
  await save()
  await page.getByText('Jejum ativo. Não oferecer alimento até o encerramento do jejum.').waitFor()
  assert(await page.getByRole('button', { name: 'Registrar alimentação', exact: true }).isDisabled())
  await page.getByRole('button', { name: 'Encerrar jejum', exact: true }).click()
  await save()
  await page.getByRole('button', { name: 'Registrar alimentação', exact: true }).click()
  await fill({ alimento: 'Ração úmida', quantidade: '40 g', aceitacaoObservacao: 'Boa aceitação' })
  await save()
  await page.getByRole('heading', { name: 'Alimentação registrada', exact: true }).waitFor()
  await shot('hospitalization')
  await visit('/inicio')
  await page.getByText('Luna', { exact: true }).waitFor()
  await shot('dashboard')
  await visit('/pacientes')
  await page.getByRole('link', { name: 'Ver detalhes de Luna' }).waitFor()
  await shot('patients')
  await page.getByPlaceholder('Buscar por paciente ou tutor').fill('ana')
  assert.equal(await page.locator('tbody tr').count(), 3)
  await page.getByRole('button', { name: 'Internados', exact: true }).click()
  assert.equal(await page.locator('tbody tr').count(), 1)
  await page.getByRole('link', { name: 'Ver detalhes de Luna' }).click()
  await page.getByRole('heading', { name: 'Histórico de internações' }).waitFor()
  await shot('patient-detail')
  await visit('/equipe')
  await page.locator('.clinic-stat').filter({ hasText: 'Total de membros' }).getByText('105', { exact: true }).waitFor()
  await shot('team')
  await page.getByPlaceholder('Nome ou e-mail').fill('Profissional 104')
  await page.getByText('Profissional 104', { exact: true }).waitFor()
  await page.getByRole('button', { name: 'Convidar membro', exact: true }).click()
  await fill({ name: 'Carlos Souza', email: 'carlos@example.com' })
  await page.locator('[name="role"]').selectOption('AUXILIAR_TECNICO')
  await save('Enviar convite')
  await page.getByPlaceholder('Nome ou e-mail').fill('Carlos')
  await page.getByRole('button', { name: 'Reenviar convite para Carlos Souza' }).click()
  await save('Reenviar convite')
  await visit('/sessoes?usuario=2')
  await page.getByRole('button', { name: 'Encerrar sessão', exact: true }).click()
  await save('Confirmar encerramento')
  await page.getByRole('heading', { name: 'Nenhuma sessão ativa' }).waitFor()
  await visit('/internacoes/1')
  await page.getByRole('button', { name: 'Encerrar internação', exact: true }).click()
  await page.locator('[name="statusEncerramento"]').selectOption('ALTA')
  await save('Confirmar encerramento')
  await page.getByText('Alta · #1').waitFor()
  assert.equal(await page.getByRole('button', { name: 'Registrar alimentação', exact: true }).count(), 0)
  await visit('/baias')
  await page.getByRole('button', { name: 'Alterar situação' }).click()
  await page.locator('[name="status"]').selectOption('MANUTENCAO')
  await save()
  await page.getByText('Situação da baia atualizada.').waitFor()
  role = 'RECEPCIONISTA'
  await visit('/internacoes/1')
  await page.getByRole('heading', { name: 'Luna', exact: true }).waitFor()
  assert.equal(await page.getByRole('heading', { name: 'Alimentação e jejum' }).count(), 0)
  await visit('/sessoes')
  await page.waitForURL('**/meu-perfil')
  role = 'AUXILIAR_TECNICO'
  await visit('/cuidados')
  await page.getByText('Consultar histórico pelo código').click()
  await page.getByLabel('Código da internação').fill('1')
  await page.getByRole('button', { name: 'Consultar', exact: true }).click()
  await page.getByText('Internação encerrada. Histórico disponível para consulta.').waitFor()
  assert.equal(await page.getByRole('button', { name: 'Registrar alimentação', exact: true }).count(), 0)
  await visit('/consultar-qr')
  await page.locator('[name="token"]').fill('inválido')
  await page.getByRole('button', { name: 'Consultar internação' }).click()
  await page.getByRole('alert').waitFor()
  await page.locator('[name="token"]').fill('https://clinic.example/public/internacoes/qr/' + token + '?origem=qr#paciente')
  await page.getByRole('button', { name: 'Consultar internação' }).click()
  await page.waitForURL('**/public/internacoes/qr/' + token)
  await page.setViewportSize({ width: 390, height: 844 })
  await page.getByRole('heading', { name: 'Luna', exact: true }).waitFor()
  assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth))
  await shot('public-mobile')
  role = 'ADMIN'
  for (const path of ['/inicio', '/pacientes', '/equipe', '/internacoes/1', '/meu-perfil']) {
    await visit(path)
    await page.locator('.clinic-state').waitFor({ state: 'detached' })
    assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), 'Mobile overflow: ' + path)
  }
  await visit('/pacientes')
  await page.getByRole('button', { name: 'Abrir menu' }).click()
  await page.getByRole('navigation').getByRole('link', { name: 'Tutores', exact: true }).click()
  await page.waitForURL('**/tutores')
  assert.equal(await page.locator('.app-sidebar.open').count(), 0)
  await page.setViewportSize({ width: 1440, height: 1000 })
  expireNextRead = true
  await visit('/tutores')
  await page.getByRole('heading', { name: 'Ana Silva' }).waitFor()
  invalidRefresh = true; expireNextRead = true
  await page.getByRole('button', { name: 'Novo tutor', exact: true }).click()
  await page.keyboard.press('Escape')
  assert.equal(await dialog().count(), 0)
  await visit('/pacientes')
  await page.waitForURL('**/login')
  invalidRefresh = false; loggedIn = false
  await visit('/login')
  await shot('login')
  await page.getByLabel('Seu e-mail').fill('marina@example.com')
  await page.getByLabel('Sua senha', { exact: true }).fill('wrong')
  await page.getByRole('button', { name: 'Entrar na minha conta' }).click()
  await page.getByText('E-mail ou senha inválidos.').waitFor()
  await page.getByLabel('Sua senha', { exact: true }).fill('Valid-123!')
  await page.getByRole('button', { name: 'Entrar na minha conta' }).click()
  await page.waitForURL('**/inicio')
  await visit('/meu-perfil')
  await page.getByLabel('Senha atual', { exact: true }).fill('wrong')
  await page.getByLabel('Nova senha', { exact: true }).fill('NewValid-123!')
  await page.getByLabel('Confirmar nova senha').fill('NewValid-123!')
  await page.getByRole('button', { name: 'Alterar senha', exact: true }).click()
  await page.getByText('A senha atual está incorreta.').waitFor()
  assert(page.url().endsWith('/meu-perfil'))
  await page.getByLabel('Senha atual', { exact: true }).fill('Valid-123!')
  await page.getByRole('button', { name: 'Alterar senha', exact: true }).click()
  await page.waitForURL('**/login')
  assert.deepEqual(unexpected, [])
  assert.deepEqual(errors, [])
  console.log('PASS: dashboard, patient/tutor creation and validation, admission, fasting, feeding, discharge, public QR, search, >100 team members, invitation/resend, session revocation, all role guards, mobile overflow, auth refresh/expiry, password change; no uncaught browser errors.')
} catch (error) {
  await shot('failure')
  console.error('Page:', page.url(), '\nUnexpected requests:', unexpected, '\nBrowser errors:', errors)
  throw error
} finally { await browser.close() }

