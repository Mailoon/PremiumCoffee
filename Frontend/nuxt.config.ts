import tailwindcss from '@tailwindcss/vite'

const isProd = process.env.NODE_ENV === 'production'

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
  'worker-src': ["'self'", 'blob:'],
 
  'manifest-src': ["'self'"],
  'connect-src': isProd
    ? ["'self'", 'https://res.cloudinary.com']
    : ["'self'", 'https://res.cloudinary.com', 'ws:', 'wss:'],

  'upgrade-insecure-requests': isProd
}

export default defineNuxtConfig({
  compatibilityDate: '2025-01-01',

  devtools: { enabled: false },
  runtimeConfig: {
    apiBase: process.env.NUXT_API_BASE || 'http://backend:8080',

    public: {
      siteUrl: process.env.NUXT_PUBLIC_SITE_URL || 'http://localhost:3000'
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

  routeRules: {
    '/api/**': {
      security: {
        xssValidator: false
      }
    },

    '/api/auth/login': {
      security: {
        rateLimiter: {
          tokensPerInterval: 5,
          interval: 60,
          headers: true
        }
      }
    },
    '/api/auth/register': {
      security: {
        rateLimiter: { tokensPerInterval: 5, interval: 60, headers: true }
      }
    },
    '/api/auth/forgot-password': {
      security: {
        rateLimiter: { tokensPerInterval: 5, interval: 60, headers: true }
      }
    },
    '/api/auth/reset-password': {
      security: {
        rateLimiter: { tokensPerInterval: 5, interval: 60, headers: true }
      }
    },
    '/api/auth/**': {
      security: {
        requestSizeLimiter: {
          maxRequestSizeInBytes: 16384,
          maxUploadFileRequestInBytes: 16384
        }
      }
    },

    '/api/media-assets/upload': {
      security: {
        requestSizeLimiter: {
          maxRequestSizeInBytes: 12 * 1024 * 1024,
          maxUploadFileRequestInBytes: 12 * 1024 * 1024
        }
      }
    }
  },

  modules: ['nuxt-security', './modules/vue-compiler-plugins-cleanup'],
  vite: {
    plugins: [tailwindcss()]
  },

  css: ['~/assets/css/main.css'],

  security: {
    nonce: true,
    sri: false,
    rateLimiter: false,
    requestSizeLimiter: false,
    corsHandler: false,
    csrf: {
      cookieKey: 'csrf',

      cookie: {
        path: '/',
        httpOnly: true,
        sameSite: 'lax',
        secure: false
      },
      headerName: 'csrf-token',
      methodsToProtect: ['POST', 'PUT', 'PATCH', 'DELETE']
    },

    headers: {
      contentSecurityPolicy,
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
    nodeTsConfig: {
      compilerOptions: {
        types: ['node']
      }
    },
    tsConfig: {
      vueCompilerOptions: {
        plugins: []
      }
    }
  }
})
function defineNuxtConfig(arg0: {
  compatibilityDate: string; devtools: { enabled: boolean }; runtimeConfig: { apiBase: any; public: { siteUrl: any } }
  // Todo lo de app/ (pages, components, layouts...) va aqui dentro.
  srcDir: string; app: { head: { htmlAttrs: { lang: string }; title: string; meta: ({ charset: string } | { name: string; content: string })[] } }; nitro: { compressPublicAssets: boolean }; routeRules: { '/api/**': { security: { xssValidator: boolean } }; '/api/auth/login': { security: { rateLimiter: { tokensPerInterval: number; interval: number; headers: boolean } } }; '/api/auth/register': { security: { rateLimiter: { tokensPerInterval: number; interval: number; headers: boolean } } }; '/api/auth/forgot-password': { security: { rateLimiter: { tokensPerInterval: number; interval: number; headers: boolean } } }; '/api/auth/reset-password': { security: { rateLimiter: { tokensPerInterval: number; interval: number; headers: boolean } } }; '/api/auth/**': { security: { requestSizeLimiter: { maxRequestSizeInBytes: number; maxUploadFileRequestInBytes: number } } }; '/api/media-assets/upload': { security: { requestSizeLimiter: { maxRequestSizeInBytes: number; maxUploadFileRequestInBytes: number } } } }; modules: string[]; vite: { plugins: any[] }; css: string[]; security: { nonce: boolean; sri: boolean; rateLimiter: boolean; requestSizeLimiter: boolean; corsHandler: boolean; csrf: { cookieKey: string; cookie: { path: string; httpOnly: boolean; sameSite: string; secure: boolean }; headerName: string; methodsToProtect: string[] }; headers: { contentSecurityPolicy: { 'base-uri': string[]; 'object-src': string[]; 'frame-ancestors': string[]; 'script-src-attr': string[]; 'form-action': string[]; 'script-src': string[]; 'style-src': string[]; 'img-src': string[]; 'media-src': string[]; 'font-src': string[]; 'worker-src': string[]; 'manifest-src': string[]; 'connect-src': string[]; 'upgrade-insecure-requests': boolean }; strictTransportSecurity: boolean | { maxAge: number; includeSubdomains: boolean; preload: boolean }; xFrameOptions: string; referrerPolicy: string; crossOriginResourcePolicy: string; crossOriginEmbedderPolicy: boolean; crossOriginOpenerPolicy: string } }; typescript: { nodeTsConfig: { compilerOptions: { types: string[] } }; tsConfig: { vueCompilerOptions: { plugins: never[] } } }
}) {
  function useRuntimeConfig() {
  throw new Error('Function not implemented.')
}
}

