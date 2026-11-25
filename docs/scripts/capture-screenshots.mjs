// Captures the real React application with fictitious data; no backend writes.
import { createRequire } from 'node:module'
import { mkdir, readFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
const require = createRequire(new URL('../../frontend/package.json', import.meta.url))
const { chromium } = require('playwright')
const output = new URL('../images/', import.meta.url)
await mkdir(output, { recursive: true })
const photos = await Promise.all(['luna', 'thor'].map(name => readFile(new URL(`../../figma-design/assets/${name}.jpg`, import.meta.url))))
const qrImage = (await readFile(new URL('../images/qr-demo.png', import.meta.url))).toString('base64')
const base = process.env.TEST_BASE_URL || 'http://127.0.0.1:5173'
const browser = await chromium.launch({ headless: true, ...(process.env.PLAYWRIGHT_CHANNEL ? { channel: process.env.PLAYWRIGHT_CHANNEL } : {}) })
const context = await browser.newContext({ viewport: { width: 1440, height: 1000 }, deviceScaleFactor: 1, locale: 'pt-BR', timezoneId: 'America/Sao_Paulo' })
const page = await context.newPage()
const errors = []
page.on('pageerror', e => errors.push(e.message))
page.setDefaultTimeout(10000)
await page.clock.setFixedTime(new Date('2026-11-10T17:30:00Z'))
await page.addInitScript(() => {
  for (const role of ['ADMIN', 'VETERINARIO', 'AUXILIAR_TECNICO']) localStorage.setItem('qrvet:onboarding:v1:2:' + role, JSON.stringify({ seen: true, step: 0 }))
})
const alerts = { alergias: 'Alergia relatada pelo tutor. Conferir orientação clínica.', restricoesAlimentares: 'Dieta gastrointestinal', cuidadosManejo: 'Manipular com calma. Sensível a ruídos.' }
const tutors = [
  { id: 1, nome: 'Ana Silva', cpf: '52998224725', telefone: '11999999999', email: 'ana@example.com', endereco: 'Endereço de demonstração' },
  { id: 2, nome: 'Carlos Mendes', cpf: '11144477735', telefone: '11988888888', email: 'carlos@example.com', endereco: 'Endereço de demonstração' },
]
const patients = [
  { id: 1, tutorId: 1, tutorNome: 'Ana Silva', nome: 'Luna', especie: 'Felina', raca: 'SRD', sexo: 'Fêmea', dataNascimento: '2021-04-12', peso: 4.5, fotoVersao: 'luna.jpg', observacoes: 'Sensível a ruídos.', alertas: alerts },
  { id: 2, tutorId: 2, tutorNome: 'Carlos Mendes', nome: 'Thor', especie: 'Canina', raca: 'Golden Retriever', sexo: 'Macho', dataNascimento: '2020-08-03', peso: 24, fotoVersao: 'thor.jpg', observacoes: '', alertas: { alergias: null, restricoesAlimentares: null, cuidadosManejo: null } },
]
const admissions = patients.map((p, index) => ({ id: index + 1, pacienteId: p.id, pacienteNome: p.nome, fotoVersao: p.fotoVersao, baiaId: index + 1, baiaIdentificacao: `A-0${index + 1}`, veterinarioId: 2, veterinarioNome: 'Marina Silva', status: 'ATIVA', entradaInternacao: '2026-11-10T12:00:00Z', saidaInternacao: null, motivo: 'Observação e acompanhamento clínico', diagnosticoInicial: 'Avaliação da equipe veterinária', observacoes: 'Manter em ambiente tranquilo.', uuidToken: 'demonstracao', alertas: p.alertas }))
const bays = Array.from({ length: 8 }, (_, i) => ({ id: i + 1, identificacao: `A-0${i + 1}`, status: i < 2 ? 'OCUPADA' : i === 7 ? 'MANUTENCAO' : 'DISPONIVEL', observacao: '' }))
const meals = [
  [1, 1, '11:00', 'PENDENTE'], [2, 2, '15:00', 'PENDENTE'], [3, 1, '18:00', 'PENDENTE'], [4, 2, '08:00', 'CONCLUIDA'],
].map(([id, admissionId, time, status]) => ({ id, internacaoId: admissionId, pacienteNome: admissions[admissionId - 1].pacienteNome, baiaIdentificacao: admissions[admissionId - 1].baiaIdentificacao, internacaoStatus: 'ATIVA', alimento: 'Ração úmida', quantidade: '40 g', horario: `2026-11-10T${String(Number(time.slice(0, 2)) + 3).padStart(2, '0')}:00:00Z`, status, jejumAtivo: false, concluidaEm: status === 'CONCLUIDA' ? '2026-11-10T11:05:00Z' : null, responsavelNome: status === 'CONCLUIDA' ? 'Marina Silva' : null, observacao: status === 'CONCLUIDA' ? 'Boa aceitação.' : null, motivoCancelamento: null }))
const notes = [
  { id: 3, texto: 'Paciente confortável e alerta. Aceitou a dieta oferecida. Mantido o acompanhamento da equipe.', dataHoraRegistro: '2026-11-10T17:20:00Z' },
  { id: 2, texto: 'Tutor trouxe a dieta habitual. Informações conferidas com a equipe.', dataHoraRegistro: '2026-11-10T15:10:00Z' },
  { id: 1, texto: 'Avaliação inicial registrada. Paciente acomodada na baia A-01.', dataHoraRegistro: '2026-11-10T12:15:00Z' },
].map(n => ({ ...n, internacaoId: 1, profissionalId: 2, profissionalNome: 'Marina Silva' }))
function paged(items, url) {
  const p = Number(url.searchParams.get('page') || 0), size = Number(url.searchParams.get('size') || 12)
  return { items: items.slice(p * size, (p + 1) * size), page: p, size, total: items.length, pages: Math.ceil(items.length / size) }
}
await page.route('**/qrvet/api/v1/**', route => {
  const request = route.request(), url = new URL(request.url()), path = url.pathname.split('/v1')[1]
  const send = (data, status = 200) => route.fulfill({ status, contentType: 'application/json', ...(status === 204 ? {} : { body: JSON.stringify(data) }) })
  if (path === '/auth/refresh') return send({ accessToken: 'demonstracao' })
  if (path === '/auth/heartbeat') return send(null, 204)
  if (path === '/auth/csrf') return send({ token: 'demonstracao', headerName: 'X-XSRF-TOKEN' })
  if (path === '/auth/me') return send({ id: 2, name: 'Marina Silva', email: 'marina@example.com', role: 'ADMIN', active: true, confirmed: true })
  if (request.method() !== 'GET') return send({ message: 'Captura de documentação não grava dados.' }, 405)
  if (path.endsWith('/foto')) {
    const id = Number(path.split('/')[2])
    return route.fulfill({ contentType: 'image/jpeg', body: photos[id === 2 ? 1 : 0] })
  }
  if (path === '/pacientes') return send(paged(patients, url))
  if (/^\/pacientes\/\d+$/.test(path)) return send(patients.find(p => p.id === Number(path.split('/')[2])))
  if (path === '/tutores') return send(paged(tutors, url))
  if (path === '/baias') return send(paged(bays, url))
  if (['/internacoes', '/internacoes/ativas', '/internacoes/cuidados'].includes(path)) return send(paged(admissions, url))
  if (path === '/internacoes/veterinarios') return send([{ id: 2, name: 'Marina Silva' }])
  if (/^\/internacoes\/\d+(\/cuidados)?$/.test(path)) return send(admissions.find(i => i.id === Number(path.split('/')[2])))
  if (/^\/internacoes\/\d+\/evolucoes$/.test(path)) return send(paged(notes, url))
  if (/^\/internacoes\/\d+\/(jejum|alimentacao|medicacao)$/.test(path)) return send([])
  if (path === '/alimentacao/agenda') return send(meals.filter(m => !url.searchParams.has('internacaoId') || m.internacaoId === Number(url.searchParams.get('internacaoId'))))
  if (path === '/internacoes/1/qrcode') return send({ uuidToken: 'demonstracao', base64: qrImage, url: base + '/internacoes/1', tutorUrl: base + '/public/internacoes/qr/demonstracao', tutorEmail: 'ana@example.com' })
  if (path === '/public/internacoes/qr/demonstracao') return send({ pacienteNome: 'Luna', especie: 'Felina', raca: 'SRD', sexo: 'Fêmea', baiaIdentificacao: 'A-01', motivo: 'Observação e acompanhamento clínico', status: 'ATIVA', jejumAtivo: false, entradaInternacao: '2026-11-10T12:00:00Z' })
  errors.push('Rota sem dados fictícios: ' + path)
  return send({ message: 'Rota não preparada para captura.' }, 404)
})
async function capture(name, path, heading, width = 1440, height = 1000) {
  await page.setViewportSize({ width, height })
  await page.goto(base + path, { waitUntil: 'networkidle' })
  await page.getByRole('heading', { name: heading, exact: true }).first().waitFor()
  await page.evaluate(() => document.fonts.ready)
  await page.locator('img').evaluateAll(images => Promise.all(images.map(img => img.complete ? Promise.resolve() : new Promise(resolve => { img.onload = resolve; img.onerror = resolve }))))
  await page.screenshot({ path: fileURLToPath(new URL(name + '.png', output)), fullPage: true, animations: 'disabled' })
  console.log('Captura salva: ' + name)
}
try {
  await capture('dashboard', '/inicio', 'Visão geral da clínica')
  await capture('pacientes', '/pacientes', 'Pacientes')
  await capture('internacao', '/internacoes/1', 'Luna')
  await capture('evolucoes', '/internacoes/1?aba=evolucoes', 'Luna')
  await capture('alimentacao', '/alimentacao?dia=2026-11-10', 'Alimentação')
  await capture('cuidados-mobile', '/cuidados', 'Cuidados', 390, 844)
  await capture('tutor-mobile', '/public/internacoes/qr/demonstracao', 'Luna', 390, 844)
  if (errors.length) throw new Error(errors.join('\n'))
} finally {
  await browser.close()
}
