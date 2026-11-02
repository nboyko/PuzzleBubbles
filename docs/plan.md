# What's That? — Game Selection

Verified and finalized in Iteration 2, Phase 6. Build, 15 unit tests, 28 affected
emulator tests, lint (zero errors, 18 warnings), and portrait/landscape enlarged-text
rendering passed. Compose previews compile; IDE rendering and physical-device
verification remain unverified. Scope and acceptance criteria are retained below.

## Goal

Home → What's That? Game Selection → selected game → existing gameplay.
This feature introduces selection and navigation only; custom-game creation,
persistence, editing, deletion, covers, and custom gameplay remain out of scope.

## Screen and rows

- Remove the selection screen's header, title, and header Back arrow.
- Reuse the fixed Home/Library bottom navigation with Home selected. Home returns
  to Home; Library opens My Library and removes selection from its back stack.
- Place a filled circular plus button at the bottom right above navigation, as in
  the supplied reference. Keep enough list padding that it cannot cover the last row.
- Keep Add Game disabled until creation exists, with the accessible description
  "Add Game — coming soon". Do not add a placeholder route or creation wizard.
- Display the built-in row as **Animals**, with bold accent-colored text and
  a slightly tinted background. Other games use white cards and regular-weight
  text. Identify built-in games explicitly in repository data, not by position
  or display name.
- Each row has the existing puzzle asset (`ui-games`), its name, and a Play action
  (`ui-play`). The whole row may also start the same game. Reuse current typography,
  spacing, colors, and card/button components.
- The UI renders a collection and supports multiple rows. Only the predefined **Animals** game
  is supplied now; additional predefined games may be added later. Future-game examples belong only in tests and previews.

## Data and identity

- Obtain game entries through the repository boundary, using a small domain summary
  containing stable ID, name, and whether it is built in. Keep content out of composables.
- The built-in game and Home catalog ID change from `choose-it` to `whats-that`.
  Pass the selected game ID to gameplay loading; do not assume the Home catalog
  entry always identifies the selected game instance.
- Keep the `CHOOSE_IT` mechanic type and asset IDs such as `choose-it-cover`.
- Future custom IDs use `whats-that-<unique suffix>`, generated once and retained
  through edits. Random UUIDs are recommended over hashes of mutable names/content.
  Do not implement custom IDs, Room entities, or custom persistence in this feature.

## Gameplay and navigation

- Entering selection does not start gameplay audio.
- Play starts the selected existing game. Preserve round generation, tap/swipe
  answers, feedback, audio, animation, and completion timing.
- Both gameplay Back actions return to the previous selection screen.
- Selection system Back and the Home tab return to Home.
- Automatic completion returns Home and clears gameplay and selection from the stack.
- Guard rapid Play taps against duplicate destinations. Leaving gameplay stops
  audio and pending feedback/completion work. Playing again after exiting starts
  a fresh session.
- Preserve selection and gameplay destinations, and current round progress,
  through ordinary activity recreation.
- Match Up, Library screens, and bottom navigation on other screens retain their behavior.

## Acceptance and verification

Verify the visible layout, fixed navigation, floating-button placement, built-in
versus other-row styling, selected IDs, disabled Add Game, both Back paths,
Home/Library navigation, rapid Play, recreation, fresh sessions, audio cleanup,
and completion stack cleanup. Update direct-entry navigation tests. Include useful
single/multiple-row, landscape, and enlarged-text previews. Run build, unit tests,
lint, affected device tests, and existing game/audio/navigation regressions.
