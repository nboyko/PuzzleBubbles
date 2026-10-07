# Iteration 1: Home screen and game navigation

Status: **In progress**. Only the user's explicit confirmation closes this iteration.

## Scope

Create the initial Puzzle Bubbles home screen with two large, image-led game
cards and navigation to separate placeholders. No game mechanics, sound
playback, matching interactions, scoring, progress, settings, Room, or networking.

The mechanic names remain **Choose It** and **Matching**. The design uses the
child-facing titles **What's That?** and **Match Up**, respectively.

## Commit phases

### Phase 1 — Home screen and game navigation

Commit: `HEAD` — `feat: add home screen and game navigation`

This phase establishes the first child-facing screen, local game catalog,
runtime cover assets, and separate navigation destinations for both MVP games.
It also removes generated and machine-specific files from version control.

The iteration remains in progress after this phase and will close only when the
user explicitly confirms completion.

#### Changes

##### Home catalog and architecture

- Adapted the existing single-module Android Compose project rather than
  recreating it.
- `domain/GameDefinition.kt` defines catalog metadata and game types;
  `domain/GameRepository.kt` exposes the catalog.
- `data/LocalGameRepository.kt` supplies the two local game definitions.
- `presentation/home/HomeViewModel.kt` exposes the catalog to the UI;
  `MainActivity.kt` wires the repository, ViewModel, theme, and application UI.
- Domain metadata uses stable cover asset IDs. `data/LocalImageAssetResolver.kt`
  maps those IDs to bundled resources, rendered by `presentation/components/AssetImage.kt`.

Paths above are relative to `app/src/main/java/com/example/puzzlebubbles/`.

##### Home design and assets

- `presentation/home/HomeScreen.kt` implements a pastel background, decorative
  bubbles, centered headings, and large clickable cards with colored title bars.
- The layout uses one column on narrow screens and two columns at widths of
  at least 700 dp, with scrolling for smaller viewports.
- Complete square cover images fit above the name stripes without cropping
  or overlapping the text.
- Runtime covers are optimized WebP resources in `app/src/main/res/drawable-nodpi/`,
  derived from `assets/source/images/game-types/whats_that.png` and `match_up.png`.
- Bundled **Baloo 2** is used for home headings and **Lilita One** for game card
  titles. Font licenses are retained in `app/src/main/assets/licenses/`.
- Existing Material theme setup is retained; the home has its own light palette.
  Downloadable-font setup and unused generated theme variants were removed.
- The display-name mapping and cover treatment are reflected in
  `docs/mvp-spec.md` and `docs/content-assets.md`.

##### Navigation

- `presentation/PuzzleBubblesApp.kt` uses Navigation Compose with `home`,
  `choose-it`, and `matching` routes, replacing boolean screen switching.
- Separate `ChooseItGameScreen` and `MatchingGameScreen` destinations reuse
  a small shared placeholder layout with game-specific descriptions.
- Both the visible Back button and system Back return to Home.
- Navigation Compose was added for this flow; no gameplay or persistence
  dependencies were introduced.

##### Repository hygiene

- Root `.gitignore` excludes local build/cache output and machine-specific settings.
- Gradle caches, `local.properties`, and device/personal IDE state were removed
  from Git tracking while preserving existing local copies.
- Shared IDE settings, including code styles and inspections, remain tracked.
- Earlier iteration notes were removed at the user's request. This file is the
  newly requested iteration 1 record.

#### Verification

- Debug assembly, the unit-test task, and Android lint completed successfully
  during the review. Some Gradle tasks were up to date.
- An earlier emulator run passed five instrumentation tests covering cover
  decoding, clickable game cards, both placeholder routes, visible/system Back,
  and destination restoration after activity recreation.
- `LocalGameRepositoryTest` checks the expected catalog metadata and uniqueness.
- `HomeNavigationTest` and `CoverAssetsTest` provide the instrumentation coverage.
- Ignore rules and retained local files were checked after the Git cleanup.
- Instrumentation tests were not rerun during the latest review or Git cleanup.

#### Remaining issues and review observations

- On smaller phones, the second game's title requires scrolling. This is a
  discoverability concern to evaluate, not a confirmed functional defect.
- Automated navigation checks do not establish visual correctness. A focused
  check of the final layout on a small phone, with enlarged text and dark mode,
  including older supported Android system bars, remains appropriate.
- The review found no confirmed critical blocker for this slice. Only repository
  hygiene was authorized for correction; the other observations remain unchanged.
- User acceptance and explicit iteration completion are still pending.
