import assert from 'node:assert/strict'
import { mkdir } from 'node:fs/promises'
import { chromium } from 'playwright'

const browser = await chromium.launch({ headless: true, channel: process.env.PLAYWRIGHT_CHANNEL || 'msedge' })
const page = await browser.newPage({ viewport: { width: 1280, height: 900 }, timezoneId: 'America/Sao_Paulo' })
page.setDefaultTimeout(10000)
await page.clock.setFixedTime(new Date('2026-11-15T15:00:00Z'))
await page.addInitScript(() => localStorage.setItem('qrvet:onboarding:1:ADMIN', 'done'))
const base = process.env.TEST_BASE_URL || 'http://127.0.0.1:5173'
const errors = [], unexpected = [], writes = []
let fail = false
const admin = { id: 1, name: 'Admin', email: 'admin@test.local', role: 'ADMIN', active: true, confirmed: true, createdAt: '2026-01-01T12:00:00Z' }
const member = { ...admin, id: 2, name: 'Marina', email: 'marina@test.local', role: 'VETERINARIO' }
const meal = { id: 1, internacaoId: 1, pacienteNome: 'Luna', baiaIdentificacao: 'B1', internacaoStatus: 'ATIVA', status: 'PENDENTE', alimento: 'Ração', quantidade: '40 g', horario: '2026-11-15T17:00:00Z', jejumAtivo: true }
const paged = items => ({ items, page: 0, size: 100, total: items.length, pages: 1 })
page.on('pageerror', e => errors.push(e.message))
await page.route('**/qrvet/api/v1/**', async route => {
  const req = route.request(), url = new URL(req.url()), path = url.pathname.split('/v1')[1]
  const send = (body, status = 200) => route.fulfill({ status, contentType: 'application/json', ...(status === 204 ? {} : { body: JSON.stringify(body) }) })
  if (path === '/auth/csrf') return send({ token: 'csrf', headerName: 'X-XSRF-TOKEN' })
  if (path === '/auth/refresh') return send({ accessToken: 'test' })
  if (path === '/auth/me') return send(admin)
  if (path === '/auth/heartbeat') return send(null, 204)
  if (path === '/users' && req.method() === 'GET') return send(paged([admin, member]))
  if (path === '/users/2/role' && req.method() === 'PUT') {
    assert.equal(req.headers()['x-xsrf-token'], 'csrf')
    writes.push({ path, body: req.postDataJSON() })
    if (fail) return send({ message: 'Falha ao atualizar permissão.' }, 503)
    Object.assign(member, req.postDataJSON())
    return send(member)
  }
  if (path === '/internacoes/cuidados') return send(paged([{ id: 1, pacienteNome: 'Luna', baiaIdentificacao: 'B1', status: 'ATIVA', fotoVersao: null }]))
  if (path === '/alimentacao/agenda') return send([meal, { ...meal, id: 2, status: 'CONCLUIDA', horario: '2026-11-15T13:00:00Z', concluidaEm: '2026-11-15T13:05:00Z', responsavelNome: 'Marina' }].filter(i => i.horario >= url.searchParams.get('inicio') && i.horario < url.searchParams.get('fim')))
  if (path === '/internacoes/1/alimentacao/agenda/1' && req.method() === 'PUT') {
    assert.equal(req.headers()['x-xsrf-token'], 'csrf')
    writes.push({ path, body: req.postDataJSON() })
    if (fail) return send({ message: 'Falha ao salvar programação.' }, 503)
    Object.assign(meal, req.postDataJSON())
    return send(meal)
  }
  unexpected.push(req.method() + ' ' + path)
  return send({}, 500)
})
const dialog = () => page.getByRole('dialog')
const closeIntro = async () => { const skip = page.getByRole('button', { name: /Pular/ }); if (await skip.isVisible()) await skip.click() }
try {
  await mkdir('tests/artifacts', { recursive: true })
  await page.goto(base + '/equipe')
  await page.getByRole('heading', { name: 'Equipe', exact: true }).waitFor()
  await closeIntro()
  assert(await page.getByRole('button', { name: 'Editar permissão de Admin', exact: true }).isDisabled())
  await page.getByRole('button', { name: 'Editar permissão de Marina', exact: true }).click()
  await dialog().locator('[name="role"]').selectOption('AUXILIAR_TECNICO')
  fail = true
  await dialog().getByRole('button', { name: 'Salvar permissão', exact: true }).click()
  await dialog().getByRole('alert').waitFor()
  fail = false
  await dialog().getByRole('button', { name: 'Salvar permissão', exact: true }).click()
  await dialog().waitFor({ state: 'detached' })
  await page.getByText('Permissão de Marina atualizada.', { exact: true }).waitFor()
  assert.equal(member.role, 'AUXILIAR_TECNICO')
  await page.screenshot({ path: 'tests/artifacts/team-permission-edited.png', fullPage: true })
  await page.goto(base + '/alimentacao?dia=2026-11-15')
  await page.getByRole('button', { name: /Editar alimentação de Luna/ }).waitFor()
  assert.equal(await page.getByRole('button', { name: /Editar alimentação de Luna/ }).count(), 1)
  await page.getByRole('button', { name: /Editar alimentação de Luna/ }).click()
  await dialog().locator('[name="alimento"]').fill('Dieta pastosa')
  await dialog().locator('[name="quantidade"]').fill('50 ml')
  await dialog().locator('[name="horario"]').fill('2026-11-15T10:00')
  await dialog().getByRole('button', { name: 'Salvar programação' }).click()
  await dialog().getByText('Selecione um horário futuro para reagendar.', { exact: true }).waitFor()
  await dialog().locator('[name="horario"]').fill('2026-11-16T16:30')
  fail = true
  await dialog().getByRole('button', { name: 'Salvar programação' }).click()
  await dialog().getByText('Falha ao salvar programação.', { exact: true }).waitFor()
  fail = false
  await page.setViewportSize({ width: 390, height: 844 })
  assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth))
  await page.screenshot({ path: 'tests/artifacts/feeding-edit-mobile.png', fullPage: true })
  await dialog().getByRole('button', { name: 'Salvar programação' }).click()
  await dialog().waitFor({ state: 'detached' })
  await page.waitForURL('**/alimentacao?dia=2026-11-16')
  await page.getByText('Dieta pastosa · 50 ml', { exact: true }).waitFor()
  assert.equal(meal.horario, '2026-11-16T19:30:00.000Z')
  assert.equal(meal.status, 'PENDENTE')
  await page.reload()
  await page.getByText('Dieta pastosa · 50 ml', { exact: true }).waitFor()
  assert.equal(writes.length, 4)
  assert.deepEqual(errors, [])
  assert.deepEqual(unexpected, [])
  console.log('PASS: team role editing, own-role protection, save failures/retry, feeding edit, timezone, future-time validation, date navigation, read-back and mobile layout.')
} finally { await browser.close() }
