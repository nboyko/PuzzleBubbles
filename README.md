# Puzzle Bubbles

Native Android educational game built with Kotlin and Jetpack Compose.
This overview describes the current checkout, not iteration acceptance.

## Current behavior

- Home shows image cards for **What's That?** (Choose It) and **Match Up**
  (Matching), with Baloo 2 headings and Lilita One card titles.
- Home and My Library share a fixed bottom navigation bar with labeled icons
  and a raised bubble around the active icon. There are no top tabs.
- My Library has Photos and Sounds destinations with Back to My Library and
  fixed Home/Library navigation. Photos shows saved images or an empty state,
  with a plus button beside its title.
  No sample media is added. Home scroll position and navigation survive recreation.
- Add Photo selects one image through Android's system picker and opens a
  square crop editor in a modal. Pan/pinch adjusts the crop; Save stores a
  1024 × 1024 WebP and X, Back, or the backdrop discards unsaved changes.
  Saved photos open the same editor with Delete/Save controls. Changed crops
  retain the photo's identity; unchanged Save closes without rewriting.
  The modal blocks background navigation and conflicting operations. Preview
  and busy state survive rotation; unsaved work is discarded after process
  death. Photos are not yet used in games.
- Sounds records microphone audio in a modal, stopping automatically after five
  seconds or when Stop is pressed. Native AAC output may have up to 100 ms of
  overhead; files longer than 5,100 ms are rejected and actual duration is retained.
  Empty Sounds shows an Add Record button beneath its sound icon, alongside the
  header plus action. Both open the same recording modal.
  Preview supports Play/Pause, Cancel, and
  explicit Save. Preview has a selected `Track N` name default that can be replaced;
  duplicate names are allowed. Saved rows show names, playback progress, and confirmed Delete.
  Renaming saved recordings is deferred.
  Rotation preserves the recording/preview; leaving or backgrounding discards
  unsaved work. Permission is requested only on Record. Save/Delete are guarded
  against repeated taps, and the modal blocks underlying navigation.
  The modal scrolls and resizes for the keyboard, including on Android 10.
  Native capture, duration limits, playback, and recovery have been checked on a
  physical Android 10 phone; broader device coverage and subjective speech-quality
  assessment remain outside that verification.
- What's That? uses ten bundled image/audio pairs. Each randomized session has
  ten rounds, each target once, and two shuffled choices with explicit correctness.
  Children can tap or swipe an image bubble in any direction to choose it, even
  if their finger leaves the bubble. Each gesture selects only its starting image.
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
  game screens are grouped by mechanic. Photos and Sounds have destination-scoped
  ViewModels and consume stable asset IDs.
- `domain/`: catalog/content models and pure session generation/state transitions.
  Static content stays separate from session progress; asset references are stable IDs.
  `PhotoRepository` exposes Library photo IDs and creation times without file paths.
  `SoundRepository` exposes recording IDs, names, durations, and creation times; separate
  recorder and playback contracts keep native audio out of screen state.
- `data/`: `GameRepository` implementation with a Kotlin preset, resource
  resolvers, and single-instance `MediaPlayer` playback. `data/photos` handles
  sampled decoding, EXIF orientation, center cropping, private WebP files, and
  Room metadata (schema version 3, exported under `app/schemas/`). Saves and
  deletes are serialized with reconciliation of interrupted operations.
  `data/sounds` stores private AAC/MPEG-4 recordings, stages validated saves,
  and reconciles interrupted writes and deletions. The existing `photos.db` has
  an additive migration for sound metadata, preserving photo records.
  `PuzzleBubblesApplication` owns Library storage services; `MainActivity` wires the
  rendering boundary and game dependencies. The Choose It ViewModel is scoped
  to its navigation destination.

Screen-scoped effects cancel feedback and completion timers on disposal and stop
audio when leaving the game. Leaving and reopening starts a fresh session;
session progress is not stored persistently. Blur-based icon glow is supported
on Android 12+; older supported devices retain the unblurred icon and rim.

Game content is bundled locally; imported photos and user recordings stay in
app-owned storage and are excluded with their metadata from cloud backup and
device transfer. Recordings are not yet used in games.
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
- [Current plan](docs/plan.md): agreed feature scope and acceptance criteria when a plan is needed.
- [Iteration 1](docs/iterations/001-iteration.md): completed Home and What's That? milestone.
- [Iteration 2](docs/iterations/002-iteration.md): Library functionality and verified phases.
- [AGENTS.md](AGENTS.md): project rules and explicit `$finalize-feature` workflow.

Finalization runs only when explicitly invoked. It verifies functionality against
the agreed scope (an explicit request or the current plan), synchronizes affected
documentation, reconciles the iteration record, and commits those changes together
unless review only is requested.
Regular coding tasks and commits do not trigger it. Small, clearly specified
improvements can be recorded directly in their iteration phase without a feature
plan. Iteration records describe verified functionality and limitations, without
backlog sections or Git metadata.
