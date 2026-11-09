import {
  appendResponseHeader,
  defineEventHandler,
  getRequestHeader,
  getRouterParam,
  getQuery,
  setResponseHeader,
  type H3Event
} from 'h3'

const API_VERSION_PREFIX = '/api/v1'

const FORWARD_REQUEST_HEADERS = ['cookie', 'x-csrf-token'] as const

const FORWARD_RESPONSE_HEADERS = ['location', 'cache-control'] as const

export default defineEventHandler(async (event: H3Event) => {
  const config = useRuntimeConfig(event)

  const path = getRouterParam(event, 'path') ?? ''

  const target = `${config.apiBase}${API_VERSION_PREFIX}/${path}`
  const query = getQuery(event) as Record<string, string>

  const method = event.method
  const hasBody = method !== 'GET' && method !== 'HEAD'

  const requestHeaders: Record<string, string> = {
    accept: 'application/json'
  }

  for (const name of FORWARD_REQUEST_HEADERS) {
    const value = getRequestHeader(event, name)
    if (value) requestHeaders[name] = value
  }

  if (hasBody) requestHeaders['content-type'] = 'application/json'

  const forwardedFor =
    getRequestHeader(event, 'x-forwarded-for') ?? event.node.req.socket.remoteAddress
  if (forwardedFor) requestHeaders['x-forwarded-for'] = forwardedFor

  try {
    const response = await $fetch.raw(target, {
      method,
      query,
      headers: requestHeaders,
      body: hasBody ? await readBody(event).catch(() => undefined) : undefined
    })

    event.node.res.statusCode = response.status

    const contentType = response.headers.get('content-type')
    if (contentType) setResponseHeader(event, 'content-type', contentType)

    propagateResponseHeaders(event, response.headers)

    return response._data
  } catch (error: unknown) {
    if (error && typeof error === 'object' && 'response' in error) {
      const err = error as {
        response?: { status?: number; _data?: unknown; headers?: Headers }
        statusCode?: number
      }
      const status = err.response?.status ?? err.statusCode ?? 502
      event.node.res.statusCode = status

      // B2 (rama de error) - Un 401 puede venir con la cookie que caduca la
      // sesion. Si no se propaga aqui, el navegador conserva la cookie vieja
      // y el bucle 401 -> reintento -> 401 no acaba nunca.
      if (err.response?.headers) {
        propagateResponseHeaders(event, err.response.headers)
      }

      return err.response?._data ?? { status, detail: 'Error del backend' }
    }

    event.node.res.statusCode = 502
    return {
      status: 502,
      title: 'Backend no disponible',
      detail:
        'No se pudo contactar el backend. Revisa que el servicio "backend" ' +
        'este levantado y que runtimeConfig.apiBase apunte bien.'
    }
  }
})

function propagateResponseHeaders(event: H3Event, headers: Headers): void {
  // B2 - Todas las cookies, no solo la primera.
  for (const cookie of readSetCookies(headers)) {
    appendResponseHeader(event, 'set-cookie', cookie)
  }

  // location, cache-control y companyas.
  for (const name of FORWARD_RESPONSE_HEADERS) {
    const value = headers.get(name)
    if (value) setResponseHeader(event, name, value)
  }
}

function readSetCookies(headers: Headers): string[] {
  if (typeof headers.getSetCookie === 'function') {
    const cookies = headers.getSetCookie()
    if (cookies.length > 0) return cookies
  }

  const raw = headers.get('set-cookie')
  if (!raw) return []

  return raw
    .split(/,(?=\s*[^;=]+=[^;]*)/)
    .map((cookie) => cookie.trim())
    .filter(Boolean)
}
function useRuntimeConfig(event: H3Event) {
  throw new Error('Function not implemented.')
}

