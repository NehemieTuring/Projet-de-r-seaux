import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
  ],
  server: {
    host: '0.0.0.0', // écoute sur toutes les interfaces réseau
    port: 5173,       // ou un autre port que tu choisis
    strictPort: true, // (optionnel) empêche de choisir un autre port si celui-ci est occupé
  }
})
