# AGENTS.md

## Project

Puzzle Bubbles is a native Android educational game for young children.

Before making product or architecture changes, read:

- `docs/mvp-spec.md`
- `docs/content-assets.md`

## Current Goal

Build the smallest working version of the application supporting two game mechanics:

- **Choose It** — hear a sound and choose the matching image;
- **Matching** — match images from one side with images on the other side.

Keep the first iteration intentionally simple.

Do not introduce categories, topics, content sets, sound modes, or other content taxonomies yet.

Additional categorization may be introduced later when there is a concrete need for it.

## Stack

- Kotlin
- Android
- Jetpack Compose
- ViewModel
- Coroutines / Flow where useful
- Bundled structured data for initial static content
- Room only when structured persistent local storage is actually useful

## Architecture

Keep these concerns separate:

```text
presentation
domain
data
```

UI must consume domain models and must not depend on Room entities, Android resource IDs, filesystem paths, or URLs.

All game content must be accessed through a repository abstraction.

Binary assets must be referenced through stable asset IDs.

The initial repository is local. A future remote repository should be able to replace it without redesigning game screens.

## Asset Storage

Do not store image or audio blobs in Room.

Initially:

- optimized images live in Android resources;
- optimized audio lives in Android resources;
- source/master assets live outside Android runtime resources;
- structured metadata may live in bundled JSON or Room.

Use stable asset IDs so local resources can later be replaced with local files or CDN URLs without changing domain models.

## Development Rules

Prefer simple code over premature abstractions.

Do not:

- add a backend;
- add networking;
- add authentication;
- add scoring;
- add analytics;
- add user profiles;
- introduce content categories or taxonomies;
- create generic frameworks for hypothetical future games.

Do not hard-code game content inside composables.

Correct answers and semantic relationships must be explicit in game data.

Presentation order may be randomized by game logic but must not define semantic relationships.

## UI

The application is designed for young children.

Prefer:

- large interactive elements;
- images over text;
- simple screens;
- immediate visual feedback;
- forgiving interactions.

Avoid tiny controls, dense layouts, or text-heavy instructions.

## Changes

Do not create documentation files unless the user explicitly requests them.
This includes implementation plans, iteration notes, change summaries, and reports.
An implementation request alone does not authorize creating documentation;
summarize the work in the response instead.

### Iteration tracking

- An iteration remains in progress until the user explicitly says it is finished.
  Completed implementation, passing tests, or a commit do not close an iteration.
- Record each commit belonging to the active iteration as a separate, numbered
  phase in its existing `docs/iterations/` file. Include the commit reference,
  changes, verification results, and any remaining issues.
- Keep uncommitted work clearly marked as pending; do not invent commit phases
  or commit hashes. Add the actual commit reference once it is available.
- Maintain the iteration file requested by the user; do not create additional
  iteration files without an explicit request. These rules do not authorize
  making commits without the user's instruction.

Before implementing a feature:

1. Identify the smallest version that satisfies the current specification.
2. Reuse existing domain concepts where appropriate.
3. Do not generalize unless current use cases require the abstraction.
4. Keep game state separate from static game content.
5. Add tests for non-trivial game/domain logic.

When a requested change conflicts with `docs/mvp-spec.md`, update the specification explicitly instead of silently changing product behavior.
