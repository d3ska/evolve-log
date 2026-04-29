# PWA Assets — Design

## File Renames

Rename icon files to include a version marker:

```
public/pwa-192.png      → public/pwa-192-v2.png
public/pwa-512.png      → public/pwa-512-v2.png
public/apple-touch-icon.png → public/apple-touch-icon-v2.png  (if present)
```

## vite.config.ts Changes

Update the `manifest` block in the PWA plugin config:

```ts
manifest: {
  icons: [
    { src: 'pwa-192-v2.png', sizes: '192x192', type: 'image/png' },
    { src: 'pwa-512-v2.png', sizes: '512x512', type: 'image/png' },
    { src: 'pwa-512-v2.png', sizes: '512x512', type: 'image/png', purpose: 'any maskable' },
  ],
}
```

## Workbox Config Changes

Add `manifest.webmanifest` to the SW exclusion list so it is never precached:

```ts
workbox: {
  navigateFallbackDenylist: [/\/manifest\.webmanifest/],
  // Existing exclusions remain (AI chat, auth endpoints, etc.)
}
```

## Server-Side Cache-Control

In the production Nginx config (or `docker-compose.prod.yml` Nginx service):

```nginx
location = /manifest.webmanifest {
    add_header Cache-Control "no-store, no-cache, must-revalidate";
}
```

In development (Vite), the manifest is served fresh by the dev server automatically.

## Why This Works

1. New filenames → new URLs → Workbox precache hash changes → SW installs a new version
2. Old SW unregisters; new SW activates and precaches the new icon URLs
3. `manifest.webmanifest` is never in the precache → browser always checks the network for it
4. On the next PWA update prompt (or automatic background update), the new icon is applied

## Note on Existing PWA Installs

Existing installs on mobile will see the updated icon after the next SW update cycle,
which triggers automatically in the background. Users do not need to reinstall the PWA.
Force-clearing can be done by incrementing `version` in the manifest:
```ts
manifest: { version: '2', ... }
```
