<script setup lang="ts">
// ==========================================================================
// Comparador de degradados. SE CONSERVA.
//
// Los colores salen del tema por `usePalette()`, que los lee de las variables
// que main.css ya tiene escritas. Aqui no hay ningun hex: si el tema cambia,
// esta pagina cambia con el y no puede desincronizarse.
//
// Cada tarjeta mide DOS contrastes, y son dos medidas distintas:
//
//   - El grande es el contraste donde SE APOYA EL TEXTO.
//   - El pequeno (`todo: X`) es el que daria un analisis automatico si el
//     texto se repartiera por todo el recorrido del degradado.
//
// Solo aparece el pequeno cuando difieren del grande. Ponerlo siempre seria
// ruido: dos numeros repetidos hacen pensar que hay dos pruebas cuando en
// realidad hay una.
// ==========================================================================

const { palette, ready } = usePalette()

// --------------------------------------------------------------------------
// Utilidades de contraste (WCAG 2.1).
// --------------------------------------------------------------------------
function parseHex(h: string): [number, number, number] {
  const s = h.replace('#', '').trim()
  const full =
    s.length === 3
      ? s
          .split('')
          .map((c) => c + c)
          .join('')
      : s
  return [
    parseInt(full.slice(0, 2), 16),
    parseInt(full.slice(2, 4), 16),
    parseInt(full.slice(4, 6), 16)
  ]
}

function toLinear(channel: number): number {
  const c = channel / 255
  return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4)
}

function luminance(hex: string): number {
  const [r, g, b] = parseHex(hex)
  return 0.2126 * toLinear(r) + 0.7152 * toLinear(g) + 0.0722 * toLinear(b)
}

function contrast(a: string, b: string): number {
  if (!a || !b) return 0
  const l1 = luminance(a)
  const l2 = luminance(b)
  return (Math.max(l1, l2) + 0.05) / (Math.min(l1, l2) + 0.05)
}

/** El extremo del degradado contra el que el texto peor se comporta. */
function worstCase(stops: string[], text: string) {
  let value = Infinity
  let at = ''
  for (const stop of stops) {
    const r = contrast(text, stop)
    if (r < value) {
      value = r
      at = stop
    }
  }
  return { value, at }
}

function gradient(stops: string[], positions?: number[], angle = 135) {
  const parts = stops.map((s, i) => (positions ? `${s} ${positions[i]}%` : s))
  return `linear-gradient(${angle}deg, ${parts.join(', ')})`
}

/**
 * Un candidato.
 *
 * `stops` son TODOS los tonos por los que pasa el degradado. `textRegion` son
 * solo aquellos sobre los que se apoya el texto; si no se indica, se considera
 * que el texto se reparte por todo el degradado.
 *
 * La distincion existe por una limitacion matematica de esta paleta: para que
 * `#efefef` siga cumpliendo 4.5:1, el tono mas claro del degradado tiene que
 * tener luminancia <= 0.1529. En la paleta eso deja solo tres: espresso,
 * brown y brown-deep. `camel` esta en 0.3184 y `tan` en 0.6071.
 *
 * O sea: un degradado no puede terminar en tono claro si el texto se apoya en
 * el. Pero si ese tono claro se reserva para la punta final y el texto vive en
 * la parte oscura, si funciona.
 */
type Candidate = {
  id: string
  name: string
  stops: string[]
  text: string
  textRegion?: string[]
  note: string
  shipped?: boolean
}

/** Paradas explicitas, solo para los degradados que no van repartidos. */
const STOP_POSITIONS: Record<string, number[]> = {
  D5: [0, 90, 100],
  D6: [0, 70, 100]
}

// Se declaran como NOMBRES de token a proposito. Si un token desaparece del
// tema, la tarjeta sale sin pintar en vez de mostrar un color viejo, que es la
// diferencia entre un aviso y un fallo silencioso.
const DARK: Candidate[] = [
  {
    id: 'D1',
    name: 'espresso -> brown',
    stops: ['espresso', 'brown'],
    text: 'bone',
    note: 'El clasico de dos paradas.'
  },
  {
    id: 'D2',
    name: 'espresso -> brown-deep',
    stops: ['espresso', 'brownDeep'],
    text: 'bone',
    note: 'Casi indistinguible: brown-deep y brown tienen la misma luminancia.'
  },
  {
    id: 'D3',
    name: 'espresso -> brown -> tan',
    stops: ['espresso', 'brown', 'tan'],
    text: 'bone',
    note: 'Se pasa de lejos: la punta clara se come el texto entero.'
  },
  {
    id: 'D4',
    name: 'espresso -> camel',
    stops: ['espresso', 'camel'],
    text: 'bone',
    note: 'Bonita punta clara, pero el texto no cabe ni en el primer tramo.'
  },
  {
    // ---- Elegido para el modo oscuro -------------------------------------
    id: 'D5',
    name: 'espresso 0-90% -> brown -> camel 100%',
    stops: ['espresso', 'brown', 'camel'],
    text: 'bone',
    textRegion: ['espresso', 'brown'],
    note: 'El camel entra en el 10% final; el texto se apoya en espresso y brown.',
    shipped: true
  },
  {
    id: 'D6',
    name: 'espresso -> brown 70% -> camel 100%',
    stops: ['espresso', 'brown', 'camel'],
    text: 'bone',
    textRegion: ['espresso', 'brown'],
    note: 'Mismo final que D5 pero con la rampa repartida: se ve mas gradual.'
  }
]

const LIGHT: Candidate[] = [
  {
    id: 'L1',
    name: 'bone -> cream',
    stops: ['bone', 'cream'],
    text: 'espresso',
    note: 'El clasico: crema, calido.'
  },
  {
    id: 'L2',
    name: 'bone -> sand',
    stops: ['bone', 'sand'],
    text: 'espresso',
    note: 'Muy parecido a L1; sand y cream casi coinciden.'
  },
  {
    id: 'L3',
    name: 'bone -> sky',
    stops: ['bone', 'sky'],
    text: 'espresso',
    note: 'El mas alto de los claros, y el mas frio.',
    shipped: true
  },
  {
    id: 'L4',
    name: 'bone -> rose',
    stops: ['bone', 'rose'],
    text: 'espresso',
    note: 'Contraste alto, pero mas pastel que el resto.'
  },
  {
    id: 'L5',
    name: 'bone -> sage',
    stops: ['bone', 'sage'],
    text: 'espresso',
    note: 'El mas bajo de los claros.'
  }
]

/** Resuelve nombres de token a valores reales de la paleta del tema. */
function resolve(c: Candidate) {
  const stops = c.stops.map((name) => palette[name] ?? '')
  const text = palette[c.text] ?? ''
  const positions = STOP_POSITIONS[c.id]

  const span = worstCase(stops, text)

  const regionStops = c.textRegion
    ? c.textRegion.map((name) => palette[name] ?? '')
    : stops
  const region = worstCase(regionStops, text)

  return {
    ...c,
    stops,
    text,
    positions,
    spanWorst: span.value,
    regionWorst: region.value,
    pass: region.value >= 4.5,
    spanDiffers: span.value !== region.value
  }
}
</script>

<template>
  <div class="pg">
    <header class="pg__head">
      <h1>Degradados candidatos</h1>
      <p v-if="ready">
        El numero grande es el contraste donde se apoya el texto. El pequeno
        (<code>todo:</code>) es el que daria un analisis automatico si el texto
        se repartiera por todo el recorrido.
      </p>
      <p v-else class="pg__loading">Leyendo la paleta del tema…</p>
    </header>

    <template v-if="ready">
      <h2 class="sec">Modo oscuro</h2>
      <div class="grid">
        <article
          v-for="c in DARK.map(resolve)"
          :key="c.id"
          class="sw"
          :class="{ 'sw--live': c.shipped }"
          :style="{ background: gradient(c.stops, c.positions) }"
        >
          <div class="sw__badges">
            <span class="badge" :class="c.pass ? 'badge--ok' : 'badge--ko'">
              {{ c.regionWorst.toFixed(2) }} · {{ c.pass ? 'AA' : 'FALLA' }}
            </span>
            <span
              v-if="c.spanDiffers"
              class="badge badge--warn"
              title="Contraste si el texto se repartiera por todo el degradado"
            >
              todo: {{ c.spanWorst.toFixed(2) }}
            </span>
            <span v-if="c.shipped" class="badge badge--live">EN EL TEMA</span>
          </div>

          <div class="sw__body" :style="{ color: c.text }">
            <h3>Cafe de especialidad</h3>
            <p>Origen, proceso y notas de cata de cada grano, con lotes trazables.</p>
            <button type="button" :style="{ borderColor: c.text }">Ver el catalogo</button>
          </div>

          <p class="sw__meta" :style="{ color: c.text }">{{ c.name }} — {{ c.note }}</p>
        </article>
      </div>

      <h2 class="sec">Modo claro</h2>
      <div class="grid">
        <article
          v-for="c in LIGHT.map(resolve)"
          :key="c.id"
          class="sw"
          :class="{ 'sw--live': c.shipped }"
          :style="{ background: gradient(c.stops, c.positions) }"
        >
          <div class="sw__badges">
            <span class="badge" :class="c.pass ? 'badge--ok' : 'badge--ko'">
              {{ c.regionWorst.toFixed(2) }} · {{ c.pass ? 'AA' : 'FALLA' }}
            </span>
            <span v-if="c.shipped" class="badge badge--live">EN EL TEMA</span>
          </div>

          <div class="sw__body" :style="{ color: c.text }">
            <h3>Cafe de especialidad</h3>
            <p>Origen, proceso y notas de cata de cada grano, con lotes trazables.</p>
            <button type="button" :style="{ borderColor: c.text }">Ver el catalogo</button>
          </div>

          <p class="sw__meta" :style="{ color: c.text }">{{ c.name }} — {{ c.note }}</p>
        </article>
      </div>

      <h2 class="sec">Paleta que hay debajo</h2>
      <ul class="swatches">
        <li v-for="(value, name) in palette" :key="name">
          <span class="swatch" :style="{ background: value }" />
          <code>{{ name }}</code>
          <code>{{ value }}</code>
        </li>
      </ul>
    </template>
  </div>
</template>

<style scoped>
.pg {
  padding: 2rem 0 4rem;
  color: var(--color-bone);
  font-family: var(--font-sans);
}
.pg__head h1 {
  margin: 0 0 0.4rem;
  font-size: 1.6rem;
}
.pg__head p {
  margin: 0;
  max-width: 68ch;
  color: var(--color-camel);
  font-size: 0.9rem;
  line-height: 1.55;
}
.pg__loading {
  font-style: italic;
}
.sec {
  margin: 2.5rem 0 1rem;
  font-size: 0.8rem;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  color: var(--color-camel);
}
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 1rem;
}
.sw {
  border-radius: 12px;
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}
/* Marca visual del que esta vivo en el tema, para no tener que leer el
   texto de la insignia a media pantalla. */
.sw--live {
  outline: 2px solid var(--color-tan);
  outline-offset: 2px;
}
.sw__badges {
  display: flex;
  flex-wrap: wrap;
  gap: 0.3rem;
}
.sw__body {
  flex: 1;
}
.sw__body h3 {
  margin: 0 0 0.4rem;
  font-size: 1.25rem;
  letter-spacing: -0.01em;
}
.sw__body p {
  margin: 0 0 1rem;
  font-size: 0.88rem;
  line-height: 1.5;
}
.sw__body button {
  background: transparent;
  color: inherit;
  border: 1px solid;
  border-radius: 999px;
  padding: 0.45rem 1.1rem;
  font: inherit;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
}
.sw__meta {
  margin: 0;
  font-size: 0.76rem;
  opacity: 0.85;
}
.badge {
  align-self: flex-start;
  font-size: 0.72rem;
  font-weight: 700;
  padding: 0.2rem 0.55rem;
  border-radius: 999px;
  font-family: ui-monospace, monospace;
}
.badge--ok {
  background: var(--color-espresso);
  color: var(--color-bone);
}
.badge--ko {
  background: var(--color-brown-deep);
  color: var(--color-bone);
}
/* Aviso, no veredicto: la medida es real pero no es la que aplica al
   diseno, asi que no se pinta como fallo. */
.badge--warn {
  background: var(--color-camel);
  color: var(--color-espresso);
}
.badge--live {
  background: var(--color-tan);
  color: var(--color-espresso);
}
.swatches {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 0.6rem;
  list-style: none;
  padding: 0;
  margin: 0;
}
.swatches li {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.75rem;
}
.swatch {
  width: 1.4rem;
  height: 1.4rem;
  border-radius: 4px;
  border: 1px solid var(--color-camel);
  flex: none;
}
code {
  background: var(--color-espresso);
  padding: 0.1rem 0.35rem;
  border-radius: 4px;
}
</style>
