import assert from 'node:assert/strict'
import { mkdir } from 'node:fs/promises'
import { chromium } from 'playwright'

const browser = await chromium.launch({ headless: true, ...(process.env.PLAYWRIGHT_CHANNEL ? { channel: process.env.PLAYWRIGHT_CHANNEL } : {}) })
const page = await browser.newPage({ viewport: { width: 390, height: 844 }, timezoneId: 'America/Sao_Paulo' })
page.setDefaultTimeout(10000)
await page.clock.setFixedTime(new Date('2026-11-10T15:00:00Z'))
const base = process.env.TEST_BASE_URL || 'http://127.0.0.1:5173'
const png = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=', 'base64')
const patients = Array.from({ length: 30 }, (_, n) => ({ id: n + 1, nome: 'Paciente ' + String(n + 1).padStart(2, '0'), especie: 'Canina', raca: 'SRD', sexo: 'Macho', peso: 5, dataNascimento: '2021-01-01', tutorId: 1, tutorNome: 'Ana Silva', fotoVersao: n === 0 ? 'photo.jpg' : null }))
const admissions = patients.map(p => ({ id: p.id, pacienteId: p.id, pacienteNome: p.nome, fotoVersao: p.fotoVersao, baiaIdentificacao: 'A-' + String(p.id).padStart(2, '0'), veterinarioId: 2, veterinarioNome: 'Marina Silva', status: 'ATIVA', motivo: 'Observação', diagnosticoInicial: 'Avaliação', observacoes: '', entradaInternacao: '2026-11-10T12:00:00Z' }))
const tutor = { id: 1, nome: 'Ana Silva', cpf: '52998224725', telefone: '11999999999', email: 'ana@example.com', endereco: 'Rua A' }
const meal = { id: 1, internacaoId: 1, pacienteNome: 'Paciente 01', baiaIdentificacao: 'A-01', internacaoStatus: 'ATIVA', status: 'PENDENTE', jejumAtivo: false, alimento: 'Ração', quantidade: '40 g', horario: '2026-11-10T14:00:00Z' }
const errors = [], unexpected = []
let failNextPatients = false
page.on('pageerror', error => errors.push(error.message))
await page.addInitScript(() => localStorage.setItem('qrvet:onboarding:v1:2:VETERINARIO', JSON.stringify({ seen: true })))
const paged = (items, url) => {
  const page = Number(url.searchParams.get('page') || 0), size = Number(url.searchParams.get('size') || 12)
  return { items: items.slice(page * size, (page + 1) * size), page, size, total: items.length, pages: Math.ceil(items.length / size) }
}
await page.route('**/qrvet/api/v1/**', async route => {
  const req = route.request(), url = new URL(req.url()), path = url.pathname.split('/v1')[1]
  const send = (data, status = 200) => route.fulfill({ status, contentType: 'application/json', ...(status === 204 ? {} : { body: JSON.stringify(data) }) })
  if (path === '/auth/csrf') return send({ token: 'csrf', headerName: 'X-XSRF-TOKEN' })
  if (path === '/auth/refresh') return send({ accessToken: 'test' })
  if (path === '/auth/heartbeat') return send(null, 204)
  if (path === '/auth/me') return send({ id: 2, name: 'Marina Silva', role: 'VETERINARIO', email: 'marina@example.com', active: true, confirmed: true })
  if (path.endsWith('/foto')) { assert(req.headers().authorization); return route.fulfill({ contentType: 'image/png', body: png }) }
  if (path === '/pacientes') {
    if (failNextPatients) return send({ message: 'Falha temporária na lista.' }, 503)
    await new Promise(resolve => setTimeout(resolve, 150))
    return send(paged(patients.filter(p => p.nome.toLowerCase().includes((url.searchParams.get('q') || '').toLowerCase())), url))
  }
  if (/^\/pacientes\/\d+$/.test(path)) return send(patients.find(p => p.id === Number(path.split('/').pop())))
  if (path === '/tutores') return send(paged([tutor], url))
  if (path === '/internacoes/cuidados' || path === '/internacoes/ativas') return send(paged(admissions, url))
  if (path === '/internacoes') return send(paged(admissions.filter(a => (!url.searchParams.has('pacienteId') || String(a.pacienteId) === url.searchParams.get('pacienteId')) && a.pacienteNome.toLowerCase().includes((url.searchParams.get('q') || '').toLowerCase())), url))
  const match = path.match(/^\/internacoes\/(\d+)(?:\/(.*))?$/)
  if (match) {
    const item = admissions.find(a => a.id === Number(match[1]))
    if (!match[2] || match[2] === 'cuidados') return send(item)
    if (match[2] === 'evolucoes') return send(paged([], url))
    if (['jejum', 'alimentacao', 'medicacao'].includes(match[2])) return send([])
    if (match[2] === 'qrcode') return send({ base64: png.toString('base64'), url: base + '/internacoes/' + item.id, tutorUrl: base + '/public/internacoes/qr/test', tutorEmail: tutor.email })
  }
  if (path === '/alimentacao/agenda') return send([meal])
  unexpected.push(req.method() + ' ' + path)
  return send({}, 404)
})
async function assertScroll(expected) {
  await page.waitForFunction(y => Math.abs(window.scrollY - y) < 5, expected)
}
try {
  await mkdir('tests/artifacts', { recursive: true })
  await page.goto(base + '/pacientes?busca=Paciente&situacao=ATIVA&pagina=1')
  const patientLink = page.getByRole('link', { name: 'Ver detalhes de Paciente 20', exact: true })
  await patientLink.scrollIntoViewIfNeeded()
  const patientY = await page.evaluate(() => window.scrollY)
  assert(patientY > 200)
  await patientLink.click()
  await page.getByRole('link', { name: 'Voltar aos pacientes', exact: true }).click()
  await page.getByRole('link', { name: 'Ver detalhes de Paciente 20', exact: true }).waitFor()
  assert.equal(await page.getByPlaceholder('Buscar por paciente ou tutor').inputValue(), 'Paciente')
  assert.equal(await page.getByRole('button', { name: 'Internados', exact: true }).getAttribute('aria-pressed'), 'true')
  assert.equal(new URL(page.url()).searchParams.get('pagina'), '1')
  await assertScroll(patientY)
  await page.getByRole('link', { name: 'Ver detalhes de Paciente 20', exact: true }).click()
  await page.goBack()
  await page.getByRole('link', { name: 'Ver detalhes de Paciente 20', exact: true }).waitFor()
  await assertScroll(patientY)
  failNextPatients = true
  await page.reload()
  await page.getByRole('alert').waitFor()
  failNextPatients = false
  await page.getByRole('button', { name: 'Tentar novamente', exact: true }).click()
  await page.getByRole('link', { name: 'Ver detalhes de Paciente 20', exact: true }).waitFor()
  await assertScroll(patientY)

  await page.goto(base + '/internacoes?busca=Paciente&situacao=ATIVA&pagina=1')
  const admissionLink = page.locator('.admission-card').filter({ hasText: 'Paciente 20' }).getByRole('link', { name: 'Acompanhar internação', exact: true })
  await admissionLink.scrollIntoViewIfNeeded()
  const admissionY = await page.evaluate(() => window.scrollY)
  await admissionLink.click()
  await page.getByRole('tab', { name: 'Resumo', exact: true }).waitFor()
  assert.equal(await page.getByRole('tab', { name: 'Resumo', exact: true }).getAttribute('aria-selected'), 'true')
  await page.getByRole('tab', { name: 'Resumo', exact: true }).focus()
  await page.keyboard.press('ArrowRight')
  await page.getByRole('button', { name: 'Adicionar evolução', exact: true }).waitFor()
  await page.keyboard.press('ArrowRight')
  await page.getByRole('button', { name: 'Registrar medicação', exact: true }).waitFor()
  await page.reload()
  await page.getByRole('button', { name: 'Registrar medicação', exact: true }).waitFor()
  await page.getByRole('link', { name: 'Voltar às internações', exact: true }).click()
  await admissionLink.waitFor()
  assert.equal(new URL(page.url()).searchParams.get('pagina'), '1')
  await assertScroll(admissionY)

  await page.goto(base + '/cuidados?busca=Paciente&pagina=1')
  const careLink = page.getByRole('link', { name: 'Abrir cuidados de Paciente 20', exact: true })
  await careLink.scrollIntoViewIfNeeded()
  const careY = await page.evaluate(() => window.scrollY)
  await careLink.click()
  await page.getByRole('button', { name: 'Registrar medicação', exact: true }).waitFor()
  await page.getByRole('link', { name: 'Voltar aos pacientes em cuidados', exact: true }).click()
  await careLink.waitFor()
  await assertScroll(careY)
  assert.equal(await page.getByPlaceholder('Nome, baia ou código da internação').inputValue(), 'Paciente')
  await page.getByPlaceholder('Nome, baia ou código da internação').fill('A-01')
  await page.getByRole('link', { name: 'Abrir cuidados de Paciente 01', exact: true }).waitFor()
  assert.equal(await page.locator('.care-patient-card').count(), 1)
  await page.getByPlaceholder('Nome, baia ou código da internação').fill('inexistente')
  await page.getByRole('heading', { name: 'Nenhum paciente nesta busca', exact: true }).waitFor()

  await page.goto(base + '/alimentacao?dia=2026-11-10&filtro=PENDENTE&busca=Paciente')
  const row = page.locator('.clinic-feeding-list > li').first()
  await row.getByRole('img', { name: 'Foto de Paciente 01', exact: true }).waitFor()
  await row.getByRole('button', { name: 'Concluir', exact: true }).click()
  const dialog = page.getByRole('dialog')
  await dialog.getByRole('img', { name: 'Foto de Paciente 01', exact: true }).waitFor()
  assert(await dialog.evaluate(el => el.contains(document.activeElement)), 'Confirmation must keep keyboard focus inside the dialog')
  assert.match(await dialog.locator('.feeding-confirm-patient').innerText(), /Baia A-01/)
  await page.screenshot({ path: 'tests/artifacts/feeding-confirm-mobile.png', fullPage: true, animations: 'disabled' })
  await page.keyboard.press('Escape')
  await row.getByRole('link', { name: 'Ver cuidados →', exact: true }).click()
  await page.getByRole('button', { name: 'Registrar medicação', exact: true }).waitFor()
  await page.goBack()
  await row.waitFor()
  assert.equal(await page.getByLabel('Dia do checklist', { exact: true }).inputValue(), '2026-11-10')
  assert.equal(await page.getByPlaceholder('Nome do paciente ou baia').inputValue(), 'Paciente')
  assert.equal(await page.getByRole('button', { name: 'Pendentes', exact: true }).getAttribute('aria-pressed'), 'true')
  for (const width of [360, 768, 1440]) {
    await page.setViewportSize({ width, height: 900 })
    await page.goto(base + '/cuidados')
    await page.locator('.care-patient-card').first().waitFor()
    assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), 'Care cards overflow at ' + width)
    await page.getByRole('link', { name: 'Abrir cuidados de Paciente 01', exact: true }).click()
    await page.getByRole('tab', { name: 'Evoluções', exact: true }).click()
    assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), 'Tabs overflow at ' + width)
    await page.screenshot({ path: `tests/artifacts/care-tabs-${width}.png`, fullPage: true, animations: 'disabled' })
  }
  assert.deepEqual(errors, [])
  assert.deepEqual(unexpected, [])
  console.log('Frontend UX: patient/admission/care return and scroll, URL filters/pagination/retry, keyboard tabs, photo checklist/confirmation, search and responsive layouts passed.')
} finally { await browser.close() }
