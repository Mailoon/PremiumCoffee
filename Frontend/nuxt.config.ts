// ==========================================================================
// PremiumCoffe — configuracion de Nuxt 4
// ==========================================================================

export default defineNuxtConfig({
  compatibilityDate: '2025-01-01',

  devtools: { enabled: false },

  // --------------------------------------------------------------------------
  // runtimeConfig — lo que se lee en TIEMPO DE EJECUCION, no al compilar
  //
  // Esta es la diferencia clave frente a un simple process.env horneado en el
  // bundle. Gracias a esto, la MISMA imagen de Docker sirve para dev, staging y
  // produccion: solo cambian las variables de entorno del contenedor.
  //
  //   "private" -> NUNCA viaja al navegador. Solo la usa el servidor.
  //                 Aqui va la URL interna del backend Kotlin.
  //   "public"  -> SI viaja al navegador. Solo URL publicas, nunca secretos.
  // --------------------------------------------------------------------------
  runtimeConfig: {
    // URL interna del backend, vista SOLO desde el servidor de Nuxt.
    // Por defecto asume que corre en la red de Docker Compose.
    apiBase: 'http://backend:8080',

    public: {
      // Lo ve el navegador. Sirve para SEO / URLs absolutas.
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

function defineNuxtConfig(arg0: {
  compatibilityDate: string; devtools: { enabled: boolean; };
  // --------------------------------------------------------------------------
  // runtimeConfig — lo que se lee en TIEMPO DE EJECUCION, no al compilar
  //
  // Esta es la diferencia clave frente a un simple process.env horneado en el
  // bundle. Gracias a esto, la MISMA imagen de Docker sirve para dev, staging y
  // produccion: solo cambian las variables de entorno del contenedor.
  //
  //   "private" -> NUNCA viaja al navegador. Solo la usa el servidor.
  //                 Aqui va la URL interna del backend Kotlin.
  //   "public"  -> SI viaja al navegador. Solo URL publicas, nunca secretos.
  // --------------------------------------------------------------------------
  runtimeConfig: {
    // URL interna del backend, vista SOLO desde el servidor de Nuxt.
    // Por defecto asume que corre en la red de Docker Compose.
    apiBase: string; public: {
      // Lo ve el navegador. Sirve para SEO / URLs absolutas.
      siteUrl: string;
    };
  };
  // Todo lo de app/ (pages, components, layouts...) va aqui dentro.
  srcDir: string; app: { head: { htmlAttrs: { lang: string; }; title: string; meta: ({ charset: string; } | { name: string; content: string; })[]; }; }; nitro: { compressPublicAssets: boolean; };
}) {
  throw new Error("Function not implemented.");
}
