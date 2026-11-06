import assert from 'node:assert/strict'
import { mkdir } from 'node:fs/promises'
import { chromium } from 'playwright'

const browser = await chromium.launch({ headless: true, ...(process.env.PLAYWRIGHT_CHANNEL ? { channel: process.env.PLAYWRIGHT_CHANNEL } : {}) })
const base = process.env.TEST_BASE_URL || 'http://127.0.0.1:5173'
const screenshots = new URL('../test-results/onboarding/', import.meta.url)
await mkdir(screenshots, { recursive: true })
const errors = []
try {
  for (const [role, count] of [['ADMIN', 5], ['VETERINARIO', 4], ['RECEPCIONISTA', 3], ['AUXILIAR_TECNICO', 2], ['TUTOR', 1]]) {
    const context = await browser.newContext({ viewport: { width: 1280, height: 900 } })
    const page = await context.newPage()
    page.on('pageerror', error => errors.push(error.message))
    await page.route('**/qrvet/api/v1/**', route => {
      const path = new URL(route.request().url()).pathname.split('/v1')[1]
      let data = { items: [], total: 0, pages: 0, page: 0, size: 12 }
      if (path === '/auth/csrf') data = { token: 'csrf', headerName: 'X-XSRF-TOKEN' }
      if (path === '/auth/refresh') data = { accessToken: 'test' }
      if (path === '/auth/me') data = { id: 1, name: 'Marina Silva', role, email: 'marina@example.com', active: true, confirmed: true }
      return route.fulfill({ contentType: 'application/json', body: JSON.stringify(data) })
    })
    await page.goto(base + '/consultar-qr')
    await page.getByRole('button', { name: 'Conhecer o sistema' }).click()
    const dialog = page.getByRole('dialog')
    await dialog.waitFor()
    assert.equal(await dialog.locator('h3').evaluate(el => el === document.activeElement), true)
    assert.match(await dialog.innerText(), new RegExp(`Passo 1 de ${count}`))
    if (count > 1) {
      await dialog.getByRole('button', { name: 'Próximo' }).click()
      await dialog.getByRole('button', { name: 'Continuar depois' }).click()
      await page.reload()
      assert.equal(await page.getByRole('button', { name: 'Conhecer o sistema' }).count(), 0)
      await page.getByRole('button', { name: 'Ajuda: guia do QRVet' }).click()
      assert.match(await dialog.innerText(), new RegExp(`Passo 2 de ${count}`))
      await dialog.getByRole('button', { name: 'Anterior' }).click()
    }
    if (role === 'ADMIN') {
      await page.screenshot({ path: new URL('desktop.png', screenshots).pathname.replace(/^\/(\w:)/, '$1') })
      await page.setViewportSize({ width: 360, height: 800 })
      await page.screenshot({ path: new URL('mobile.png', screenshots).pathname.replace(/^\/(\w:)/, '$1') })
      assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), true)
      const box = await dialog.boundingBox()
      assert(box.x >= 0 && box.x + box.width <= 360)
    }
    for (let n = 1; n < count; n++) await dialog.getByRole('button', { name: 'Próximo' }).click()
    await dialog.getByRole('button', { name: 'Concluir guia' }).click()
    await page.reload()
    assert.equal(await page.getByRole('button', { name: 'Conhecer o sistema' }).count(), 0)
    const help = page.getByRole('button', { name: 'Ajuda: guia do QRVet' })
    await help.click()
    assert.match(await dialog.innerText(), new RegExp(`Passo 1 de ${count}`))
    await page.keyboard.press('Escape')
    await dialog.waitFor({ state: 'detached' })
    assert.equal(await help.evaluate(el => el === document.activeElement), true)
    const tip = page.getByRole('button', { name: 'Ajuda sobre Consultar QR Code' })
    await tip.focus()
    await page.getByRole('tooltip').waitFor()
    await page.keyboard.press('Escape')
    assert.equal(await page.getByRole('tooltip').count(), 0)
    await tip.click()
    await page.getByRole('tooltip').waitFor()
    await page.getByRole('heading', { name: 'Encontre a internação' }).click()
    assert.equal(await page.getByRole('tooltip').count(), 0)
    await context.close()
  }
  assert.deepEqual(errors, [])
  console.log('Onboarding: five roles, resume, completion, replay, keyboard, tooltips and mobile passed.')
} finally { await browser.close() }
