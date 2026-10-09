import { defineEventHandler, getRouterParam, setHeader, type H3Event } from 'h3'

const API_VERSION_PREFIX = '/api/v1'

export default defineEventHandler(async (event: H3Event) => {
  const config = useRuntimeConfig(event)
  const path = getRouterParam(event, 'path') ?? ''
  const target = `${config.apiBase}${API_VERSION_PREFIX}/${path}`
  const query = Object.fromEntries(
    Object.entries(event.node.req.query ?? {}).map(([k, v]) => [k, String(v)])
  )
  const method = event.method as 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH'

  try {
    const response = await $fetch.raw(target, {
      method,
      query,
      headers: {
        accept: 'application/json',
        ...(method !== 'GET' && method !== 'HEAD'
          ? { 'content-type': 'application/json' }
          : {})
      },
      body:
        method !== 'GET' && method !== 'HEAD'
          ? await readBody(event).catch(() => undefined)
          : undefined
    })

    event.node.res.statusCode = response.status

    const contentType = response.headers.get('content-type')
    if (contentType) setHeader(event, 'content-type', contentType)

    return response._data
  } catch (error: unknown) {

    if (error && typeof error === 'object' && 'response' in error) {
      const err = error as {
        response?: { status?: number; _data?: unknown }
        statusCode?: number
      }
      const status = err.response?.status ?? err.statusCode ?? 502
      event.node.res.statusCode = status
      return err.response?._data ?? { status, detail: 'Error del backend' }
    }

    event.node.res.statusCode = 502
    return {
      status: 502,
      title: 'Backend no disponible',
      detail:
        'No se pudo contactar el backend. Revisa que el servicio "backend" ' +
        'este levantado y que apiBase apunte bien.'
    }
  }
})