import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { resolve } from 'path';

export default defineConfig({
  plugins: [react()],

  base: './',

  publicDir: 'public',

  build: {
    outDir: 'dist',
    emptyOutDir: true,

    rollupOptions: {
      input: {
        dashboard: resolve(__dirname, 'dashboard.html'),
        index: resolve(__dirname, 'index.html'),
        serviceWorker: resolve(
          __dirname,
          'src/background/serviceWorker.ts'
        ),
        naukri: resolve(
          __dirname,
          'src/contentScripts/naukri.ts'
        ),
        wellfound: resolve(
         __dirname,
          'src/contentScripts/wellfound.ts'
          ),
          },

      output: {
        entryFileNames: (chunk) => {
          if (chunk.name === 'serviceWorker') {
            return 'serviceWorker.js';
          }

          if (chunk.name === 'naukri') {
            return 'naukri.js';
          }

          if (chunk.name === 'wellfound') {
              return 'wellfound.js';
                }

          return 'assets/[name]-[hash].js';
        },
      },
    },
  },
}); //mistake is that we have 3 public folderrs;
