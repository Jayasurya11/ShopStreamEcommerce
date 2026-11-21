import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

// The browser only talks to :5173; Vite forwards every /api call to the API gateway (:8080),
// which routes it to the owning microservice.
export default defineConfig({
  plugins: [react()],
  test: { environment: 'jsdom' },
  server: {
    port: 5173,
    proxy: { '/api': 'http://localhost:8080' },
  },
});
