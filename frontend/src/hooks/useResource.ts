import { useEffect, useState } from 'react'
import { readableError } from '../services/api'

export function useResource<T>(loader: () => Promise<T>, key: string | number) {
  const [data, setData] = useState<T>()
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [revision, setRevision] = useState(0)
  useEffect(() => {
    let active = true
    setLoading(true); setError(''); setData(undefined)
    loader().then(value => { if (active) setData(value) }).catch(e => { if (active) setError(readableError(e)) }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
    // The key includes every input used by the loader.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key, revision])
  return { data, error, loading, reload: () => setRevision(value => value + 1) }
}

