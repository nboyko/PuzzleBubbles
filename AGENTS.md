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

### Documentation and finalization

- Iterations are functional milestones, not Git commits. `docs/iterations/`
  holds brief descriptions of verified functionality and remaining milestone work.
- `docs/plan.md` defines the agreed next step and acceptance criteria. Finalization
  compares implementation against this plan; update it for the next feature only
  after the current verified plan has been committed.
- `README.md` describes implemented features and architecture;
  `docs/content-assets.md` holds asset conventions. Permanent documentation must
  reflect verified current behavior. `docs/mvp-spec.md` defines requirements,
  including unimplemented scope; it is not an implementation status report.
- At meaningful boundaries, explicitly invoke `$finalize-feature` with the
  iteration file and functional scope. Its workflow is in
  [.agents/skills/finalize-feature/SKILL.md](.agents/skills/finalize-feature/SKILL.md).
- Only an explicit `$finalize-feature` invocation runs finalization and its
  documentation synchronization/reconciliation. Coding tasks and regular commits
  do not trigger either; separate documentation requests may be handled directly.
  After verification succeeds, invocation authorizes a scoped local commit of the
  implementation, current plan, iteration record, and affected documentation,
  unless the user requests review only. It does not authorize pushing or history
  rewriting. Do not add hooks or other implicit triggers. Report critical missing
  requirements or verification
  failures; never hide them by rewriting requirements or declaring completion.

### Iteration tracking

- Only the latest numbered iteration may remain open. Do not open a new iteration
  until the user explicitly finishes the current one; keep earlier iterations
  closed and do not reopen them automatically.
- An iteration remains in progress until the user explicitly says it is finished.
  Completed implementation, passing tests, or a commit do not close an iteration.
- On explicit finalization, reconcile the plan into a brief description of
  verified working functionality. Preserve unfinished requirements separately in
  the appropriate upcoming iteration; do not silently discard or expand them.
- Each successfully finalized feature step adds a concise functional phase to
  the selected iteration, defaulting to the latest existing iteration file.
  Preserve previous phases and update an existing phase when re-finalizing the
  same step. A finalized phase does not close the full iteration.
- Iteration records do not require commit hashes, Git metadata, per-commit phases,
  implementation diaries, or detailed change logs. Git status does not determine
  functional completion.
- Maintain the iteration file requested by the user; do not create additional
  iteration files without an explicit request.

Before implementing a feature:

1. Identify the smallest version that satisfies the current specification.
2. Reuse existing domain concepts where appropriate.
3. Do not generalize unless current use cases require the abstraction.
4. Keep game state separate from static game content.
5. Add tests for non-trivial game/domain logic.

When a requested change conflicts with `docs/mvp-spec.md`, update the specification explicitly instead of silently changing product behavior.
