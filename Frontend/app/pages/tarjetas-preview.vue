<script setup lang="ts">
// ==========================================================================
// Vista previa de tarjetas de catalogo con los degradados del tema.
// SE BORRA cuando se apruebe.
//
// Los colores salen de `usePalette()`, que los lee de las variables que
// main.css ya tiene escritas. Aqui no hay ni un hex duplicado: si el tema
// cambia, esta pagina cambia con el.
//
// Las combinaciones se declaran como NOMBRES DE TOKEN, no como valores. Si un
// token desaparece del tema, la tarjeta sale sin pintar en vez de mostrar un
// color viejo que ya no existe — que es la diferencia entre un aviso y un
// fallo silencioso.
//
// RESTRICCION QUE ESTA PAGINA ENSEÑO: un chip es un pastel con texto oscuro
// encima, y solo los cinco tonos claros lo aguantan. Los dos marrones no, y
// por eso hay un bloque entero en main.css que lo dice en mayusculas.
// ==========================================================================

const { palette, ready, readToken } = usePalette();

// El degradado del hero sale del propio tema, no se reescribe aqui, y se lee
// PARA CADA MODO: si se leyera una sola vez del <html>, las dos secciones
// saldrian con el mismo degradado y la comparacion no serviria de nada.
const HERO_DARK = computed(() => readToken("--gradient-hero", "dark"));
const HERO_LIGHT = computed(() => readToken("--gradient-hero", "light"));

type Variant = { label: string; tone: string };
type Product = {
  name: string;
  origin: string;
  notes: string;
  price: string;
  /** Tono de la miniatura: degradado de relleno, sin texto. */
  tone: string;
  variants: Variant[];
};

const products: Product[] = [
  {
    name: "Etiopia Yirgacheffe",
    origin: "Gedeo, Etiopia · Lavado · 2100 m",
    notes: "Jazmin, bergamota y un final largo de miel.",
    price: "12,40",
    tone: "sky",
    variants: [
      { label: "Lavado", tone: "sky" },
      { label: "Medio", tone: "sage" },
    ],
  },
  {
    name: "Colombia Huila",
    origin: "Pitalito, Colombia · Honey · 1750 m",
    notes: "Panela, ciruela roja y cacao.",
    price: "9,80",
    tone: "sand",
    variants: [
      { label: "Honey", tone: "sand" },
      { label: "Natural", tone: "cream" },
    ],
  },
  {
    name: "Brasil Cerrado",
    origin: "Minas Gerais, Brasil · Natural · 1150 m",
    notes: "Avellana, chocolate con leche y crema.",
    price: "8,20",
    tone: "camel",
    variants: [
      { label: "Natural", tone: "sage" },
      { label: "Medio-oscuro", tone: "sand" },
      { label: "500 g", tone: "cream" },
    ],
  },
];
</script>

<template>
  <div class="pg">
    <header class="pg__head">
      <h1>Tarjetas de catalogo</h1>
      <p v-if="ready">
        El degradado del hero es <code>--gradient-hero</code>, leido del tema:
        no hay una copia en esta pagina que pueda quedar desfasada.
      </p>
      <p v-else class="pg__loading">Leyendo la paleta del tema…</p>
    </header>

    <template v-if="ready">
      <!-- ============================ OSCURO ============================ -->
      <section
        class="mock"
        :style="{ background: palette.espresso, color: palette.bone }"
      >
        <p class="tag">Modo oscuro</p>

        <div
          class="hero"
          :style="{
            background: HERO_DARK,
            borderColor: palette.brownDeep,
          }"
        >
          <div>
            <p class="hero__eyebrow" :style="{ color: palette.tan }">
              Cafe de especialidad · Colombia
            </p>
            <h2>Premium Coffee</h2>
            <p class="hero__lead">
              Cada variante se puede ver en 3D antes de pedirla.
            </p>
          </div>
          <button
            type="button"
            class="btn"
            :style="{
              background: palette.tan,
              color: palette.espresso,
              borderColor: palette.tan,
            }"
          >
            Ver el catalogo
          </button>
        </div>

        <div class="cards">
          <articl
            v-for="prod in products"
            :key="prod.name"
            class="card"
            :style="{
              background: palette.espresso,
              borderColor: palette.brownDeep,
            }"
          >
            <section v-if="prod.variants && prod.variants.length > 0">
              <div
                class="thumb"
                :style="{
                  background: `linear-gradient(140deg, ${palette[prod.tone]}, ${palette.brown})`,
                }"
              >
                <span
                  class="chip chip--light"
                  :style="{ color: palette.espresso }"
                >
                  {{ prod.variants[0]?.label }}
                </span>
              </div>

              <div class="card__body">
                <h3>{{ prod.name }}</h3>
                <p class="orig" :style="{ color: palette.cream }">
                  {{ prod.origin }}
                </p>
                <p class="notes" :style="{ color: palette.tan }">
                  {{ prod.notes }}
                </p>

                <div class="chips">
                  <span
                    v-for="v in prod.variants"
                    :key="v.label"
                    class="chip"
                    :style="{
                      background: palette[v.tone],
                      color: palette.espresso,
                    }"
                  >
                    {{ v.label }}
                  </span>
                </div>

                <div class="row" :style="{ borderColor: palette.brownDeep }">
                  <span class="price">{{ prod.price }} EUR</span>
                  <button
                    type="button"
                    class="btn btn--sm"
                    :style="{
                      background: palette.tan,
                      color: palette.espresso,
                      borderColor: palette.tan,
                    }"
                  >
                    Anadir
                  </button>
                </div>
              </div>
            </section>
          </articl>
        </div>
      </section>

      <!-- ============================ CLARO ============================ -->
      <section
        class="mock"
        :style="{ background: palette.bone, color: palette.espresso }"
      >
        <p class="tag">Modo claro</p>

        <div
          class="hero"
          :style="{ background: HERO_LIGHT, borderColor: palette.cream }"
        >
          <div>
            <p class="hero__eyebrow" :style="{ color: palette.brown }">
              Cafe de especialidad · Colombia
            </p>
            <h2>Premium Coffee</h2>
            <p class="hero__lead">
              Cada variante se puede ver en 3D antes de pedirla.
            </p>
          </div>
          <button
            type="button"
            class="btn"
            :style="{
              background: palette.brown,
              color: palette.bone,
              borderColor: palette.brown,
            }"
          >
            Ver el catalogo
          </button>
        </div>

        <div class="cards">
          <article
            v-for="prod in products"
            :key="prod.name"
            class="card"
            :style="{ background: palette.white, borderColor: palette.cream }"
          >
            <div
              class="thumb"
              :style="{
                background: `linear-gradient(140deg, ${palette[prod.tone]}, ${palette.sand})`,
              }"
            >
              <span
                class="chip chip--light"
                :style="{ color: palette.espresso }"
              >
                {{ prod.variants[0]?.label }}
              </span>
            </div>

            <div class="card__body">
              <h3>{{ prod.name }}</h3>
              <p class="orig" :style="{ color: palette.brown }">
                {{ prod.origin }}
              </p>
              <p class="notes">{{ prod.notes }}</p>

              <div class="chips">
                <span
                  v-for="v in prod.variants"
                  :key="v.label"
                  class="chip"
                  :style="{
                    background: palette[v.tone],
                    color: palette.espresso,
                  }"
                >
                  {{ v.label }}
                </span>
              </div>

              <div class="row" :style="{ borderColor: palette.cream }">
                <span class="price">{{ prod.price }} EUR</span>
                <button
                  type="button"
                  class="btn btn--sm"
                  :style="{
                    background: palette.brown,
                    color: palette.bone,
                    borderColor: palette.brown,
                  }"
                >
                  Anadir
                </button>
              </div>
            </div>
          </article>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.pg {
  padding: 2rem 0 4rem;
  font-family: var(--font-sans);
}
.pg__head h1 {
  margin: 0 0 0.4rem;
  font-size: 1.6rem;
  color: var(--color-bone);
}
.pg__head p {
  margin: 0;
  color: var(--color-camel);
  font-size: 0.9rem;
}
.pg__loading {
  font-style: italic;
}
code {
  background: var(--color-espresso);
  padding: 0.1rem 0.35rem;
  border-radius: 4px;
}
.mock {
  margin-top: 2rem;
  border-radius: 16px;
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}
.tag {
  margin: 0;
  font-size: 0.72rem;
  text-transform: uppercase;
  letter-spacing: 0.14em;
  opacity: 0.6;
}
.hero {
  border: 1px solid;
  border-radius: 14px;
  padding: 1.75rem 2rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1.5rem;
  flex-wrap: wrap;
}
.hero__eyebrow {
  margin: 0 0 0.4rem;
  font-size: 0.72rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.14em;
}
.hero h2 {
  margin: 0 0 0.4rem;
  font-size: clamp(1.6rem, 4vw, 2.4rem);
  letter-spacing: -0.02em;
}
.hero__lead {
  margin: 0;
  font-size: 0.92rem;
  opacity: 0.85;
}
.cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1rem;
}
.card {
  border: 1px solid;
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.thumb {
  height: 120px;
  position: relative;
}
.thumb .chip {
  position: absolute;
  top: 0.65rem;
  left: 0.65rem;
}
.card__body {
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  flex: 1;
}
.card h3 {
  margin: 0;
  font-size: 1.05rem;
}
.orig {
  margin: 0;
  font-size: 0.76rem;
}
.notes {
  margin: 0;
  font-size: 0.85rem;
  line-height: 1.5;
}
.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
  margin-top: auto;
  padding-top: 0.5rem;
}
.chip {
  padding: 0.2rem 0.55rem;
  border-radius: 999px;
  font-size: 0.7rem;
  font-weight: 700;
}
.chip--light {
  background: rgba(255, 255, 255, 0.82);
}
.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding-top: 0.75rem;
  border-top: 1px solid;
}
.price {
  font-weight: 700;
  font-size: 1rem;
}
.btn {
  border: 1px solid;
  border-radius: 999px;
  padding: 0.6rem 1.3rem;
  font: inherit;
  font-size: 0.85rem;
  font-weight: 700;
  cursor: pointer;
}
.btn--sm {
  padding: 0.45rem 1rem;
  font-size: 0.8rem;
}
</style>
