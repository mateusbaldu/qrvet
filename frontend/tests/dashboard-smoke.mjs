import assert from 'node:assert/strict'
import { mkdir } from 'node:fs/promises'
import { chromium } from 'playwright'

const browser = await chromium.launch({ headless: true, ...(process.env.PLAYWRIGHT_CHANNEL ? { channel: process.env.PLAYWRIGHT_CHANNEL } : {}) })
const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } })
const base = process.env.TEST_BASE_URL || 'http://127.0.0.1:5173'
const errors = [], unexpected = [], admissionPages = new Set()
let role = 'VETERINARIO', fail = false
const admissions = Array.from({ length: 115 }, (_, index) => ({
  id: index + 1, pacienteId: index + 1, fotoVersao: index === 0 ? 'photo.jpg' : null, pacienteNome: index === 114 ? 'Érico' : ['Luna', 'Mel', 'Thor', 'Nina'][index % 4] + ' ' + (index + 1),
  baiaId: index + 1, baiaIdentificacao: 'A-' + (index + 1), veterinarioId: index % 2 ? 3 : 2,
  veterinarioNome: index % 2 ? 'Paulo Santos' : 'Marina Silva', status: 'ATIVA',
  entradaInternacao: new Date(Date.now() - index * 3600000).toISOString(), motivo: 'Observação',
}))
const bays = ['OCUPADA', 'OCUPADA', 'DISPONIVEL', 'MANUTENCAO'].map((status, i) => ({ id: i + 1, identificacao: 'A-' + i, status }))
const meals = [
  { id: 1, status: 'PENDENTE', jejumAtivo: false, horario: new Date(Date.now() - 3600000).toISOString() },
  { id: 2, status: 'PENDENTE', jejumAtivo: true, horario: new Date(Date.now() - 3600000).toISOString() },
  { id: 3, status: 'PENDENTE', jejumAtivo: false, horario: new Date(Date.now() + 3600000).toISOString() },
  { id: 4, status: 'CONCLUIDA', jejumAtivo: false, horario: new Date().toISOString() },
  { id: 5, status: 'CANCELADA', jejumAtivo: false, horario: new Date().toISOString() },
].map(item => ({ ...item, internacaoId: item.id, pacienteNome: admissions[item.id - 1].pacienteNome, baiaIdentificacao: 'A-' + item.id }))
page.on('pageerror', error => errors.push(error.message))
await page.addInitScript(() => {
  for (const role of ['VETERINARIO', 'RECEPCIONISTA']) localStorage.setItem('qrvet:onboarding:v1:2:' + role, JSON.stringify({ seen: true, step: 0 }))
})
function paged(items, url) {
  const page = Number(url.searchParams.get('page') || 0), size = Number(url.searchParams.get('size') || 12)
  return { items: items.slice(page * size, (page + 1) * size), page, size, total: items.length, pages: Math.ceil(items.length / size) }
}
await page.route('**/qrvet/api/v1/**', route => {
  const url = new URL(route.request().url()), path = url.pathname.split('/v1')[1]
  const send = (data, status = 200) => route.fulfill({ status, contentType: 'application/json', ...(status === 204 ? {} : { body: JSON.stringify(data) }) })
  if (path === '/auth/csrf') return send({ token: 'csrf', headerName: 'X-XSRF-TOKEN' })
  if (path === '/auth/refresh') return send({ accessToken: 'test' })
  if (path === '/auth/me') return send({ id: 2, name: 'Marina Silva', email: 'marina@example.com', role, active: true, confirmed: true })
  if (path === '/auth/heartbeat') return send(null, 204)
  if (path === '/internacoes/1/foto') return route.fulfill({ contentType: 'image/png', body: Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=', 'base64') })
  if (path === '/pacientes') return send({ ...paged([], url), total: 200 })
  if (path === '/tutores') return send({ ...paged([], url), total: 150 })
  if (path === '/baias') return send(paged(bays, url))
  if (path === '/alimentacao/agenda') return send(meals)
  if (path === '/internacoes/cuidados') return send(paged(admissions, url))
  if (path === '/internacoes/ativas') {
    admissionPages.add(Number(url.searchParams.get('page')))
    return fail ? send({ message: 'Não foi possível carregar as internações.' }, 503) : send(paged(admissions, url))
  }
  unexpected.push(path)
  return send({}, 404)
})
const stat = label => page.locator('.dashboard-stat').filter({ hasText: label }).locator('> strong')
const metric = label => page.locator('.dashboard-feeding-metric').filter({ hasText: label }).locator('strong')
try {
  await mkdir('tests/artifacts', { recursive: true })
  await page.goto(base + '/inicio')
  await stat('Internações ativas').getByText('115', { exact: true }).waitFor()
  assert.deepEqual([...admissionPages], [0, 1], 'Dashboard must load beyond the first admissions page')
  await page.getByRole('img', { name: 'Foto de Luna 1', exact: true }).waitFor()
  assert.equal(await stat('Baias disponíveis').innerText(), '1')
  await metric('Concluídas').getByText('1', { exact: true }).waitFor()
  assert.equal(await metric('Pendentes').innerText(), '3')
  assert.equal(await metric('Atrasadas').innerText(), '1', 'Fasting meals must not count as late')
  assert.equal(await page.locator('.dashboard-next-meals > a').count(), 2)
  assert.equal(await page.locator('.sidebar-nav').getByRole('link', { name: 'Equipe', exact: true }).count(), 0)
  const search = page.getByRole('searchbox', { name: 'Buscar paciente internado' })
  await search.fill('erico')
  await page.locator('.dashboard-patient-row').getByText('Érico', { exact: true }).waitFor()
  assert.equal(await page.locator('.dashboard-patient-row').count(), 1)
  await search.fill('Paulo')
  await page.getByRole('button', { name: 'Meus pacientes', exact: true }).click()
  await page.getByRole('heading', { name: 'Nenhum paciente nesta busca' }).waitFor()
  await search.fill('')
  assert.equal(await page.locator('.dashboard-patient-row').count(), 6)
  assert.equal(await page.locator('.dashboard-patient-row').filter({ hasText: 'Paulo Santos' }).count(), 0)
  await page.getByRole('button', { name: 'Meus pacientes', exact: true }).click()
  await page.screenshot({ path: 'tests/artifacts/dashboard-vet-desktop.png', fullPage: true, animations: 'disabled' })
  for (const width of [360, 768, 1280, 1440]) {
    await page.setViewportSize({ width, height: 900 })
    assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), 'Dashboard overflow at ' + width)
    if (width === 360) {
      await page.screenshot({ path: 'tests/artifacts/dashboard-vet-mobile.png', fullPage: true, animations: 'disabled' })
      await page.getByRole('button', { name: 'Abrir menu', exact: true }).click()
      await page.keyboard.press('Escape')
      assert.equal(await page.locator('.app-sidebar.open').count(), 0)
      assert(await page.getByRole('button', { name: 'Abrir menu', exact: true }).evaluate(el => el === document.activeElement))
    }
  }
  await page.getByRole('button', { name: 'Recolher menu lateral' }).click()
  assert.equal(await page.locator('.app-sidebar.collapsed').count(), 1)
  await page.getByRole('button', { name: 'Expandir menu lateral' }).click()
  await page.locator('.dashboard-feeding-metric').filter({ hasText: 'Atrasadas' }).click()
  await page.waitForURL('**/alimentacao?filtro=ATRASADA')
  assert.equal(await page.getByRole('button', { name: 'Atrasadas', exact: true }).getAttribute('aria-pressed'), 'true')
  await page.locator('.clinic-feeding-list > li').first().waitFor()
  assert.equal(await page.locator('.clinic-feeding-list > li').count(), 1)
  await page.goto(base + '/inicio')
  await stat('Internações ativas').getByText('115', { exact: true }).waitFor()
  fail = true
  await page.getByRole('button', { name: 'Atualizar painel', exact: true }).click()
  await page.getByRole('alert').getByText('Não foi possível carregar as internações.').waitFor()
  fail = false
  await page.getByRole('button', { name: 'Tentar novamente', exact: true }).click()
  await stat('Internações ativas').getByText('115', { exact: true }).waitFor()
  role = 'RECEPCIONISTA'
  await page.reload()
  await stat('Internações ativas').getByText('115', { exact: true }).waitFor()
  assert.equal(await page.locator('.dashboard-feeding').count(), 0)
  assert.equal(await page.getByRole('button', { name: 'Meus pacientes', exact: true }).count(), 0)
  assert.equal(await page.locator('.sidebar-nav').getByRole('link', { name: 'Cuidados', exact: true }).count(), 0)
  assert.deepEqual(unexpected, [])
  assert.deepEqual(errors, [])
  console.log('Dashboard: pagination, search, my patients, feeding/fasting counts, role access, retry, menu, desktop and mobile passed.')
} finally { await browser.close() }
