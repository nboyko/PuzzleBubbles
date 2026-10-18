# Puzzle Bubbles

Native Android educational game built with Kotlin and Jetpack Compose.
This overview describes the current checkout, not iteration acceptance.

## Current behavior

- Home shows image cards for **What's That?** (Choose It) and **Match Up**
  (Matching), with Baloo 2 headings and Lilita One card titles.
- Home and My Library share a fixed bottom navigation bar with labeled icons
  and a raised bubble around the active icon. There are no top tabs.
- My Library has Photos and Sounds destinations with Back to My Library and
  fixed Home/Library navigation. Sounds remains an empty placeholder. Photos
  shows saved images or an empty state, with a plus button beside its title.
  No sample media is added. Home scroll position and navigation survive recreation.
- Add Photo selects one image through Android's system picker and opens a
  full-screen, centered square preview. Save stores a 1024 × 1024 WebP; Cancel
  discards it. A saved photo opens Delete/Save/X controls: Delete removes only
  the Library copy, while Save and X close unchanged. The modal blocks Back and
  background navigation. Preview and busy state survive rotation; an unsaved
  preview is discarded after process death. Manual editing and use in games
  are not included.
- What's That? uses ten bundled image/audio pairs. Each randomized session has
  ten rounds, each target once, and two shuffled choices with explicit correctness.
  Audio plays on entry and can be replayed. Wrong answers stay on the round;
  correct answers hide the other choice, center/grow the bubble, and play success
  audio together before advancing. Growth is capped at +30% and stays inside the
  gameplay area. Completion shows a pulsing gold trophy inside the supplied
  bubble background for at least three seconds, waiting for the complete yay
  sound before returning Home.
- Match Up is a separate placeholder destination. Back returns Home.
- Game controls use `BubbleButton`: circular for icon-only controls and pill-shaped
  for text, with configurable colors, border, and optional restrained neon glow.
  Back is borderless; Replay is bordered. Both match the game title's blue.

## Architecture

One Android `app` module, with sources under
`app/src/main/java/com/example/puzzlebubbles/`:

- `presentation/`: Compose screens, ViewModels, and Navigation Compose routes
  (`home`, `library`, `library/photos`, `library/sounds`, `choose-it`, `matching`);
  game screens are grouped by mechanic. Photos has a destination-scoped ViewModel
  and renders images through stable asset IDs.
- `domain/`: catalog/content models and pure session generation/state transitions.
  Static content stays separate from session progress; asset references are stable IDs.
  `PhotoRepository` exposes Library photo IDs and creation times without file paths.
- `data/`: `GameRepository` implementation with a Kotlin preset, resource
  resolvers, and single-instance `MediaPlayer` playback. `data/photos` handles
  sampled decoding, EXIF orientation, center cropping, private WebP files, and
  Room metadata (schema version 1, exported under `app/schemas/`). Saves and
  deletes are serialized with reconciliation of interrupted operations.
  `PuzzleBubblesApplication` owns the photo services; `MainActivity` wires the
  rendering boundary and game dependencies. The Choose It ViewModel is scoped
  to its navigation destination.

Screen-scoped effects cancel feedback and completion timers on disposal and stop
audio when leaving the game. Leaving and reopening starts a fresh session;
session progress is not stored persistently. Blur-based icon glow is supported
on Android 12+; older supported devices retain the unblurred icon and rim.

Game content is bundled locally; imported photos stay in app-owned storage and
are excluded together with their metadata from cloud backup and device transfer.
There is no network integration. Bundled source assets live under `assets/source/`;
their runtime assets are Android resources.
See [asset conventions](docs/content-assets.md) for formats, paths, and IDs.

Photo selection uses AndroidX `PickVisualMedia`, including its system document
fallback, without broad media permissions. On API 36, Google Photo Picker build
36 (`com.google.android.photopicker`) returns cancellation without an image from
Preview → Select; this build uses the single-image document picker instead when
available. Other builds retain AndroidX's normal selection flow. Cancellation
never imports or saves an image.

## Build and checks

Use Android Studio's configured Gradle JDK and Android SDK (compile/target SDK 36,
minimum SDK 26). From the repository root on Windows:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

The second command requires an emulator or device. These are verification
commands, not claims that the current checkout has passed every check.

## Requirements and iteration workflow

- [MVP specification](docs/mvp-spec.md): intended product requirements.
- [Current plan](docs/plan.md): next agreed feature and acceptance criteria.
- [Iteration 1](docs/iterations/001-iteration.md): completed Home and What's That? milestone.
- [Iteration 2](docs/iterations/002-iteration.md): Library functionality and verified phases.
- [AGENTS.md](AGENTS.md): project rules and explicit `$finalize-feature` workflow.

Finalization runs only when explicitly invoked. It verifies functionality against
the current plan, synchronizes affected documentation, reconciles the iteration
record, and commits those changes together unless review only is requested.
Regular coding tasks and commits do not trigger it. Update the plan for the next
agreed feature afterward; iteration records do not require Git metadata.
