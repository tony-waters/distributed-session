import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

// `npm run dev` mirrors what nginx does on port 3000 in docker-compose (see
// nginx/nginx.conf), so the app makes the identical /api/* calls either way. Point it at a
// running `docker compose up` stack; order doesn't matter, Vite matches longest prefix first.
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api/cart': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
});
