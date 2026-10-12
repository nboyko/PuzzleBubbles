# Puzzle Bubbles

Native Android educational game built with Kotlin and Jetpack Compose.
This overview describes the current checkout, not iteration acceptance.

## Current behavior

- Home shows image cards for **What's That?** (Choose It) and **Match Up**
  (Matching), with Baloo 2 headings and Lilita One card titles.
- What's That? uses ten bundled image/audio pairs. Each randomized session has
  ten rounds, each target once, and two shuffled choices with explicit correctness.
  Audio plays on entry and can be replayed. Wrong answers stay on the round;
  correct answers hide the other choice, center/grow the bubble, and play success
  audio before advancing. Completion shows a pulsing trophy for three seconds,
  then returns Home.
- Match Up is a separate placeholder destination. Back returns Home.
- The Home settings gear is tappable but performs no action.

## Architecture

One Android `app` module, with sources under
`app/src/main/java/com/example/puzzlebubbles/`:

- `presentation/`: Compose screens, ViewModels, and Navigation Compose routes
  (`home`, `choose-it`, `matching`); game screens are grouped by mechanic.
- `domain/`: catalog/content models and pure session generation/state transitions.
  Static content stays separate from session progress; asset references are stable IDs.
- `data/`: `GameRepository` implementation with a Kotlin preset, resource
  resolvers, and single-instance `MediaPlayer` playback. Resource lookup stays at
  the rendering/playback boundary. `MainActivity` wires dependencies; the Choose It
  ViewModel is scoped to its navigation destination.

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
