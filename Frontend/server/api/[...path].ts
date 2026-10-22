// ==========================================================================
// Proxy delgado hacia el backend Kotlin.
//
// REGLA INNEGOCIABLE: aqui NO hay logica de negocio. Ni precios, ni
// validaciones, ni reglas. Eso vive en Kotlin. Si este archivo empieza a
// decidir cosas, se rompio la arquitectura: habria dos fuentes de verdad.
//
// Lo que si hace, y por que vale la pena:
//
//   1. Oculta la URL interna de Kotlin. El navegador nunca la ve.
//   2. Reescribe /api/<algo> hacia <apiBase>/api/v1/<algo>.
//      El navegador escribe "/api/products" y nunca "/api/v1/products".
//      Cuando llegue la v2 se cambia SOLO la constante API_VERSION_PREFIX,
//      y ni el frontend web ni la app Android se tocan.
//   3. Un solo origen, por lo que NO hay CORS que configurar nunca.
//   4. Punto unico para meter cabeceras: sesion, tokens, CORS de emergencia.
//
// ---------------------------------------------------------------------------
// SESION Y COOKIES (BFF) - esto es lo que hace que el patron funcione
// ---------------------------------------------------------------------------
// El patron BFF (D3) es: el navegador NO guarda tokens. Kotlin mantiene la
// sesion y devuelve una cookie HttpOnly; el navegador la reenvia sola en
// cada peticion.
//
// Para que eso funcione, el proxy tiene que hacer DOS cosas, y antes no
// hacia ninguna:
//
//   B1 - Reenviar la cookie ENTRANTE.
//        Sin esto, Kotlin nunca ve la sesion y responde 401 a todo.
//
//   B2 - Reenviar el Set-Cookie SALIENTE.
//        Sin esto, la cookie que Kotlin emite se queda en el servidor de
//        Nitro y el navegador nunca la recibe: no hay sesion que exista.
//
// Ojo con el detalle de B2: una respuesta puede llevar VARIAS cookies
// (por ejemplo la de sesion y la que la invalida), y por eso hay que
// anexar una a una. Con setHeader, la segunda machaca la primera y una
// sola cookie devorada rompe toda la sesion.
//
// Y hay que hacerlo TAMBIEN en la rama de error: un 401 que invalida la
// sesion llega con su propio Set-Cookie (la que la caduca), y si esa rama
// no lo propaga, el navegador se queda con la cookie vieja para siempre.
//
// OJO con multipart: /api/media-assets/upload sube archivos binarios (GLB, JPG).
// Esta ruta generica lee el cuerpo como JSON, asi que una subida pasaria como
// texto y llegaria corrupta al backend. Para eso hace falta una ruta propia
// que use readMultipartFormData(). Pendiente cuando se implemente el visor.
// ==========================================================================

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

// Cabeceras que SI se reenvian al backend. Lista explicita y corta, nunca
// "reenviar todo": cada cabecera que pasa al backend es una cabecera que
// puede acabar en manos de un tercero.
//
//   cookie        -> la sesion (B1)
//   x-csrf-token  -> el token CSRF, por si Kotlin decide validarlo tambien
//   x-forwarded-for -> la IP del cliente, que es la clave del limitador de
//                      peticiones del backend (F4.34). Kotlin no puede
//                      deducirla del socket: desde dentro de la red de
//                      Compose todas las peticiones llegan desde el mismo
//                      contenedor de Nitro, y un limite global de 5/min
//                      bloquearia a todos los usuarios a la vez.
//
// Todo lo demas que llegue del navegador (x-forwarded-*, cabeceras de
// rastreo) se queda en el borde: el backend no las necesita y no debe
// fiarse de ellas.
const FORWARD_REQUEST_HEADERS = ['cookie', 'x-csrf-token'] as const


// Cabeceras de la RESPUESTA de Kotlin que se propagan al navegador.
//
//   location     -> los 302 de verificacion de correo y reset de contrasena
//                   (F3.3). Sin esto, ese flujo se queda en un JSON 302.
//   cache-control-> "no-store" en las respuestas de auth (F3.4). Una
//                   respuesta de sesion cacheada en un proxy intermedio es
//                   una sesion filtrada.
const FORWARD_RESPONSE_HEADERS = ['location', 'cache-control'] as const

export default defineEventHandler(async (event: H3Event) => {
  const config = useRuntimeConfig(event)

  // El comodin [...path] devuelve todo lo que viene despues de /api/,
  // por ejemplo "products/123".
  const path = getRouterParam(event, 'path') ?? ''

  const target = `${config.apiBase}${API_VERSION_PREFIX}/${path}`
  const query = getQuery(event) as Record<string, string>

  const method = event.method
  const hasBody = method !== 'GET' && method !== 'HEAD'

  const requestHeaders: Record<string, string> = {
    accept: 'application/json'
  }

  // B1 - La cookie de sesion viaja con la peticion. Sin esta linea, el
  // backend nunca ve la sesion y todo lo privado responde 401.
  for (const name of FORWARD_REQUEST_HEADERS) {
    const value = getRequestHeader(event, name)
    if (value) requestHeaders[name] = value
  }

  if (hasBody) requestHeaders['content-type'] = 'application/json'

  // F4.34 — La IP del cliente, para el limitador de peticiones del backend.
  //
  // Se reenvia TAL CUAL llega, y Caddy la reescribe con la IP real antes de
  // que llegue aqui (header_up X-Forwarded-For {remote_host}), asi que la
  // cadena es cliente -> Caddy -> Nitro -> Kotlin y el valor es fiable.
  //
  // Si no viene ninguna (por ejemplo en desarrollo, sin Caddy delante), se
  // usa la direccion del socket: en ese caso la IP es la del cliente local
  // y el limite aplica solo a el.
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

    // El status real del backend se propaga tal cual (404, 409, 400...).
    // Sin esto el navegador veria siempre 200 y no podria distinguir un
    // 404 de un 409, y el frontend no podria mostrar el error correcto.
    event.node.res.statusCode = response.status

    const contentType = response.headers.get('content-type')
    if (contentType) setResponseHeader(event, 'content-type', contentType)

    propagateResponseHeaders(event, response.headers)

    return response._data
  } catch (error: unknown) {
    // $fetch lanza FetchError con .response cuando el backend responde con
    // 4xx/5xx. Eso NO es un fallo del proxy: hay que reenviar su status y su
    // cuerpo, no convertirlo en un 500 generico que oculta el problema real.
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

    // Sin respuesta del backend: esta caido, o no resuelve el host.
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

// ---------------------------------------------------------------------------
// Utilidades
// ---------------------------------------------------------------------------

/**
 * Propaga al navegador las cabeceras de la respuesta de Kotlin.
 *
 * Set-Cookie es el caso especial: puede venir mas de una vez y hay que
 * ANEXARLAS, no reemplazarlas. Si solo se pusiera la ultima, perder la
 * cookie de sesion deja al usuario fuera de su cuenta.
 */
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

/**
 * Lee todas las cabeceras Set-Cookie de una respuesta.
 *
 * `getSetCookie()` es el metodo estandar (Node 18.14+ / undici). Se
 * mantiene el respaldo con `get('set-cookie')` partido a mano por si el
 * runtime usa una implementacion de Headers que no lo expone.
 */
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
