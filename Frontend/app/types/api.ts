// ==========================================================================
// Tipos de las respuestas que devuelve Kotlin.
//
// Por que este archivo existe:
//
// useFetch('/api/categories') sin tipo devuelve Promise<DataT | null> donde
// DataT cae a un `{}` generico. Entonces `data.value.totalElements` da
//   error TS2339: Property 'totalElements' does not exist on type '{}'
// que es un error REAL de tipado (no un falso), pero cuya causa es que no
// declaramos la forma de la respuesta. Sin este archivo no hay forma de que
// el compilador lo sepa.
//
// Estos tipos NO se inventan: son la transcripcion de los data class de
// Kotlin. Si cambias un DTO en Backend/src/main/kotlin/Coffee/.../DTO/,
// cambia aqui el tipo que lo refleja.
//
//   PageResponse<T>  <-  Coffee.Common.DTO.PageResponse
//   CategoryResponse <-  Coffee.Category.DTO.CategoryResponse
// ==========================================================================

/**
 * Envoltura de paginacion que devuelve Spring Data via Kotlin.
 *
 * Equivale a `Coffee.Common.DTO.PageResponse`.
 *
 * Ojo: Kotlin serializa `Long` como numero JSON, no como string, asi que
 * `totalElements` es `number` y no `string`. Usar `number` aqui es lo que
 * permite hacer comparaciones y aritmetica sin castear.
 */
export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

/**
 * Equivale a `Coffee.Category.DTO.CategoryResponse`.
 *
 * Los UUID viajan como string, no como objeto: es como los serializa
 * Jackson desde Kotlin.
 */
export interface CategoryResponse {
  id: string
  parentId: string | null
  name: string
  slug: string
  active: boolean
  createdAt: string
  updatedAt: string
}
