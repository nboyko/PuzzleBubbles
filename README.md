# Puzzle Bubbles

Native Android educational game built with Kotlin and Jetpack Compose.
This overview describes the current checkout, not iteration acceptance.

## Current behavior

- Home shows image cards for **What's That?** (Choose It) and **Match Up**
  (Matching), with Baloo 2 headings and Lilita One card titles.
- Home and My Library share a fixed bottom navigation bar with labeled icons
  and a raised bubble around the active icon. There are no top tabs.
- My Library has Photos and Sounds cards opening dedicated empty screens.
  Each child has its title, icon, empty message, Back to My Library, and the
  fixed Home/Library navigation bar with Library selected. No sample media,
  importing, playback, or storage is included in Library. Home scroll position
  and navigation survive recreation.
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
  game screens are grouped by mechanic. Library's empty states are presentation
  only; no user-media data layer is introduced.
- `domain/`: catalog/content models and pure session generation/state transitions.
  Static content stays separate from session progress; asset references are stable IDs.
- `data/`: `GameRepository` implementation with a Kotlin preset, resource
  resolvers, and single-instance `MediaPlayer` playback. Resource lookup stays at
  the rendering/playback boundary. `MainActivity` wires dependencies; the Choose It
  ViewModel is scoped to its navigation destination.

Screen-scoped effects cancel feedback and completion timers on disposal and stop
audio when leaving the game. Leaving and reopening starts a fresh session;
session progress is not stored persistently. Blur-based icon glow is supported
on Android 12+; older supported devices retain the unblurred icon and rim.

Content is bundled locally. There is no Room database or network integration.
Source assets live under `assets/source/`; runtime assets are Android resources.
See [asset conventions](docs/content-assets.md) for formats, paths, and IDs.

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
- [Iteration 1](docs/iterations/001-iteration.md): functional scope and pending work.
- [AGENTS.md](AGENTS.md): project rules and explicit `$finalize-iteration` workflow.

Finalization runs only when explicitly invoked. It verifies functionality against
the current plan, synchronizes affected documentation, reconciles the iteration
record, and commits those changes together unless review only is requested.
Regular coding tasks and commits do not trigger it. Update the plan for the next
agreed feature afterward; iteration records do not require Git metadata.
