import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

// Configuracion de pruebas separada de vite.config.ts para que el build de produccion
// no dependa de las herramientas de test.
export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    include: ['src/**/*.test.{ts,tsx}'],
  },
})
