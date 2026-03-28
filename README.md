# Canvas Launcher (MVP)

`Canvas Launcher` is an Android Home Launcher MVP that replaces fixed home-screen pages with an infinite 2D canvas.

## What MVP does

- Registers as a launcher (`HOME` + `DEFAULT`).
- Adds app icon entry (`MAIN` + `LAUNCHER`) that opens onboarding/default-setup flow.
- Onboarding screen:
  - animated intro,
  - button to request/set default home app,
  - entry point to Settings screen.
- On startup:
  - reads launchable apps from `PackageManager`,
  - reconciles them with `Room`,
  - places missing apps by pluggable initial layout strategy (`SpiralLayoutStrategy`).
- Infinite canvas interactions:
  - one-finger pan,
  - two-finger pinch-to-zoom,
  - constrained zoom (`MIN_SCALE = 0.3`, `MAX_SCALE = 2.0`).
- App icon interactions:
  - tap to launch app,
  - long press + drag to move icon in world-space coordinates,
  - persist position in DB only on drag end.
  - multi-touch/pan arbitration suppresses accidental app taps during transform.
- Runtime package updates:
  - `PACKAGE_ADDED`, `PACKAGE_REMOVED`, `PACKAGE_CHANGED` via `BroadcastReceiver`,
  - added app appears at current viewport center,
  - removed app disappears,
  - changed app refreshes label and icon cache.
- Performance basics:
  - viewport culling of non-visible items,
  - in-memory LRU bitmap icon cache,
  - no `PackageManager` icon fetches in pan/zoom loop,
  - orientation dot background and zoom-triggered minimap overlay.

## Modular architecture

### Modules

- `:app`
  - Android entry point, `Application`, `MainActivity`, `DefaultActivity`, `SettingsActivity`, package receiver, launcher manifest declarations.
- `:core:common`
  - shared primitives (`AppResult`, `DispatchersProvider`).
- `:core:model`
  - immutable models for apps/canvas/camera.
- `:core:ui`
  - shared Compose theme.
- `:core:settings`
  - DataStore-backed theme preferences repository + DI bindings.
- `:core:database`
  - Room `Entity/Dao/Database`, DB DI module and migration registry.
- `:core:packages`
  - PackageManager data source, app launch service, icon LRU cache, package event bus.
- `:core:performance`
  - pure world/screen transforms and viewport culling engine.
- `:domain`
  - repository contracts, initial layout strategy abstraction, use cases.
- `:feature:apps`
  - `CanvasAppsStore` implementation over Room + DI bindings.
- `:feature:canvas`
  - canvas rendering UI and independent controllers for viewport/gesture/drag-drop.
- `:feature:launcher`
  - launcher screen, `LauncherViewModel`, UDF state orchestration and DI for presentation controllers.

### Layering

- `presentation`: `feature:*`, `core:ui`.
- `domain`: `:domain`.
- `data`: `feature:apps`, `core:database`, `core:packages`.
- `UI` never talks directly to `Room` or `PackageManager`.

## Project tree (high-level)

```text
CanvasLauncher/
  app/
  core/
    common/
    model/
    ui/
    settings/
    database/
    packages/
    performance/
  domain/
  feature/
    apps/
    canvas/
    launcher/
```

## Key technical decisions

- Compose + low-level gesture handling in a single canvas surface for MVP speed and architecture cleanliness.
- Camera state (`worldCenter`, `scale`, viewport size) is isolated from object model.
- World-space persistence for icons (`x`, `y` in DB) with deterministic transforms:
  - `screen = (world - center) * scale + viewportCenter`
  - `world = (screen - viewportCenter) / scale + center`
- Culling is computed in world-space with an extra buffer to reduce pop-in.
- Icon cache:
  - `Drawable -> Bitmap` normalized to `144x144`,
  - stored in memory LRU (`~24MB` default),
  - preloaded after sync and invalidated on package changes.
- Sync logic is implemented as use case (`SyncAppsWithSystemUseCase`) and reused for initial reconciliation.

## Database

- Table: `apps_table`
- Columns:
  - `packageName` (`PRIMARY KEY`)
  - `label` (`TEXT`)
  - `x` (`REAL`)
  - `y` (`REAL`)
- Future migrations:
  - `DatabaseMigrations.ALL` is the dedicated extension point for explicit migrations when schema version increases.

## Security and reliability

- No unnecessary dangerous permissions.
- Package visibility handled via `<queries>` for launcher-intent resolution.
- `QUERY_ALL_PACKAGES` is intentionally not used in this MVP.
- Launch failures and missing intents are handled with safe `AppResult` failures.
- Receiver does minimal work and only publishes lightweight events to in-app bus.
- Null/race-safe handling for package add/remove/change flows.

## Build & run

## Requirements

- Android Studio (recent stable)
- Android SDK 26+
- JDK 17

## Commands

```bash
./gradlew :app:assembleDebug
./gradlew :domain:test :core:performance:test
```

Install and set `Canvas Launcher` as default Home app on device/emulator.

## Testing

Unit tests are included for:

- `SpiralLayoutStrategy`
- world/screen coordinate transforms
- viewport culling
- sync/reconciliation logic
- theme mode use cases + preferences mapping
- tap/gesture arbitration around app icons

Main test files:

- `domain/src/test/.../SpiralLayoutStrategyTest.kt`
- `core/performance/src/test/.../WorldScreenTransformerTest.kt`
- `core/performance/src/test/.../ViewportCullerTest.kt`
- `domain/src/test/.../SyncAppsWithSystemUseCaseTest.kt`

## Performance notes

### Potential bottlenecks

- icon decode/preload latency at cold start,
- excessive allocations if visible-app list churns too often,
- DB writes if drag-save frequency increases.

### How to validate FPS/Jank

- Use `Profile GPU Rendering` and `Macrobenchmark/JankStats` in v2.
- Stress scenario: 200+ apps, rapid pan/zoom, repeated drag interactions.
- Ensure culling keeps composed/drawn items near visible bounds only.

### Planned v2 optimizations

- staged icon prefetch by proximity to viewport,
- persistent disk icon cache,
- stronger render-layer batching for large datasets,
- optional velocity-based camera fling and kinetic panning.

## MVP limitations (intentional)

- No widgets
- No folders
- No icon packs
- No app drawer screen
- No advanced wallpaper controls
- No cloud sync/search

## Roadmap (v2+)

- extended settings for canvas tuning (grid density, minimap threshold)
- backup/restore of icon coordinates
- app grouping primitives
- richer animation system with strict frame-budget controls
- benchmark + baseline profile integration
