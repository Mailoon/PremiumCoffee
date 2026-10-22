<script setup lang="ts">
// ==========================================================================
// Contenedor raiz. Aporta el cromo comun (marca, nav, conmutador de tema).
// El color, el espacio y el tipo salen todos de los tokens de
// assets/css/main.css: aqui no hay ni un literal.
// ==========================================================================

const { isDark, toggle } = useTheme()

// El atributo va en el <html> y NO en un div. Eso es lo que hace que
// `dark:` responda al tema y lo que evita el parpadeo: como la preferencia
// llega por cookie, useHead la resuelve en el servidor y el HTML ya sale
// con la clase correcta.
//
// Ojo: `htmlAttrs.class` administra el atributo entero. Si algun dia se anade
// otra clase al <html> desde fuera, hay que acumularla aqui o se perdera.
useHead({
  htmlAttrs: {
    class: computed(() => (isDark.value ? 'dark' : 'light'))
  }
})
</script>

<template>
  <div class="min-h-dvh bg-bg text-text">
    <header class="shell__header border-b border-border">
      <NuxtLink to="/" class="text-lg font-bold text-text no-underline">
        Premium Coffee
      </NuxtLink>

      <nav class="flex items-center gap-5">
        <NuxtLink
          to="/catalogo"
          class="text-muted no-underline transition-colors hover:text-accent"
        >
          Catalogo
        </NuxtLink>

        <!-- aria-pressed en vez de aria-label: un boton que conmuta necesita
             decir su estado actual, no solo que existe. Un lector de pantalla
             anuncia "alternado" y con eso ya sabe si esta en claro o oscuro. -->
        <button
          type="button"
          class="rounded-pill border border-border px-3 py-1.5 text-sm font-semibold
                 text-muted transition-colors
                 hover:border-accent hover:text-accent
                 focus-visible:outline-2 focus-visible:outline-offset-2
                 focus-visible:outline-accent"
          :aria-pressed="isDark"
          :aria-label="isDark ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro'"
          @click="toggle"
        >
          {{ isDark ? 'Claro' : 'Oscuro' }}
        </button>
      </nav>
    </header>

    <main class="mx-auto w-full max-w-[1100px] px-4 py-8 sm:px-8">
      <NuxtPage />
    </main>
  </div>
</template>
