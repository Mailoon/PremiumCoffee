export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface CategoryResponse {
  id: string
  parentId: string | null
  name: string
  slug: string
  active: boolean
  createdAt: string
  updatedAt: string
}
