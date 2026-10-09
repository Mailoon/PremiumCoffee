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
// OJO con multipart: /api/media-assets/upload sube archivos binarios (GLB, JPG).
// Esta ruta generica lee el cuerpo como JSON, asi que una subida pasaria como
// texto y llegaria corrupta al backend. Para eso hace falta una ruta propia
// que use readMultipartFormData(). Pendiente cuando se implemente el visor.
// ==========================================================================

import { defineEventHandler, getRouterParam, getQuery, setHeader, type H3Event } from 'h3'

/**
 * Version de la API de Kotlin a la que se reenvia.
 *
 * NO cambiar esto por otra version sin revisar los tests del proxy: es el
 * unico punto del frontend que conoce el versionado.
 */
const API_VERSION_PREFIX = '/api/v1'

export default defineEventHandler(async (event: H3Event) => {
  const config = useRuntimeConfig(event)

  // El comodin [...path] devuelve todo lo que viene despues de /api/,
  // por ejemplo "products/123".
  const path = getRouterParam(event, 'path') ?? ''

  const target = `${config.apiBase}${API_VERSION_PREFIX}/${path}`

  // Query string con getQuery(), NO con event.node.req.query.
  //
  // Es un detalle que fallo en silencio y rompio la paginacion entera:
  // req.query llega VACIO cuando la ruta usa un comodin, asi que un
  // "?page=2&size=7" se perdia y el backend respondia con sus defaults
  // page=0 size=20. Como la respuesta era 200 igual, nada delata el fallo:
  // solo se nota que la pagina 2 nunca trae datos.
  // getQuery() lee la URL cruda del evento y si funciona con comodines.
  const query = getQuery(event) as Record<string, string>

  const method = event.method as 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH'
  const hasBody = method !== 'GET' && method !== 'HEAD'

  try {
    const response = await $fetch.raw(target, {
      method,
      query,
      headers: {
        accept: 'application/json',
        ...(hasBody ? { 'content-type': 'application/json' } : {})
      },
      body: hasBody ? await readBody(event).catch(() => undefined) : undefined
    })

    // El status real del backend se propaga tal cual (404, 409, 400...).
    // Sin esto el navegador veria siempre 200 y no podria distinguir un
    // 404 de un 409, y el frontend no podria mostrar el error correcto.
    event.node.res.statusCode = response.status

    const contentType = response.headers.get('content-type')
    if (contentType) setHeader(event, 'content-type', contentType)

    return response._data
  } catch (error: unknown) {
    // $fetch lanza FetchError con .response cuando el backend responde con
    // 4xx/5xx. Eso NO es un fallo del proxy: hay que reenviar su status y su
    // cuerpo, no convertirlo en un 500 generico que oculta el problema real.
    if (error && typeof error === 'object' && 'response' in error) {
      const err = error as {
        response?: { status?: number; _data?: unknown }
        statusCode?: number
      }
      const status = err.response?.status ?? err.statusCode ?? 502
      event.node.res.statusCode = status
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