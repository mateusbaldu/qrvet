import assert from 'node:assert/strict'
import { mkdir } from 'node:fs/promises'
import { chromium } from 'playwright'

const base = process.env.TEST_BASE_URL || 'http://127.0.0.1:5173'
const browser = await chromium.launch({
  headless: true,
  ...(process.env.PLAYWRIGHT_CHANNEL ? { channel: process.env.PLAYWRIGHT_CHANNEL } : {}),
})
const page = await browser.newPage({ timezoneId: 'America/Sao_Paulo', viewport: { width: 1440, height: 960 } })
const errors = []
const reportQueries = []
const exportQueries = []
let role = 'ADMIN'
let failReport = false
let failExport = true

const admissions = { totalNoPeriodo: 12, ativas: 3, altas: 8, obitos: 1, encerradasNoPeriodo: 9, tempoMedioInternacaoHoras: 36.5 }
const care = {
  evolucoesRegistradas: 28, medicacoesAplicadas: 45, alimentacoesRegistradas: 60, refeicoesAgendadas: 70,
  refeicoesConcluidas: 60, refeicoesPendentes: 6, refeicoesCanceladas: 4, jejunsIniciados: 5,
}
const vet = {
  veterinarioId: 2, veterinarioNome: 'Helena Costa', veterinarioEmail: 'helena@example.test',
  totalInternacoes: 8, ativas: 2, altas: 5, obitos: 1, evolucoesRegistradas: 19,
}
const professional = {
  usuarioId: 5, nome: 'Joana Silva', email: 'joana@example.test', role: 'AUXILIAR_TECNICO',
  evolucoesRegistradas: 4, medicacoesAplicadas: 35, alimentacoesRegistradas: 50, totalAcoesClinicas: 89,
}
const bay = { baiaId: 1, identificacao: 'B-01', statusAtual: 'OCUPADA', totalInternacoesNoPeriodo: 5 }
const appliedQuery = {
  startDate: '2026-11-01T03:00:00.000Z', endDate: '2026-12-01T02:59:59.999Z', veterinarioId: '2',
}
const csv = '\uFEFFPERÍODO DO RELATÓRIO\r\nIndicador,Valor\r\n'

page.on('pageerror', error => errors.push(error.message))
await page.addInitScript(() => {
  for (const role of ['ADMIN', 'VETERINARIO']) {
    localStorage.setItem('qrvet:onboarding:v1:1:' + role, JSON.stringify({ seen: true, step: 0 }))
  }
})
await page.route('**/qrvet/api/v1/**', route => {
  const url = new URL(route.request().url())
  const path = url.pathname.split('/v1')[1]
  const send = (data, status = 200) => route.fulfill({ status, json: data })
  if (path === '/auth/csrf') return send({ token: 'test', headerName: 'X-XSRF-TOKEN' })
  if (path === '/auth/refresh') return send({ accessToken: 'test' })
  if (path === '/auth/me') return send({ id: 1, name: 'Daniel Souza', email: 'daniel@example.test', role, active: true, confirmed: true })
  if (path === '/auth/heartbeat') return route.fulfill({ status: 204 })
  if (path === '/internacoes/veterinarios') return send([{ id: 2, name: 'Helena Costa' }])
  if (path === '/relatorios/clinica') {
    reportQueries.push(Object.fromEntries(url.searchParams))
    assert.equal(route.request().headers().authorization, 'Bearer test')
    if (failReport) return send({ message: 'Serviço indisponível.' }, 503)
    const veterinarian = url.searchParams.get('veterinarioId')
    const empty = url.searchParams.has('startDate') && !veterinarian
    return send({
      periodo: {
        startDate: url.searchParams.get('startDate'), endDate: url.searchParams.get('endDate'),
        veterinarioId: veterinarian ? Number(veterinarian) : null,
      },
      resumoInternacoes: empty ? Object.fromEntries(Object.keys(admissions).map(key => [key, 0])) : {
        ...admissions,
        ...(veterinarian ? { totalNoPeriodo: 8, ativas: 2, altas: 5, obitos: 1, encerradasNoPeriodo: 6 } : {}),
      },
      resumoCuidados: empty ? Object.fromEntries(Object.keys(care).map(key => [key, 0])) : care,
      atendimentosPorVeterinario: empty ? [{ ...vet, totalInternacoes: 0, ativas: 0, altas: 0, obitos: 0, evolucoesRegistradas: 0 }] : [vet],
      produtividadeEquipe: empty ? [] : [professional],
      utilizacaoBaias: empty ? [{ ...bay, totalInternacoesNoPeriodo: 0 }] : [bay],
    })
  }
  if (path === '/relatorios/clinica/export') {
    exportQueries.push(Object.fromEntries(url.searchParams))
    assert.equal(route.request().headers().authorization, 'Bearer test')
    if (failExport) return send({ message: 'Exportação indisponível.' }, 503)
    return route.fulfill({ contentType: 'text/csv; charset=UTF-8', body: csv })
  }
  return send({}, 404)
})

try {
  await page.goto(base + '/meu-perfil')
  await page.getByRole('link', { name: 'Relatórios', exact: true }).click()
  await page.waitForURL('**/relatorios')
  const stats = page.getByRole('region', { name: 'Internações', exact: true })
  const opened = stats.locator('.report-stat').filter({ hasText: 'Abertas no período' }).locator('dd')
  await opened.filter({ hasText: /^12$/ }).waitFor()
  const initialQueries = [...reportQueries]
  assert(initialQueries.length && initialQueries.every(query => Object.keys(query).length === 0))
  await stats.getByText('36,5 h', { exact: true }).waitFor()
  await page.getByRole('table', { name: 'Produtividade da equipe' }).getByText('Auxiliar técnico', { exact: true }).waitFor()
  await page.getByRole('table', { name: 'Utilização das baias' }).getByText('Ocupada', { exact: true }).waitFor()

  const start = page.getByLabel('Data inicial')
  const end = page.getByLabel('Data final')
  const vetFilter = page.getByLabel('Veterinário responsável')
  const apply = page.getByRole('button', { name: 'Aplicar filtros' })
  const downloadButton = page.getByRole('button', { name: 'Baixar relatório (CSV)' })
  await start.fill('2026-11-01')
  await end.fill('2026-10-31')
  await vetFilter.selectOption('2')
  assert(await downloadButton.isDisabled())
  await apply.click()
  await page.getByRole('alert').getByText('A data final deve ser igual ou posterior à data inicial.').waitFor()
  assert.deepEqual(reportQueries, initialQueries)

  await end.fill('2026-11-30')
  await apply.click()
  await opened.filter({ hasText: /^8$/ }).waitFor()
  await page.getByText('01/11/2026 a 30/11/2026', { exact: true }).waitFor()
  assert.deepEqual(reportQueries, [...initialQueries, appliedQuery])

  await downloadButton.click()
  await page.getByRole('alert').getByText(/Exportação indisponível/).waitFor()
  failExport = false
  const [download] = await Promise.all([page.waitForEvent('download'), downloadButton.click()])
  assert.equal(download.suggestedFilename(), 'relatorio-performance-clinica.csv')
  const stream = await download.createReadStream()
  const buffers = []
  for await (const chunk of stream) buffers.push(chunk)
  assert.equal(Buffer.concat(buffers).toString('utf8'), csv)
  assert.deepEqual(exportQueries, [appliedQuery, appliedQuery])

  await mkdir('tests/artifacts', { recursive: true })
  for (const width of [1440, 1024, 768, 390, 320]) {
    await page.setViewportSize({ width, height: 960 })
    assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), `Report page overflow at ${width}px`)
    const baysTable = page.getByRole('table', { name: 'Utilização das baias' })
    assert(await baysTable.evaluate(table => table.parentElement.scrollWidth <= table.parentElement.clientWidth), `Bay table overflow at ${width}px`)
    if (width === 1440 || width === 390) {
      await page.screenshot({ path: `tests/artifacts/reports-${width}.png`, fullPage: true, animations: 'disabled' })
    }
  }
  await page.setViewportSize({ width: 1440, height: 960 })

  failReport = true
  await end.fill('')
  await vetFilter.selectOption('')
  await apply.click()
  await page.getByRole('alert').getByText('Serviço indisponível.').waitFor()
  assert(await downloadButton.isDisabled())
  assert.deepEqual(reportQueries.at(-1), { startDate: appliedQuery.startDate })
  failReport = false
  await page.getByRole('button', { name: 'Tentar novamente' }).click()
  await page.getByText('Desde 01/11/2026', { exact: true }).waitFor()
  await page.getByText('Nenhuma ação clínica registrada para estes filtros.').waitFor()
  await page.getByRole('button', { name: 'Limpar', exact: true }).click()
  await opened.filter({ hasText: /^12$/ }).waitFor()
  assert.deepEqual(reportQueries.at(-1), {})

  role = 'VETERINARIO'
  const reportCount = reportQueries.length
  await page.goto(base + '/relatorios')
  await page.waitForURL('**/meu-perfil')
  assert.equal(await page.getByRole('link', { name: 'Relatórios', exact: true }).count(), 0)
  assert.equal(reportQueries.length, reportCount)
  assert.deepEqual(errors, [])
  console.log('Relatórios: consultas, filtros, horários, CSV, falhas, permissões e layout responsivo verificados.')
} finally {
  await browser.close()
}
