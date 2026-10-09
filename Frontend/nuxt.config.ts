// ==========================================================================
// PremiumCoffe — configuracion de Nuxt 4
// ==========================================================================

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
  }
})