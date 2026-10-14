import assert from 'node:assert/strict'
import { mkdir } from 'node:fs/promises'
import { chromium } from 'playwright'

const browser = await chromium.launch({
  headless: true,
  ...(process.env.PLAYWRIGHT_CHANNEL ? { channel: process.env.PLAYWRIGHT_CHANNEL } : {}),
})
const page = await browser.newPage({
  viewport: { width: 1440, height: 1000 },
  timezoneId: 'America/Sao_Paulo',
})
await page.clock.setFixedTime(new Date('2026-09-28T15:00:00Z'))
page.setDefaultTimeout(10000)
const errors = [],
  unexpected = [],
  writes = []
page.on('pageerror', e => errors.push(e.message))
let photoFails = true,
  fasting = false
const tutors = [],
  patients = [],
  agenda = []
const png = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=',
  'base64'
)
const care = { id: 1, pacienteNome: 'Mel', baiaIdentificacao: 'A-01', status: 'ATIVA' }
const baseMeal = {
  internacaoId: 1,
  pacienteNome: 'Mel',
  baiaIdentificacao: 'A-01',
  internacaoStatus: 'ATIVA',
  alimento: 'Ração úmida',
  quantidade: '40 g',
  status: 'PENDENTE',
  jejumAtivo: false,
  concluidaEm: null,
  responsavelNome: null,
  observacao: null,
  motivoCancelamento: null,
}
const paged = items => ({
  items,
  total: items.length,
  pages: items.length ? 1 : 0,
  page: 0,
  size: 100,
})
await page.route('**/qrvet/api/v1/**', async route => {
  const req = route.request(),
    url = new URL(req.url()),
    path = url.pathname.split('/v1')[1],
    method = req.method()
  const send = (data, status = 200) =>
    route.fulfill({
      status,
      contentType: 'application/json',
      ...(status === 204 ? {} : { body: JSON.stringify(data) }),
    })
  if (method !== 'GET') {
    assert.equal(req.headers()['x-xsrf-token'], 'csrf')
    if (path.endsWith('/foto')) {
      assert(req.headers().authorization)
      const patient = patients.find(p => p.id === Number(path.split('/')[2]))
      if (method === 'DELETE') {
        patient.fotoVersao = null
        return send(null, 204)
      }
      assert.match(req.headers()['content-type'], /^multipart\/form-data; boundary=/)
      if (photoFails) return send({ message: 'Não foi possível armazenar a foto.' }, 500)
      patient.fotoVersao = 'local-photo.jpg'
      return send(patient)
    }
    const body = req.postData() ? req.postDataJSON() : {}
    writes.push({ path, method, body })
    if (path === '/auth/refresh') return send({ accessToken: 'test' })
    if (path === '/auth/heartbeat') return send(null, 204)
    if (path === '/pacientes' && method === 'POST') {
      if (body.novoTutor) {
        assert.equal(body.tutorId, undefined)
        const tutor = { ...body.novoTutor, id: tutors.length + 1 }
        tutors.push(tutor)
        body.tutorId = tutor.id
      }
      const patient = { ...body, id: patients.length + 1, fotoVersao: null }
      patients.push(patient)
      return send(patient, 201)
    }
    if (/^\/pacientes\/\d+$/.test(path) && method === 'PUT') {
      const patient = patients.find(p => p.id === Number(path.split('/')[2]))
      Object.assign(patient, body)
      return send(patient)
    }
    if (/^\/tutores\/\d+$/.test(path) && method === 'PUT') {
      Object.assign(tutors[0], body)
      return send(tutors[0])
    }
    if (path === '/internacoes/1/alimentacao/agenda') {
      const created = body.horarios
        .map(horario => ({
          ...baseMeal,
          id: agenda.length + 1,
          alimento: body.alimento,
          quantidade: body.quantidade,
          horario,
        }))
        .map((item, n) => ({ ...item, id: agenda.length + n + 1 }))
      agenda.push(...created)
      return send(created, 201)
    }
    const match = path.match(/^\/internacoes\/1\/alimentacao\/agenda\/(\d+)\/(concluir|cancelar)$/)
    if (match) {
      const meal = agenda.find(i => i.id === Number(match[1]))
      if (match[2] === 'concluir')
        Object.assign(meal, {
          status: 'CONCLUIDA',
          concluidaEm: '2026-09-28T15:00:00Z',
          responsavelNome: 'Marina Silva',
          observacao: body.observacao,
        })
      else Object.assign(meal, { status: 'CANCELADA', motivoCancelamento: body.motivo })
      return send(meal)
    }
  } else {
    if (path === '/auth/csrf') return send({ token: 'csrf', headerName: 'X-XSRF-TOKEN' })
    if (path === '/auth/me')
      return send({ id: 1, name: 'Marina Silva', role: 'ADMIN', email: 'marina@example.com' })
    if (path === '/tutores') return send(paged(tutors))
    if (path === '/pacientes') return send(paged(patients))
    if (/^\/pacientes\/\d+$/.test(path))
      return send(patients.find(p => p.id === Number(path.split('/')[2])))
    if (path.endsWith('/foto')) return route.fulfill({ contentType: 'image/png', body: png })
    if (path === '/internacoes' || path === '/internacoes/ativas') return send(paged([]))
    if (path === '/internacoes/cuidados') return send(paged([care]))
    if (path === '/internacoes/1/cuidados') return send(care)
    if (path === '/internacoes/1/jejum' || path === '/internacoes/1/alimentacao') return send([])
    if (path === '/baias')
      return send(paged([{ id: 1, identificacao: 'A-01', status: 'DISPONIVEL' }]))
    if (path === '/internacoes/veterinarios') return send([{ id: 2, name: 'Marina Silva' }])
    if (path === '/alimentacao/agenda')
      return send(
        agenda
          .filter(
            i =>
              i.horario >= url.searchParams.get('inicio') && i.horario < url.searchParams.get('fim')
          )
          .map(i => ({ ...i, jejumAtivo: fasting }))
          .sort((a, b) => a.horario.localeCompare(b.horario))
      )
  }
  unexpected.push(method + ' ' + path)
  return send({ message: 'Unexpected request' }, 500)
})
const base = process.env.TEST_BASE_URL || 'http://127.0.0.1:5173'
const visit = path => page.goto(base + path, { waitUntil: 'domcontentloaded' })
const fill = async data => {
  for (const [key, value] of Object.entries(data))
    await page.locator('[name="' + key + '"]').fill(value)
}
const dialog = () => page.getByRole('dialog')
const save = async (name = 'Salvar') => {
  await dialog().getByRole('button', { name, exact: true }).click()
  await dialog().waitFor({ state: 'detached' })
}
const shot = name =>
  page.screenshot({
    path: 'tests/artifacts/mvp-' + name + '.png',
    fullPage: true,
    animations: 'disabled',
  })
await mkdir('tests/artifacts', { recursive: true })
try {
  await visit('/pacientes?novo=1')
  await dialog().waitFor()
  await fill({
    nome: 'Mel',
    especie: 'Canina',
    raca: 'SRD',
    dataNascimento: '2022-01-01',
    peso: '8.5',
    tutor_nome: 'Beatriz Silva',
    tutor_cpf: '52998224725',
    tutor_telefone: '11999999999',
    tutor_email: 'bia@example.com',
    tutor_endereco: 'Rua B',
  })
  await page.locator('[name="sexo"]').selectOption('Fêmea')
  await page
    .locator('[name="foto"]')
    .setInputFiles({ name: 'mel.png', mimeType: 'image/png', buffer: png })
  await shot('cadastro-conjunto')
  await save()
  await page.waitForURL('**/pacientes/1')
  await page.getByText(/Cadastro salvo, mas a foto não foi enviada/).waitFor()
  assert.equal(patients.length, 1)
  assert.equal(tutors.length, 1)
  photoFails = false
  await page.getByRole('button', { name: 'Adicionar foto', exact: true }).click()
  await page
    .locator('[name="foto"]')
    .setInputFiles({ name: 'mel.png', mimeType: 'image/png', buffer: png })
  await save()
  await page.getByAltText('Foto de Mel').waitFor()
  await page.reload()
  await page.getByAltText('Foto de Mel').waitFor()
  await page.getByRole('button', { name: 'Editar paciente', exact: true }).click()
  await fill({ peso: '9.2', observacoes: 'Prefere alimento úmido.' })
  await save()
  await page.getByText('9,2 kg', { exact: true }).waitFor()
  assert.equal(patients.length, 1)
  await page.getByRole('button', { name: 'Editar tutor', exact: true }).click()
  await fill({ telefone: '11888888888' })
  await save()
  await page.getByRole('link', { name: '11888888888' }).waitFor()
  await shot('ficha')
  await page.getByRole('button', { name: 'Remover foto', exact: true }).click()
  await save()
  await page.getByRole('button', { name: 'Adicionar foto', exact: true }).waitFor()
  await visit('/pacientes?novo=1&tutor=1')
  await fill({
    nome: 'Thor',
    especie: 'Canina',
    raca: 'SRD',
    dataNascimento: '2020-01-01',
    peso: '12',
  })
  await page.locator('[name="sexo"]').selectOption('Macho')
  await page.getByLabel('Abrir internação após salvar').check()
  await dialog().getByRole('button', { name: 'Salvar', exact: true }).click()
  await page.waitForURL('**/internacoes?paciente=2')
  await dialog().getByRole('heading', { name: 'Abrir internação', exact: true }).waitFor()
  assert.equal(await page.locator('[name="pacienteId"]').inputValue(), '2')
  await page.keyboard.press('Escape')

  agenda.push(
    { ...baseMeal, id: 1, horario: '2026-09-28T12:00:00.000Z' },
    { ...baseMeal, id: 2, horario: '2026-09-28T21:00:00.000Z' }
  )
  await visit('/alimentacao')
  const rows = page.locator('.clinic-feeding-list > li')
  await rows.first().waitFor()
  assert.equal(await rows.count(), 2)
  assert(await rows.nth(1).getByRole('button', { name: 'Concluir', exact: true }).isDisabled())
  fasting = true
  await page.getByRole('button', { name: 'Atualizar checklist' }).click()
  await rows.first().getByText('Em jejum', { exact: true }).waitFor()
  assert(await rows.first().getByRole('button', { name: 'Concluir', exact: true }).isDisabled())
  fasting = false
  await page.getByRole('button', { name: 'Atualizar checklist' }).click()
  await rows.first().getByRole('button', { name: 'Concluir', exact: true }).click()
  await fill({ observacao: 'Boa aceitação' })
  await save('Confirmar alimentação')
  await rows
    .first()
    .getByText(/Marina Silva/)
    .waitFor()
  assert.equal(writes.filter(w => w.path.endsWith('/concluir')).length, 1)
  await page.getByRole('button', { name: 'Agendar alimentação', exact: true }).click()
  await page.locator('[name="internacaoId"]').selectOption('1')
  await fill({ alimento: 'Dieta prescrita', quantidade: '50 g', fim: '2026-09-29' })
  await shot('agendamento')
  await save('Criar agendamentos')
  const scheduleWrite = writes.find(w => w.path === '/internacoes/1/alimentacao/agenda')
  assert.equal(
    scheduleWrite.body.horarios.length,
    5,
    'Past times today must be skipped, tomorrow must retain all times'
  )
  await page.getByRole('button', { name: 'Cancelar alimentação de Mel às 18:00' }).click()
  await fill({ motivo: 'Dieta revisada' })
  await save('Confirmar cancelamento')
  await page.getByText('Dieta revisada', { exact: true }).waitFor()
  await shot('checklist')
  await page.getByRole('button', { name: 'Concluídas', exact: true }).click()
  assert.equal(await rows.count(), 1)
  await page.getByRole('button', { name: 'Todas', exact: true }).click()
  await page.getByLabel('Dia do checklist', { exact: true }).fill('2026-09-29')
  await page.getByText('08:00', { exact: true }).waitFor()
  assert.equal(await rows.count(), 3)
  await page.getByPlaceholder('Nome do paciente ou baia').fill('inexistente')
  await page.getByRole('heading', { name: 'Nenhuma refeição neste filtro' }).waitFor()
  await page.getByPlaceholder('Nome do paciente ou baia').fill('')
  await page.setViewportSize({ width: 390, height: 844 })
  assert(
    await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth),
    'Checklist must fit mobile'
  )
  await shot('checklist-mobile')
  await visit('/pacientes?novo=1')
  await dialog().waitFor()
  assert(
    await dialog().evaluate(el => el.scrollWidth <= el.clientWidth),
    'Patient form must fit mobile'
  )
  const overflow = await page.evaluate(() =>
    [...document.querySelectorAll('body *')]
      .filter(e => e.getBoundingClientRect().right > innerWidth + 1)
      .map(e => ({
        tag: e.tagName,
        cls: e.className,
        width: e.getBoundingClientRect().width,
        right: e.getBoundingClientRect().right,
      }))
  )
  assert(
    await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth),
    JSON.stringify(overflow)
  )
  await shot('cadastro-mobile')
  assert.deepEqual(unexpected, [])
  assert.deepEqual(errors, [])
  console.log(
    'PASS: combined patient/tutor, photo upload/retry/removal, editing, admission shortcut, daily schedule, fasting, completion audit, cancellation, filters, date selection, desktop/mobile.'
  )
} catch (error) {
  await shot('failure')
  console.error({ errors, unexpected })
  throw error
} finally {
  await browser.close()
}
