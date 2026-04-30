# PWA Assets — Tasks

- [ ] **T1** Add `{ src: 'favicon.svg', sizes: 'any', type: 'image/svg+xml' }` as first entry
  in the `manifest.icons` array in `vite.config.ts`

- [ ] **T2** Add `manifest.webmanifest` to Workbox `navigateFallbackDenylist` in `vite.config.ts`

- [ ] **T3** Add `Cache-Control: no-store` header for `/manifest.webmanifest` in the Nginx
  production config (`docker-compose.prod.yml` or dedicated nginx.conf)

- [ ] **T4** Verify: build the app, inspect `dist/sw.js` precache manifest — confirm `favicon.svg`
  appears in the manifest icons and `manifest.webmanifest` is not listed in the precache

- [ ] **T5** Verify on a real Android device: install/update PWA and confirm the SVG icon
  appears on the home screen
