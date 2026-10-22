export type Theme = 'light' | 'dark'
const COOKIE_NAME = 'pc_theme'
const DEFAULT_THEME: Theme = 'dark'

export function useTheme() {
  const themeCookie = useCookie<Theme>(COOKIE_NAME, {
    default: () => DEFAULT_THEME,
    sameSite: 'lax',
    maxAge: 60 * 60 * 24 * 365
  })
  const theme = useState<Theme>('theme', () => themeCookie.value ?? DEFAULT_THEME)

  const isDark = computed(() => theme.value === 'dark')

  function set(next: Theme) {
    theme.value = next
    themeCookie.value = next
  }

  function toggle() {
    set(isDark.value ? 'light' : 'dark')
  }

  return { theme, isDark, set, toggle }
}
