# UI/UX Improvement Plan for `cljs-karaoke-client`

## Summary
Focus the first pass on the three highest-leverage areas: first-run onboarding, the home/control workflow, and playlist/playback clarity on mobile. Keep the visual language and existing Bulma/Stylefy approach, but restructure the surfaces so the primary tasks are obvious: enable audio, find a song, start playback, manage playlist, adjust sync.

## Key Changes
- Replace the first-run audio modal in [modals.cljs](/home/victorgil/dev/cljs-karaoke-client/src/main/cljs_karaoke/modals.cljs#L46) with a friendlier onboarding pattern.
  Default: keep it as a modal for now, but change it to a task-specific prompt with a clearer title, shorter copy, and a primary CTA like `Enable audio`.
  Add a secondary dismiss path only if playback/home still remain understandable afterward.
  Acceptance: first-time users understand why interaction is needed and what happens after clicking.

- Rework the home view in [control_panel.cljs](/home/victorgil/dev/cljs-karaoke-client/src/main/cljs_karaoke/views/control_panel.cljs#L134) into a task-first layout.
  Surface a compact "current song" summary and the existing playback controls panel near the top instead of hiding most controls behind the side menu.
  Keep the menu as secondary utilities.
  Put song search and song list directly below the primary controls.
  Acceptance: the home route supports the full main flow without needing the menu.

- Improve song library presentation in [songs.cljs](/home/victorgil/dev/cljs-karaoke-client/src/main/cljs_karaoke/songs.cljs#L20).
  Add a normalization helper for display labels so raw slugs render more cleanly.
  Add a visible search label or placeholder.
  Wrap the table in Bulma `table-container`.
  Add a small result summary such as current range / total count.
  Keep routing behavior unchanged.
  Acceptance: users can scan, search, and launch songs faster on desktop and mobile.

- Redesign playlist rows in [playlist_mode.cljs](/home/victorgil/dev/cljs-karaoke-client/src/main/cljs_karaoke/views/playlist_mode.cljs#L53) for narrow screens.
  Replace the dense action cell with either:
  1. a stacked card row on mobile and table on desktop, or
  2. a simplified single responsive row with labeled actions.
  Default: use a unified stacked card/list item layout for all breakpoints to avoid dual rendering complexity.
  Add accessible labels/titles to every icon-only action.
  Acceptance: playlist actions are understandable and tappable on a phone-sized viewport.

- Improve playback state clarity in [playback.cljs](/home/victorgil/dev/cljs-karaoke-client/src/main/cljs_karaoke/views/playback.cljs#L117) and [app.cljs](/home/victorgil/dev/cljs-karaoke-client/src/main/cljs_karaoke/app.cljs#L97).
  Add explicit state messaging for `loading`, `ready but paused`, `playing with no current lyric frame`, and `cannot play yet`.
  Keep the immersive full-screen stage, but add a compact contextual header or badge with current song name and state.
  De-emphasize the control strip visually so it reads as controls, not the primary content.
  Acceptance: playback never looks broken or empty without explanation.

- Tighten mobile and accessibility behavior in [navbar.cljs](/home/victorgil/dev/cljs-karaoke-client/src/main/cljs_karaoke/views/navbar.cljs#L18), [songs.cljs](/home/victorgil/dev/cljs-karaoke-client/src/main/cljs_karaoke/songs.cljs#L46), and [playlist_mode.cljs](/home/victorgil/dev/cljs-karaoke-client/src/main/cljs_karaoke/views/playlist_mode.cljs#L22).
  Add `aria-label`/`title` to burger and icon-only buttons.
  Ensure the burger exposes `aria-expanded`.
  Label the search input.
  Preserve keyboard access and avoid hidden full-height tap zones being the only seek affordance.
  Acceptance: Lighthouse accessibility should improve from the current failures, especially unnamed buttons.

- Refine supporting styling in [main.scss](/home/victorgil/dev/cljs-karaoke-client/resources/sass/main.scss#L121) and [styles.cljs](/home/victorgil/dev/cljs-karaoke-client/src/main/cljs_karaoke/styles.cljs#L39).
  Reduce dependence on heavy text-shadow for readability.
  Increase contrast consistency for text over image backgrounds.
  Adjust control density, spacing, and vertical rhythm for mobile.
  Keep the playful karaoke visual style.
  Acceptance: text and controls remain readable across background images and narrow screens.

## Public Interface / Behavior Changes
- No route changes.
- No data model or event contract changes required for v1.
- Visible behavior changes:
  home route becomes the main operational dashboard,
  playlist becomes mobile-usable,
  playback exposes clearer state messaging,
  first-run audio setup becomes easier to understand.

## Test Plan
- Manual smoke test on `http://localhost:8089/` desktop width:
  open app, enable audio, search for a song, load it, start playback, pause, stop, return home.
- Manual smoke test on mobile width:
  verify home layout, playlist actions, playback readability, and navbar burger behavior.
- Accessibility checks:
  rerun Lighthouse mobile audit and confirm unnamed-button issues are resolved.
  verify search input and navbar burger expose accessible names.
- Regression checks:
  song routes with and without offset still load correctly.
  playlist jump/move/forget-delay actions still work.
  share link and metadata actions still open the expected modal flows.

## Assumptions / Defaults
- Keep Bulma, Sass, Stylefy, and current Re-frame structure; do not introduce a new UI framework.
- Prioritize clarity and responsiveness over preserving the current exact layout.
- Do not attempt a full visual rebrand in this pass.
- Use the existing `playback-controls-panel` as the base for the home-page primary controls instead of inventing a separate duplicate control surface.
