import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    // Forward API calls to Spring Boot so the browser never needs CORS in dev.
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
