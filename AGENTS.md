# Repository Guidelines

## Project Structure & Module Organization
Core application code lives under `src/main/cljs_karaoke`. The main entry point is `src/main/cljs_karaoke/app.cljs`, with feature code split into `events/`, `subs/`, `views/`, `components/`, `router/`, and `remote_control/`. Development-only build hooks and helper namespaces live in `src/dev`. Static assets, song data, Sass, and generated SEO templates live in `resources/`. The compiled site is emitted to `public/`, which is generated output and ignored by Git.

## Build, Test, and Development Commands
Install dependencies with `npm install`. Start local development with `npm run cljs-watch`; Shadow CLJS serves the app at `http://localhost:8089` and watches `src/` plus `resources/`. Build styles once with `npm run css-build`, or keep Sass recompiling with `npm run css-watch`. Produce the optimized production bundle with `npm run release` or `shadow-cljs release app`; this is the same release step used in CI. `npm test` is currently a placeholder and intentionally fails, so use release builds and manual smoke checks instead.

## Coding Style & Naming Conventions
Follow existing Clojure(Script) style: two-space indentation, aligned `:require` blocks, and kebab-case for namespaces, vars, and keywords. Keep namespaces focused and grouped by concern, for example `cljs-karaoke.events.audio` or `cljs-karaoke.views.playback`. Prefer pure helpers where possible and keep `re-frame` registrations near related feature code. In Sass, keep selectors nested conservatively and reuse the shared variables in `resources/sass/main.scss`.

## Testing Guidelines
`shadow-cljs.edn` includes `src/test` on the classpath, but no automated tests are checked in yet. When adding tests, place them under `src/test` and mirror the production namespace name with a `-test` suffix. Until a runner is added, verify changes by loading the app locally, exercising the affected flow, and confirming `shadow-cljs release app` completes without errors.

## Commit & Pull Request Guidelines
Recent history favors short, imperative commit messages such as `add song stats to playback view` or `change cheatsheet shortcut`. Keep commits narrow and descriptive. For pull requests, include a concise summary, note any user-visible behavior changes, link related issues when applicable, and attach screenshots or short recordings for UI changes. Ensure the release build passes before requesting review.
