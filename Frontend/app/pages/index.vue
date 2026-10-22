<script setup lang="ts">
// ==========================================================================
// Portada (ruta "/").
//
// Esto NO es una pagina de datos: aqui no se consulta la API. Es la pantalla
// de entrada, asi que no pide nada al backend y por eso carga siempre.
//
// Si alguna vez hiciera falta mostrar datos reales aqui, se pedirian con
// useFetch() como en catalogo.vue. No se copia ese patron sin motivo.
// ==========================================================================

useHead({
  title: 'Premium Coffee — Cafe de especialidad',
  meta: [
    {
      name: 'description',
      content:
        'Cafe de especialidad de origen, con catalogo de variantes y visor 3D.'
    }
  ]
})

// --------------------------------------------------------------------------
// Contenido estatico de la portada.
//
// Se declara aqui en vez de escribirlo en el <template> a pelo porque asi el
// texto vive separado del marcado: cuando haya que editar un titular se toca
// una linea de datos, no un bloque de HTML con mil etiquetas encima.
// --------------------------------------------------------------------------
const highlights = [
  {
    title: 'Cafe de especialidad',
    text: 'Origen, proceso y notas de cata de cada grano, con lotes trazables.'
  },
  {
    title: 'Variantes a medida',
    text: 'Tamanos, molienda y formato: pedidos distintos sobre el mismo producto.'
  },
  {
    title: 'Visor 3D',
    text: 'El GLB lo carga Three.js en el navegador. El servidor solo envia URLs.'
  },
  {
    title: 'Builder de componentes',
    text: 'Vaso, liquido, crema y toppings activables pieza a pieza.'
  }
]
</script>

<template>
  <section class="home">
    <!-- ---------------------------------------------------------- hero -->
    <div class="hero">
      <p class="hero__eyebrow">Cafe de especialidad · Colombia</p>
      <h1 class="hero__title">Premium Coffee</h1>
      <p class="hero__lead">
        Un catalogo de cafe de especialidad donde cada variante se puede ver en
        3D antes de pedirla.
      </p>

      <div class="hero__actions">
        <NuxtLink to="/catalogo" class="btn btn--primary">Ver el catalogo</NuxtLink>
        <a href="#como-funciona" class="btn">Como funciona</a>
      </div>
    </div>

    <!-- ----------------------------------------------------- highlights -->
    <div id="como-funciona" class="grid">
      <article
        v-for="item in highlights"
        :key="item.title"
        class="card"
      >
        <h2 class="card__title">{{ item.title }}</h2>
        <p class="card__text">{{ item.text }}</p>
      </article>
    </div>

    <!-- ------------------------------------------------------------ cta -->
    <div class="cta">
      <div>
        <h2 class="cta__title">Todo empieza por el catalogo</h2>
        <p class="cta__text">
          Productos, variantes y precios vienen del backend. Esta portada solo
          te presenta.
        </p>
      </div>
      <NuxtLink to="/catalogo" class="btn btn--primary">Entrar al catalogo</NuxtLink>
    </div>
  </section>
</template>

<style scoped>
.home {
  display: flex;
  flex-direction: column;
  gap: 3.5rem;
}

/* --- hero ---------------------------------------------------------------- */
.hero {
  padding: 3rem 0 1rem;
}

.hero__eyebrow {
  margin: 0 0 0.75rem;
  color: var(--accent);
  font-size: 0.8rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.14em;
}

.hero__title {
  margin: 0 0 0.75rem;
  font-size: clamp(2.5rem, 6vw, 4rem);
  line-height: 1.05;
  letter-spacing: -0.02em;
}

.hero__lead {
  margin: 0 0 2rem;
  max-width: 46ch;
  color: var(--muted);
  font-size: 1.1rem;
  line-height: 1.6;
}

.hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

/* --- botones ------------------------------------------------------------- */
.btn {
  display: inline-flex;
  align-items: center;
  padding: 0.7rem 1.4rem;
  border: 1px solid var(--border);
  border-radius: 999px;
  background: transparent;
  color: var(--text);
  font: inherit;
  font-weight: 600;
  text-decoration: none;
  cursor: pointer;
  transition: border-color 0.15s ease, color 0.15s ease, background 0.15s ease;
}

.btn:hover {
  border-color: var(--accent);
  color: var(--accent);
}

.btn--primary {
  background: var(--accent);
  border-color: var(--accent);
  color: #1a1310;
}

.btn--primary:hover {
  background: #d99b63;
  color: #1a1310;
}

/* --- rejilla ------------------------------------------------------------- */
.grid {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
}

.card {
  border: 1px solid var(--border);
  background: var(--surface);
  border-radius: 10px;
  padding: 1.25rem 1.4rem;
}

.card__title {
  margin: 0 0 0.4rem;
  font-size: 1.02rem;
}

.card__text {
  margin: 0;
  color: var(--muted);
  font-size: 0.93rem;
  line-height: 1.55;
}

/* --- cta ----------------------------------------------------------------- */
.cta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 1.5rem;
  padding: 1.75rem 2rem;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: linear-gradient(135deg, #241b16, var(--surface));
}

.cta__title {
  margin: 0 0 0.35rem;
  font-size: 1.3rem;
}

.cta__text {
  margin: 0;
  max-width: 48ch;
  color: var(--muted);
  font-size: 0.93rem;
}
</style>
