import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  // GitHub Pages project sites are served from https://<user>.github.io/<repo>/ -
  // set BASE_PATH=/<repo>/ when building for that (the deploy workflow does this for you).
  // Left as '/' for local dev and for user/org root pages or a custom domain.
  base: process.env.BASE_PATH || '/',
  // sockjs-client (used by useRoomChannel.js for the online-room WebSocket
  // push) still references Node's `global`, which - unlike Webpack - Vite
  // doesn't polyfill for the browser. Without this the whole app fails to
  // boot with "ReferenceError: global is not defined" the moment that
  // module loads, not just the WebSocket feature.
  define: {
    global: 'globalThis'
  },
  server: {
    port: 5173
  }
})
