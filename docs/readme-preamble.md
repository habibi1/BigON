<div align="center">

# Sinema

**A TMDB movie explorer for Android** — offline-first, modular, and built to a Clean Architecture
dependency rule that is enforced by tests rather than by convention.

Kotlin · Jetpack Compose · Hilt · Room · Retrofit · Coroutines — minSdk 26

</div>

| Home | Detail | Search | Settings |
| :--: | :----: | :----: | :------: |
| <img src="docs/screenshots/home.png" width="200" alt="Home screen showing a grid of trending films"> | <img src="docs/screenshots/detail.png" width="200" alt="Movie detail with backdrop, cast and trailer button"> | <img src="docs/screenshots/search.png" width="200" alt="Search screen with genre filter chips"> | <img src="docs/screenshots/settings_dark.png" width="200" alt="Settings in dark theme"> |

<sub>Light and dark are the same components reading different tokens — no component knows which theme it is in
([Home in dark](docs/screenshots/home_dark.png)).</sub>

## What it does

- **Browse** trending, popular, now playing, top rated and upcoming — each list paginated with infinite scroll.
- **Search** with a 300 ms debounce and genre filtering, cancelling stale requests so late responses cannot overwrite newer ones.
- **Open a detail** through a shared-element transition that carries the poster from the grid into the header.
- **Favourite** titles as self-contained snapshots that survive clearing the cache and going offline.
- **Work offline.** Room is the single source of truth; a failed refresh never blanks content that is already cached.

## How it's built

| | |
| --- | --- |
| **Layering is enforced, not documented** | Konsist rules fail the build if a JVM module imports `android.*`, if `Context` escapes the adapter modules, or if `androidx.navigation` appears outside the app shell. |
| **Business logic cannot see the framework** | Seven modules are plain Kotlin/JVM. They *cannot* import Android, so platform types cannot leak into business rules — and their tests run in milliseconds without a device. |
| **Errors are values** | No exception crosses a layer boundary. Every call returns `AppResult`, so callers handle failure exhaustively and the compiler proves it. |
| **Navigation has one source of truth** | Type-safe `@Serializable` routes; the selected tab is *derived* from the back stack rather than tracked beside it, so the two cannot disagree. |
| **The design system is real** | Tokens, ~16 components and ~92 preview functions expanding to ~160 rendered variants across themes, font scales, devices and RTL. |

65 unit tests, all green.

## Getting started

The app needs a free [TMDB API key](https://www.themoviedb.org/settings/api). Credentials are read from
`local.properties`, which is gitignored — they appear in no source file, build script or committed config.

1. Add your key to `local.properties` in the project root:

   ```properties
   TMDB_API_KEY=your_v3_api_key_here
   ```

   A v4 read access token works too — set `TMDB_READ_ACCESS_TOKEN` instead and it is preferred automatically.

2. Build and install:

   ```bash
   ./gradlew installDebug
   ```

3. Run the checks:

   ```bash
   ./gradlew test
   ```

> **A note on the key.** It is compiled into the APK and recoverable by unpacking the dex — obscurity, not
> security. That is acceptable for a free, read-only, rotatable token on a portfolio app; a commercial product
> would proxy TMDB behind its own backend so the client never holds a credential.

## Editing this file

Everything below the rule is the technical solution: architecture, every stack decision and its rejected
alternatives, and the current state of implementation. It is **generated** from
[`docs/technical-solution.html`](docs/technical-solution.html) — edit that file, not this one, then run:

```bash
python3 docs/tools/artifact_to_markdown.py \
    docs/technical-solution.html README.md --preamble docs/readme-preamble.md
```

The landing section above the rule comes from [`docs/readme-preamble.md`](docs/readme-preamble.md).
