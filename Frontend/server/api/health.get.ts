// ==========================================================================
// Health del frontend.
//
// NO toca el backend, y esa es toda la gracia.
//
// Hay dos Health en la arquitectura y se probea cada uno por separado:
//
//   GET /api/health   -> el frontend (este archivo). ¿El SSR vive?
//   GET /actuator/health en :8080 -> el backend. ¿Kotlin y la BD viven?
//
// Si este health,/api/health, llamara al backend, un fallo de la base de
// datos haria que el health del frontend fallara tambien, y no se podria
// distinguir "Nitro esta caido" de "Postgres esta caido". Cada pieza
// responde por lo suyo.
//
// Nota de ruteo: este archivo es una ruta ESTATICA (`health.get.ts`) y el
// proxy generico de la API es un COMODIN (`[...path].ts`). En el enrutador de
// Nitro las rutas estaticas tienen prioridad sobre los comodines, asi que
// `/api/health` llega aqui y no al proxy. Si algun dia este archivo desaparece,
// `/api/health` pasaria a caer en el proxy y devolveria el health del BACKEND:
// mismo 200, otro significado. Conviene saberlo.
// ==========================================================================

export default defineEventHandler(() => ({
  status: 'ok',
  service: 'frontend',
  timestamp: new Date().toISOString()
}))
