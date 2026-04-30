# PWA Assets — Proposal

## Why

After a favicon update, mobile PWA installs continue showing the old purple icon. The root cause
is that Workbox precaches icon files by URL — if the filename stays the same, the URL hash
doesn't change, the Service Worker doesn't update, and the old icon persists indefinitely.

A secondary issue: `manifest.webmanifest` may be cached by the browser or SW itself, preventing
new icon references from being picked up even after a SW update.

## What Changes

1. **Rename icon files** with a version suffix so Workbox sees new URLs and evicts old cache entries
2. **Add `Cache-Control: no-store` handling** for `manifest.webmanifest` so the browser always
   fetches it fresh on install/update
3. **Exclude `manifest.webmanifest` from SW precache** — it should be network-fetched, not cached

## Impact

- `vite.config.ts`: updated icon filenames in PWA manifest config
- Actual icon files renamed in `public/`
- Nginx/server config (or Vite dev server config): `Cache-Control: no-store` for manifest
- No backend changes
