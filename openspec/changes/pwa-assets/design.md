# PWA Assets — Design

## PWA Manifest Icons

Use `favicon.svg` as the primary scalable icon so the home-screen icon
matches the favicon. Keep PNG variants for legacy and maskable purposes:

```ts
manifest: {
  icons: [
    { src: 'favicon.svg',               sizes: 'any',    type: 'image/svg+xml' },
    { src: 'pwa-192x192.png',           sizes: '192x192', type: 'image/png' },
    { src: 'pwa-512x512.png',           sizes: '512x512', type: 'image/png' },
    { src: 'maskable-icon-512x512.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' },
  ],
}
```

`sizes: 'any'` is the correct declaration for SVG — it tells the browser
the icon scales to any required size. Modern Android Chrome and desktop
Chromium will prefer the SVG entry. iOS Safari ignores SVG manifest icons
and falls back to `apple-touch-icon-180x180.png` (already in `includeAssets`).

## Workbox Config Changes

Add `manifest.webmanifest` to the SW exclusion list so it is never precached:

```ts
workbox: {
  navigateFallbackDenylist: [/\/manifest\.webmanifest/],
  // Existing exclusions remain (OAuth endpoints, etc.)
}
```

## Server-Side Cache-Control

In the production Nginx config:

```nginx
location = /manifest.webmanifest {
    add_header Cache-Control "no-store, no-cache, must-revalidate";
}
```

## Why This Works

1. SVG icon with `sizes: 'any'` → browser picks the best icon at every resolution
2. `manifest.webmanifest` is never in the precache → browser always fetches it fresh
3. iOS falls back to `apple-touch-icon-180x180.png` — already handled
4. Maskable icon stays PNG for reliable adaptive-icon support
