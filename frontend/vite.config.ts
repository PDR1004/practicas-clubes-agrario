import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// El SPA se sirve bajo /clubes/ detras de Nginx: ver docs/adr/0007 y infra/nginx/default.conf.
export default defineConfig({
  base: '/clubes/',
  plugins: [react()],
  server: {
    port: 5173,
  },
})
