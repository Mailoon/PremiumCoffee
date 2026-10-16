import { defineNuxtModule } from '@nuxt/kit'

export default defineNuxtModule({
  meta: {
    name: 'premiumcoffee:vue-compiler-plugins-cleanup'
  },
  setup(_options, nuxt) {

    nuxt.hook('modules:done', () => {
      nuxt.hook('prepare:types', ({ tsConfig }) => {
        tsConfig.vueCompilerOptions ||= {}
        tsConfig.vueCompilerOptions.plugins = []
      })
    })
  }
})
