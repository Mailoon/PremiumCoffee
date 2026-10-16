/** Convierte `brown-deep` en `brownDeep` para poder escribirlo como propiedad. */
function toCamel(name: string): string {
  return name.replace(/-([a-z])/g, (_, c: string) => c.toUpperCase())
}

export function usePalette() {
  // Vacio hasta que se monta. Ver la nota de SSR mas abajo.
  const palette = reactive<Record<string, string>>({})
  const ready = ref(false)
  onMounted(() => {
    const styles = getComputedStyle(document.documentElement)
    for (let i = 0; i < styles.length; i++) {
      const prop = styles[i]
      if (!prop) return 
      if (!prop.startsWith('--color-')) continue
      const key = prop.slice('--color-'.length)
      palette[toCamel(key)] = styles.getPropertyValue(prop).trim()
    }

    ready.value = true
  })

  return { palette, ready, readToken }
}

function readToken(name: string, mode: 'light' | 'dark' = 'light'): string {
  if (import.meta.server) return ''

  const root = document.documentElement
  const estabaEnOscuro = root.classList.contains('dark')

  root.classList.toggle('dark', mode === 'dark')
  const value = getComputedStyle(root).getPropertyValue(name).trim()
  root.classList.toggle('dark', estabaEnOscuro)

  return value
}
