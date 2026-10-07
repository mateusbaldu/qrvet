import { useCallback, useEffect, useState } from 'react'
import { readableError } from '../services/api'

export function useResource<T>(loader: () => Promise<T>, dependencies: readonly unknown[]) {
  const [data, setData] = useState<T>()
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [revision, setRevision] = useState(0)
  const dependencyKey = JSON.stringify(dependencies)
  useEffect(() => {
    let active = true
    setLoading(true)
    setError('')
    setData(undefined)
    loader()
      .then(value => {
        if (active) setData(value)
      })
      .catch(e => {
        if (active) setError(readableError(e))
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [dependencyKey, revision])
  const reload = useCallback(() => setRevision(value => value + 1), [])
  return { data, error, loading, reload }
}
