import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { resolve } from 'path'

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
        index: resolve(__dirname, 'index.html')
      }
    }
  }
})  //mistake is that we have 3 public folderrs;
