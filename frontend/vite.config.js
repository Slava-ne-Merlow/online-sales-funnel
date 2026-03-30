import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

const allowedTunnelHosts = ['ixw52f-81-90-29-147.ru.tuna.am']

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    host: '0.0.0.0',
    port: 5173,
    strictPort: true,
    allowedHosts: allowedTunnelHosts,
    cors: {
      origin: [
        'http://127.0.0.1:5173',
        'http://localhost:5173',
        /^https:\/\/.*\.ru\.tuna\.am$/,
      ],
      credentials: true,
    },
  },
})
