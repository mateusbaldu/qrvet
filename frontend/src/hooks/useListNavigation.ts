import { useEffect, useRef } from 'react'
import { useLocation, useSearchParams } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'

export function useListNavigation() {
  const [params, setParams] = useSearchParams()
  const location = useLocation()
  function updateQuery(values: Record<string, string | number | null>, replace = true) {
    setParams(previous => {
      const next = new URLSearchParams(previous)
      Object.entries(values).forEach(([name, value]) => {
        if (value === null || value === '') next.delete(name)
        else next.set(name, String(value))
      })
      return next
    }, { replace, state: location.state })
  }
  return { params, updateQuery, listLocation: location.pathname + location.search }
}

export function queryPage(params: URLSearchParams) {
  const value = Number(params.get('pagina') ?? 0)
  return Number.isSafeInteger(value) && value >= 0 ? value : 0
}

export function returnToList(state: unknown, path: string) {
  const target = (state as { returnTo?: unknown } | null)?.returnTo
  return typeof target === 'string' && (target === path || target.startsWith(path + '?')) ? target : path
}

// Positions are scoped to the account and exact list URL, inside this browser tab.
// Restore after asynchronous data arrives, when the list has its final height.
export function useListScroll(ready: boolean, enabled = true) {
  const location = useLocation()
  const { user } = useAuth()
  const key = `qrvet:list-scroll:${user?.id}:${user?.role}:${location.pathname}${location.search}`
  const restored = useRef(false)
  const position = useRef(0)
  useEffect(() => {
    const previous = history.scrollRestoration
    history.scrollRestoration = 'manual'
    return () => { history.scrollRestoration = previous }
  }, [])
  useEffect(() => {
    if (!enabled) { restored.current = false; return }
    if (!ready) return
    let frame = 0
    if (!restored.current) {
      let saved = 0
      try { saved = Number(sessionStorage.getItem(key)) || 0 } catch { /* Optional browser storage. */ }
      frame = requestAnimationFrame(() => {
        window.scrollTo({ top: saved, behavior: 'instant' })
        position.current = window.scrollY
        restored.current = true
      })
    } else position.current = window.scrollY
    const save = () => {
      if (!restored.current) return
      position.current = window.scrollY
      try { sessionStorage.setItem(key, String(position.current)) } catch { /* Navigation still works without storage. */ }
    }
    window.addEventListener('scroll', save, { passive: true })
    window.addEventListener('pagehide', save)
    return () => {
      cancelAnimationFrame(frame)
      window.removeEventListener('scroll', save)
      window.removeEventListener('pagehide', save)
      if (restored.current) {
        try { sessionStorage.setItem(key, String(position.current)) } catch { /* Optional browser storage. */ }
      }
    }
  }, [ready, key, enabled])
}
