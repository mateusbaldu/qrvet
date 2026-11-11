import assert from 'node:assert/strict'
import { mkdir } from 'node:fs/promises'
import { chromium } from 'playwright'

const browser = await chromium.launch({ headless: true, ...(process.env.PLAYWRIGHT_CHANNEL ? { channel: process.env.PLAYWRIGHT_CHANNEL } : {}) })
const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } })
page.setDefaultTimeout(10000)
const base = process.env.TEST_BASE_URL || 'http://127.0.0.1:5173'
const errors = [], unexpected = [], writes = []
let role = 'VETERINARIO', name = 'Marina Silva', noteFails = false, profileFails = false, photoMissing = false
const photo = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=', 'base64')
const alerts = { alergias: null, restricoesAlimentares: null, cuidadosManejo: null }
const patient = { id: 1, tutorId: 1, nome: 'Luna', especie: 'Felina', raca: 'SRD', sexo: 'Fêmea', dataNascimento: '2021-04-12', peso: 4.5, fotoVersao: null, alertas: alerts }
const tutor = { id: 1, nome: 'Ana Silva', cpf: '52998224725', telefone: '11999999999', email: 'ana@example.com', endereco: 'Rua A' }
const admission = { id: 1, pacienteId: 1, pacienteNome: 'Luna', fotoVersao: 'photo-v1.jpg', baiaId: 1, baiaIdentificacao: 'A-01', veterinarioId: 2, veterinarioNome: 'Marina Silva', status: 'ATIVA', entradaInternacao: '2026-11-10T12:00:00Z', motivo: 'Observação', diagnosticoInicial: 'Avaliação', observacoes: '', alertas: alerts }
const notes = Array.from({ length: 13 }, (_, i) => ({ id: 13 - i, internacaoId: 1, profissionalId: 2, profissionalNome: name, texto: 'Observação anterior ' + (13 - i), dataHoraRegistro: new Date(Date.now() - (i + 1) * 60000).toISOString() }))
const paged = (items, url) => {
  const page = Number(url.searchParams.get('page') || 0), size = Number(url.searchParams.get('size') || 12)
  return { items: items.slice(page * size, (page + 1) * size), page, size, total: items.length, pages: Math.ceil(items.length / size) }
}
page.on('pageerror', error => errors.push(error.message))
await page.addInitScript(() => {
  for (const role of ['VETERINARIO', 'AUXILIAR_TECNICO', 'RECEPCIONISTA']) localStorage.setItem('qrvet:onboarding:v1:2:' + role, JSON.stringify({ seen: true, step: 0 }))
})
await page.route('**/qrvet/api/v1/**', route => {
  const req = route.request(), url = new URL(req.url()), path = url.pathname.split('/v1')[1], method = req.method()
  const send = (data, status = 200) => route.fulfill({ status, contentType: 'application/json', ...(status === 204 ? {} : { body: JSON.stringify(data) }) })
  const user = () => ({ id: 2, name, role, email: 'marina@example.com', active: true, confirmed: true })
  if (method !== 'GET') {
    assert.equal(req.headers()['x-xsrf-token'], 'csrf')
    const body = req.postData() ? req.postDataJSON() : {}
    writes.push({ path, method, body })
    if (path === '/auth/refresh') return send({ accessToken: 'test' })
    if (path === '/auth/heartbeat') return send(null, 204)
    if (path === '/auth/me' && method === 'PUT') {
      if (profileFails) return send({ message: 'Não foi possível salvar o perfil.' }, 503)
      name = body.name
      return send(user())
    }
    if (path === '/pacientes/1/alertas') { Object.assign(alerts, body); return send(patient) }
    if (path === '/internacoes/1/evolucoes') {
      if (noteFails) return send({ message: 'Não foi possível salvar a evolução.' }, 503)
      const note = { id: notes.length + 1, internacaoId: 1, profissionalId: 2, profissionalNome: name, texto: body.texto, dataHoraRegistro: new Date().toISOString() }
      notes.unshift(note)
      return send(note, 201)
    }
  } else {
    if (path === '/auth/csrf') return send({ token: 'csrf', headerName: 'X-XSRF-TOKEN' })
    if (path === '/auth/me') return send(user())
    if (path === '/pacientes/1') return send(patient)
    if (path === '/tutores') return send(paged([tutor], url))
    if (path === '/internacoes') return send(paged([admission], url))
    if (path === '/internacoes/cuidados') return send(paged([admission], url))
    if (path === '/internacoes/1') return send(admission)
    if (path === '/internacoes/1/foto') {
      assert(req.headers().authorization, 'Hospitalization photo must use authenticated requests')
      return photoMissing ? send({ message: 'Foto não encontrada.' }, 404) : route.fulfill({ contentType: 'image/png', body: photo })
    }
    if (path === '/internacoes/1/cuidados') return send(admission)
    if (path === '/internacoes/1/evolucoes') return send(paged(notes, url))
    if (path === '/internacoes/1/jejum' || path === '/internacoes/1/alimentacao' || path === '/internacoes/1/medicacao' || path === '/alimentacao/agenda') return send([])
    if (path === '/internacoes/1/qrcode') return send({ base64: '', url: base + '/internacoes/1', tutorUrl: base + '/public/internacoes/qr/test', tutorEmail: tutor.email })
  }
  unexpected.push(method + ' ' + path)
  return send({ message: 'Unexpected request' }, 500)
})
try {
  await mkdir('tests/artifacts', { recursive: true })
  await page.goto(base + '/meu-perfil')
  const nameInput = page.getByLabel('Nome completo', { exact: true })
  await nameInput.fill('Marina Costa')
  assert.equal(await page.locator('.app-header').getByRole('link', { name: /Consultar.*QR/i }).count(), 0)
  profileFails = true
  await page.getByRole('button', { name: 'Salvar nome', exact: true }).click()
  await page.getByRole('alert').getByText('Não foi possível salvar o perfil.').waitFor()
  assert.equal(await nameInput.inputValue(), 'Marina Costa')
  profileFails = false
  await page.getByRole('button', { name: 'Salvar nome', exact: true }).click()
  await page.getByText('Nome atualizado.', { exact: true }).waitFor()
  assert.match(await page.locator('.header-profile').innerText(), /Marina Costa/)
  assert.match(await page.locator('.sidebar-user').innerText(), /Marina Costa/)
  await page.reload()
  await nameInput.waitFor()
  assert.equal(await nameInput.inputValue(), 'Marina Costa')
  assert.equal(await page.getByLabel('E-mail', { exact: true }).getAttribute('readonly'), '')
  await page.screenshot({ path: 'tests/artifacts/profile-edit.png', fullPage: true, animations: 'disabled' })

  await page.goto(base + '/pacientes/1')
  await page.getByRole('button', { name: 'Editar alertas', exact: true }).click()
  const dialog = page.getByRole('dialog')
  await dialog.getByLabel('Alergias', { exact: false }).fill('Alergia conhecida à substância X')
  await dialog.getByLabel('Restrições alimentares', { exact: false }).fill('Evitar frango')
  await dialog.getByLabel('Cuidados de manejo', { exact: false }).fill('Sensível a ruídos.\nManipular com calma.')
  await dialog.getByRole('button', { name: 'Salvar', exact: true }).click()
  await page.locator('.patient-alerts').getByText('Evitar frango', { exact: true }).waitFor()
  await page.reload()
  await page.locator('.patient-alerts').getByText('Alergia conhecida à substância X', { exact: true }).waitFor()

  await page.goto(base + '/internacoes')
  await page.locator('.admission-card').getByRole('img', { name: 'Foto de Luna', exact: true }).waitFor()
  assert(await page.locator('.admission-card img').evaluate(img => img.complete && img.naturalWidth > 0))
  await page.goto(base + '/internacoes/1')
  await page.getByRole('tab', { name: 'Evoluções', exact: true }).click()
  await page.locator('.hospitalization-banner').getByRole('img', { name: 'Foto de Luna', exact: true }).waitFor()
  await page.locator('.patient-alerts').getByText('Evitar frango', { exact: true }).waitFor()
  await page.locator('.evolution-notes-list li').first().waitFor()
  assert.equal(await page.locator('.evolution-notes-list li').count(), 5)
  await page.locator('.evolution-notes').getByRole('button', { name: 'Próxima', exact: true }).click()
  await page.getByText('Observação anterior 8', { exact: true }).waitFor()
  await page.locator('.evolution-notes').getByRole('button', { name: 'Próxima', exact: true }).click()
  await page.getByText('Observação anterior 1', { exact: true }).waitFor()
  await page.getByRole('button', { name: 'Adicionar evolução', exact: true }).click()
  await dialog.getByLabel('Anotação de evolução', { exact: false }).fill('Paciente recusou alimento.\nTutor trouxe ração.')
  noteFails = true
  await dialog.getByRole('button', { name: 'Salvar evolução', exact: true }).click()
  await dialog.getByRole('alert').waitFor()
  assert.equal(await dialog.getByLabel('Anotação de evolução', { exact: false }).inputValue(), 'Paciente recusou alimento.\nTutor trouxe ração.')
  noteFails = false
  await dialog.getByRole('button', { name: 'Salvar evolução', exact: true }).click()
  await dialog.waitFor({ state: 'detached' })
  await page.locator('.evolution-notes-list li').first().getByText('Marina Costa', { exact: true }).waitFor()
  assert.match(await page.locator('.evolution-notes-list li').first().innerText(), /Paciente recusou alimento/)
  assert.equal(notes.length, 14)
  assert.deepEqual(writes.filter(w => w.path.endsWith('/evolucoes')).at(-1).body, { texto: 'Paciente recusou alimento.\nTutor trouxe ração.' })
  await page.reload()
  await page.locator('.evolution-notes-list li').first().getByText('Marina Costa', { exact: true }).waitFor()
  await page.screenshot({ path: 'tests/artifacts/clinical-records-desktop.png', fullPage: true, animations: 'disabled' })
  await page.setViewportSize({ width: 360, height: 800 })
  assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth))
  await page.getByRole('button', { name: 'Adicionar evolução', exact: true }).click()
  const box = await dialog.boundingBox()
  assert(box.x >= 0 && box.x + box.width <= 360)
  await page.screenshot({ path: 'tests/artifacts/clinical-records-mobile.png', fullPage: true, animations: 'disabled' })
  await page.keyboard.press('Escape')

  photoMissing = true
  await page.reload()
  await page.locator('.hospitalization-banner').getByText('Sem foto', { exact: true }).waitFor()
  await page.locator('.evolution-notes-list li').first().waitFor()
  assert.equal(await page.locator('.hospitalization-banner img').count(), 0)
  photoMissing = false
  admission.fotoVersao = null
  await page.reload()
  await page.locator('.evolution-notes-list li').first().waitFor()
  await page.locator('.hospitalization-banner').getByText('Sem foto', { exact: true }).waitFor()
  admission.fotoVersao = 'photo-v2.jpg'

  role = 'AUXILIAR_TECNICO'
  await page.goto(base + '/cuidados?internacao=1')
  await page.getByRole('tab', { name: 'Evoluções', exact: true }).click()
  await page.locator('.hospitalization-banner').getByRole('img', { name: 'Foto de Luna', exact: true }).waitFor()
  await page.locator('.evolution-notes-list li').first().waitFor()
  await page.locator('.patient-alerts').getByText('Evitar frango', { exact: true }).waitFor()
  assert.equal(await page.getByRole('button', { name: 'Editar alertas', exact: true }).count(), 0)
  assert.equal(await page.getByRole('button', { name: 'Adicionar evolução', exact: true }).count(), 1)
  admission.status = 'ALTA'
  await page.reload()
  await page.getByText('Internação encerrada. Evoluções disponíveis para consulta.', { exact: true }).waitFor()
  assert.equal(await page.getByRole('button', { name: 'Adicionar evolução', exact: true }).count(), 0)

  role = 'VETERINARIO'
  await page.goto(base + '/pacientes/1')
  await page.getByRole('button', { name: 'Editar alertas', exact: true }).click()
  for (const field of ['alergias', 'restricoesAlimentares', 'cuidadosManejo']) await dialog.locator('[name="' + field + '"]').fill('')
  await dialog.getByRole('button', { name: 'Salvar', exact: true }).click()
  await page.getByText('Nenhum alerta registrado. Confirme as informações com a equipe.', { exact: true }).waitFor()
  assert.deepEqual(errors, [])
  assert.deepEqual(unexpected, [])
  console.log('Clinical records: profile save/retry/persistence, patient alerts, evolution creation/retry/pagination/authorship, closed admissions, roles and mobile passed.')
} finally { await browser.close() }
