# PWA Assets — Tasks

- [ ] **T1** Rename icon files in `public/`:
  - `pwa-192.png` → `pwa-192-v2.png`
  - `pwa-512.png` → `pwa-512-v2.png`
  - Any `apple-touch-icon.png` → `apple-touch-icon-v2.png`

- [ ] **T2** Update `vite.config.ts` manifest icon references to new filenames

- [ ] **T3** Add `manifest.webmanifest` to Workbox `navigateFallbackDenylist` in `vite.config.ts`

- [ ] **T4** Add `version: '2'` to the PWA manifest object in `vite.config.ts` to force reinstall
  on existing PWA installs

- [ ] **T5** Add `Cache-Control: no-store` header for `/manifest.webmanifest` in Nginx production config

- [ ] **T6** Verify: build the app, inspect `dist/sw.js` precache manifest — confirm new icon filenames appear and no manifest URL is listed

- [ ] **T7** Verify on a real mobile device: install/update PWA and confirm new icon is shown
