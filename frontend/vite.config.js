import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  // GitHub Pages project sites are served from https://<user>.github.io/<repo>/ -
  // set BASE_PATH=/<repo>/ when building for that (the deploy workflow does this for you).
  // Left as '/' for local dev and for user/org root pages or a custom domain.
  base: process.env.BASE_PATH || '/',
  server: {
    port: 5173
  }
})
