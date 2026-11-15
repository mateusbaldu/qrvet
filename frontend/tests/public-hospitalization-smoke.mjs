import assert from 'node:assert/strict'
import { mkdir } from 'node:fs/promises'
import { chromium } from 'playwright'

const browser = await chromium.launch({ headless: true, ...(process.env.PLAYWRIGHT_CHANNEL ? { channel: process.env.PLAYWRIGHT_CHANNEL } : {}) })
const page = await browser.newPage({ viewport: { width: 1280, height: 900 } })
const base = process.env.TEST_BASE_URL || 'http://127.0.0.1:5173'
const errors = [], unexpected = []
let empty = false, fails = false, closed = false
const notes = Array.from({ length: 7 }, (_, index) => ({ profissionalNome: 'Marina Silva', texto: `Evolução ${7 - index}\nCuidados registrados pela equipe.`, dataHoraRegistro: new Date(Date.UTC(2026, 10, 15, 12, 7 - index)).toISOString() }))
page.on('pageerror', error => errors.push(error.message))
await page.route('**/qrvet/api/v1/**', route => {
  const request = route.request(), url = new URL(request.url()), path = url.pathname.split('/v1')[1]
  const send = (body, status = 200) => route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) })
  if (path.startsWith('/public/internacoes/qr/')) {
    assert.equal(request.method(), 'GET')
    assert(!request.headers().authorization)
    if (fails) return send({ message: 'Não foi possível consultar a internação.' }, 503)
    const current = Number(url.searchParams.get('page') || 0), size = Number(url.searchParams.get('size'))
    assert.equal(size, 5)
    return send({ pacienteNome: 'Luna', especie: 'Felina', raca: 'SRD', sexo: 'Fêmea', baiaIdentificacao: 'A-01', motivo: 'Observação', status: closed ? 'ALTA' : 'ATIVA', jejumAtivo: true, entradaInternacao: '2026-11-14T10:00:00Z', saidaInternacao: closed ? '2026-11-15T16:00:00Z' : null, veterinarioNome: 'Marina Silva', alertas: empty ? {} : { alergias: 'Alergia registrada', restricoesAlimentares: 'Evitar frango', cuidadosManejo: 'Manipular com calma.\nEvitar ruídos.' }, evolucoes: { items: empty ? [] : notes.slice(current * size, (current + 1) * size), page: current, size, total: empty ? 0 : notes.length, pages: empty ? 0 : 2 } })
  }
  if (path === '/auth/csrf') return send({ token: 'test', headerName: 'X-XSRF-TOKEN' })
  if (path === '/auth/refresh' || path === '/auth/me') return send({ message: 'Unauthorized' }, 401)
  unexpected.push(path)
  return send({}, 500)
})
try {
  await mkdir('tests/artifacts', { recursive: true })
  await page.goto(base + '/public/internacoes/qr/9a4a39e6-4323-48b1-9321-d8e12a8e4031')
  await page.getByRole('heading', { name: 'Luna', exact: true }).waitFor()
  await page.getByText('Evitar frango', { exact: true }).waitFor()
  assert.equal(await page.locator('.evolution-notes-list li').count(), 5)
  assert.match(await page.locator('.evolution-notes-list li').first().innerText(), /Evolução 7/)
  assert.equal(await page.getByRole('button', { name: /Editar|Adicionar|Salvar/ }).count(), 0)
  await page.screenshot({ path: 'tests/artifacts/public-hospitalization-desktop.png', fullPage: true })
  await page.getByRole('button', { name: 'Próxima', exact: true }).click()
  await page.getByText('Evolução 2', { exact: false }).waitFor()
  assert.equal(await page.locator('.evolution-notes-list li').count(), 2)
  await page.getByRole('button', { name: 'Anterior', exact: true }).click()
  await page.getByText('Evolução 7', { exact: false }).waitFor()
  for (const width of [360, 390]) {
    await page.setViewportSize({ width, height: 800 })
    assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth))
  }
  await page.screenshot({ path: 'tests/artifacts/public-hospitalization-mobile.png', fullPage: true })
  closed = true
  await page.getByRole('button', { name: 'Atualizar informações', exact: true }).click()
  await page.getByText('Encerramento da internação', { exact: true }).waitFor()
  assert.equal(await page.getByText('Paciente em jejum.', { exact: true }).count(), 0)
  empty = true
  await page.getByRole('button', { name: 'Atualizar informações', exact: true }).click()
  await page.getByRole('heading', { name: 'Nenhuma evolução registrada' }).waitFor()
  await page.getByText(/Nenhum alerta ou restrição registrado/).waitFor()
  fails = true
  await page.getByRole('button', { name: 'Atualizar informações', exact: true }).click()
  await page.getByRole('alert').waitFor()
  fails = false
  await page.getByRole('button', { name: 'Tentar novamente', exact: true }).click()
  await page.getByRole('heading', { name: 'Luna', exact: true }).waitFor()
  assert.deepEqual(errors, [])
  assert.deepEqual(unexpected, [])
  console.log('PASS: tutor alerts, restrictions, fasting, evolutions, pagination, refresh, discharge, empty/error/retry states, read-only access and mobile layout.')
} finally {
  await browser.close()
}
