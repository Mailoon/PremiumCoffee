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
