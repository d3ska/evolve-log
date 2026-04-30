# Spec: cache-busting

### Requirement: Updated PWA icons appear on mobile after next SW update

#### Scenario: User has old PWA install
- **WHEN** the app deploys with renamed icon files and updated vite.config.ts
- **THEN** Workbox generates a new SW with updated precache manifest
- **AND** the new SW installs in the background
- **AND** on next app open the new icon is displayed on the home screen

### Requirement: manifest.webmanifest is never served from SW cache

#### Scenario: Browser checks for manifest on install
- **WHEN** a browser requests `/manifest.webmanifest`
- **THEN** the response has `Cache-Control: no-store`
- **AND** the SW does not intercept or cache this request

### Requirement: Icon files are served with long-lived cache (via Workbox precache)

#### Scenario: Icon request after SW install
- **WHEN** the PWA requests `pwa-192-v2.png` after SW installation
- **THEN** the response is served from the SW precache (CacheFirst strategy)
- **AND** the file is not re-fetched from the network on repeat loads
