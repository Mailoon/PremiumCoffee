const isProd = process.env.NODE_ENV === 'production'

// ==========================================================================
// Content-Security-Policy
// ==========================================================================
// Se emite desde aqui, y NO desde Caddy, por una razon concreta: la politica
// usa un nonce distinto en cada peticion. Solo la capa que RENDERIZA el HTML
// puede generar ese nonce e inyectarlo en sus <script>. Si la cabecera la
// pone un proxy que no ve el HTML, el nonce de la cabecera nunca coincide con
// el del HTML y el navegador bloquea todo.
//
// Un dato que hace esto mucho mas barato de lo que parece: Nuxt 4 NO emite el
// payload como JavaScript ejecutable, sino como
// <script type="application/json" id="__NUXT_DATA__">. Ese tag NO lo ejecuta
// el navegador, asi que CSP no lo toca. Y el <script type="importmap"> tampoco
// es JavaScript. En el HTML de este proyecto solo hay UN script inline
// ejecutable: el que inyecta runtimeConfig. El nonce lo resuelve.
//
// Lo que SI queda bloqueado sin nonce es un XSS inyectado en cualquier punto:
// no puede ni crear un <script> ni un onclick=.
// ==========================================================================
const contentSecurityPolicy = {
  'base-uri': ["'none'"],
  'object-src': ["'none'"],
  'frame-ancestors': ["'none'"],
  'script-src-attr': ["'none'"],
  'form-action': ["'self'"],
  'script-src': ["'self'", "'nonce-{{nonce}}'", "'strict-dynamic'"],
  'style-src': ["'self'", "'unsafe-inline'"],
  'img-src': ["'self'", 'data:', 'blob:', 'https://res.cloudinary.com'],
  'media-src': ["'self'", 'blob:', 'https://res.cloudinary.com'],
  'font-src': ["'self'", 'data:'],
  // worker-src 'blob:' es la previsibilidad para Three.js: los loaders de Draco y KTX2
  // corren en Web Workers, y estos se crean con blob: URL.
  'worker-src': ["'self'", 'blob:'],
 
  'manifest-src': ["'self'"],
  'connect-src': isProd
    ? ["'self'", 'https://res.cloudinary.com']
    : ["'self'", 'https://res.cloudinary.com', 'ws:', 'wss:'],

  // ---- Solo en produccion ------------------------------------------------
  // obliga a que cualquier recurso se pida por https
  'upgrade-insecure-requests': isProd
}

export default defineNuxtConfig({
  compatibilityDate: '2025-01-01',

  devtools: { enabled: false },
  runtimeConfig: {
    apiBase: 'http://backend:8080',

    public: {
      siteUrl: 'http://localhost:3000'
    }
  },

  // Todo lo de app/ (pages, components, layouts...) va aqui dentro.
  srcDir: 'app/',

  app: {
    head: {
      htmlAttrs: { lang: 'es' },
      title: 'Premium Coffee',
      meta: [
        { charset: 'utf-8' },
        { name: 'viewport', content: 'width=device-width, initial-scale=1' }
      ]
    }
  },

  nitro: {
    compressPublicAssets: true
  },

  // El orden importa: este modulo se instala despues del nucleo de Nuxt, que
  // es lo que mete el plugin de Volar roto en los tsconfigs. Va el ultimo.
  modules: ['nuxt-security', './modules/vue-compiler-plugins-cleanup'],

  security: {
    // Genera un nonce por peticion. Necesario para que el HTML y la cabecera
    // CSP coincidan.
    nonce: true,
    sri: false,

    headers: {
      contentSecurityPolicy,

      // ---- HSTS ----------------------------------------------------------
      // Dice al navegador: durante max-age segundos, esta URL solo se
      // acepta por https y nunca se baja a http. Sin esto, un atacante que
      // controle la red local (wifi de un cafe) solo tiene que esperar al
      // primer click sin https.
      //
      // En DESARROLLO va desactivado a proposito. Caddy usa `tls internal`,
      // un certificado autofirmado: si el navegador guardase un HSTS de un ano
      // sobre localhost, seguiria exigiendo https al sitio LOCAL mucho tiempo
      // despues de arreglar el certificado, sin forma de quitarlo desde el
      // codigo. Solo el cache del navegador lo limpia.
      //
      // includeSubdomains va en false: si mas adelante se sirve
      // api.tudominio.com, un HSTS con includeSubdomains aqui dejaria ese
      // subdominio con https sin haber emitido su certificado.
      strictTransportSecurity: isProd
        ? { maxAge: 31_552_000, includeSubdomains: false, preload: false }
        : false,
      xFrameOptions: 'DENY',
      referrerPolicy: 'strict-origin-when-cross-origin',
      crossOriginResourcePolicy: 'same-site',
      crossOriginEmbedderPolicy: false,
      crossOriginOpenerPolicy: 'same-origin',
    }
  },

  typescript: {
    // Los tsconfigs que Nuxt genera en .nuxt/ traen `compilerOptions.types: []`,
    // es decir, ningun @types se incluye solo. Sin esto, nuxt.config.ts da
    // error TS2591 en la linea 1 (`process.env`) porque no existe `process`.
    //
    // `nodeTsConfig` es el que se aplica a .nuxt/tsconfig.node.json, que es el
    // unico proyecto que incluye nuxt.config.ts. Los tipos de Node solo hacen
    // falta ahi: en app/ y server/ no se usan y no se arrastran.
    nodeTsConfig: {
      compilerOptions: {
        types: ['node']
      }
    },

    // Nuxt mete "vue-router/volar/sfc-route-blocks" en los vueCompilerOptions
    // de .nuxt/tsconfig.app.json. Ese plugin sirve para los bloques <route> de
    // vue-router, y vue-router 4.6.4 ya no lo exporta en su package.json.
    // Volar intenta resolverlo, falla y se come el error con un try/catch: el
    // typecheck sigue bien, pero en cada ejecucion y en el IDE sale esto, que
    // parece un fallo del proyecto cuando no lo es:
    //
    //   [Vue] Resolve plugin path failed: vue-router/volar/sfc-route-blocks
    //   Error [ERR_PACKAGE_PATH_NOT_EXPORTED]
    //
    // Vaciar la lista lo hace desaparecer. Es seguro: Nuxt declara las rutas
    // con definePageMeta({ path: '...' }), nunca con bloques <route>.
    tsConfig: {
      vueCompilerOptions: {
        plugins: []
      }
    }
  }
})
