# Puzzle Bubbles

Native Android educational game for toddlers, built with Kotlin and Jetpack Compose.

## Why this app

I am building Puzzle Bubbles for my toddler and for other toddlers. It ships with
simple, ready-to-play games, and it also lets parents create custom games for their
own child. Instead of choosing between generic stock pictures, a child sees what is
familiar from real life: parents photograph family members, pets, and everyday
objects, and record prompts in their own voices, in the languages and phrases
they actually use at home. The child then matches a familiar voice to a familiar
face or object, building connections between what they hear and what they see.

The app is an MVP and is being developed step by step. More games will follow.
Everything is local: there is no backend, no account, and no network access. Photos
and recordings stay in the app's private storage on the parent's phone and are
excluded from cloud backup, so personal family media never leaves the device.

## Demo

Short screen recordings (MP4) from the [v0.1.0 release](https://github.com/nboyko/PuzzleBubbles/releases/tag/v0.1.0):

- [Add photos and record sounds in the Library](https://github.com/nboyko/PuzzleBubbles/releases/download/v0.1.0/add_to_lib.mp4)
  — import and crop a photo, record a prompt in a parent's voice.
- [Play a custom game](https://github.com/nboyko/PuzzleBubbles/releases/download/v0.1.0/play_custom_game.mp4)
  — hear the recorded prompt and choose the matching photo.
- [Edit and delete a custom game](https://github.com/nboyko/PuzzleBubbles/releases/download/v0.1.0/edit_delete_game.mp4)
  — swipe a game to change its questions or remove it.

## Features

- **What's That?** — hear a sound and tap or swipe the matching picture. The
  built-in **Animals** game has ten randomized rounds, forgiving wrong answers,
  a success animation, and a trophy at the end.
- **Custom games** — parents choose photos from their Library and pair each
  question with their own recording (1–10 questions). Games can be edited and
  deleted; photos and sounds used by a game are protected from deletion.
- **Photos** — import one image at a time through the system picker (no media
  permission needed) and crop it to a square.
- **Sounds** — record short prompts (up to five seconds), name, preview, and save
  them. All sounds are balanced to one common loudness.
- **Made for small hands** — large targets, pictures over text, and selection by
  tap or swipe in any direction.
- **Match Up** — second game, in progress.

## Tech highlights

- Kotlin, Jetpack Compose, Navigation Compose, ViewModel, Coroutines/Flow.
- Layered `presentation` / `domain` / `data` design. Screens use domain models and
  stable asset IDs only, so storage can later move to files or a remote service
  without changing game screens.
- Room with exported schemas and non-destructive migrations (versions 1–4).
- Crash-safe media storage: staged writes and recovery of interrupted saves/deletes.
- Audio: microphone capture, and loudness normalization with `MediaCodec`
  decoding/encoding.
- Tests: JVM unit tests for game sessions and audio math; instrumented Compose UI,
  storage, gesture, and audio tests.

## Architecture

One Android `app` module under `app/src/main/java/com/example/puzzlebubbles/`:

- `presentation/` — Compose screens, ViewModels, and navigation, grouped by game.
- `domain/` — content models, repository contracts, and pure session logic. Static
  content stays separate from session state.
- `data/` — repository implementations: bundled games, Room metadata, private photo
  (WebP) and recording (AAC) files, and audio playback.

Bundled source assets live under `assets/source/`; runtime copies are Android
resources. See [asset conventions](docs/content-assets.md) for formats and IDs.

## Build and run

Open the project in Android Studio (compile/target SDK 36, minimum SDK 26), or from
the repository root on Windows:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest   # needs an emulator or device
```

Run instrumented tests on a disposable emulator: their cleanup can uninstall the
app and clear its Library data.

## Project docs

- [MVP specification](docs/mvp-spec.md) — product requirements in detail.
- [Iteration 1](docs/iterations/001-iteration.md) and
  [Iteration 2](docs/iterations/002-iteration.md) — verified milestones.
- [AGENTS.md](AGENTS.md) — development rules and workflow.

## Credits

- Images (animals, game covers, gameplay images) are AI-generated for this project
  or come from [Unsplash](https://unsplash.com) under the
  [Unsplash License](https://unsplash.com/license).
- Bundled sounds come from [Pixabay](https://pixabay.com) under the
  [Pixabay Content License](https://pixabay.com/service/license-summary/).
- Fonts: Lilita One and Baloo 2 under the SIL Open Font License 1.1; see
  `app/src/main/assets/licenses/`.

## License

Copyright © 2026 Nataliia Boiko. All rights reserved.

This repository is published for viewing and evaluation only. No license is granted
to copy, modify, distribute, or use this code or its original assets, in whole or
in part, without prior written permission. Third-party assets listed under Credits
remain under their own licenses.
