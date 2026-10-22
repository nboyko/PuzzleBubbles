---
name: finalize-feature
description: On explicit invocation, verify a feature against its agreed plan, review tests and Compose previews, reconcile documentation and the iteration phase, and commit scoped changes.
---

# Finalize feature

Run only when the user explicitly invokes `$finalize-feature`. Coding tasks,
regular commits, and requests to edit this skill do not invoke it. Do not add
hooks or implicit triggers; preserve explicit-only discovery policy.

Apply this revised workflow to future features, without retroactively imposing
new gates on previously finalized phases or rewriting their commits.

Invocation authorizes clear in-scope fixes, missing tests/previews, affected
documentation updates, and a scoped local commit. A review-only invocation makes
no source/documentation changes and does not commit. Neither mode authorizes
new requirements, unrelated refactoring, pushing, or history rewriting.

## 1. Establish scope

- Read applicable `AGENTS.md`, `docs/plan.md`, the selected iteration record,
  relevant `docs/mvp-spec.md` and `docs/content-assets.md` sections, and available
  designs/references. Inspect actual code, tests, assets, and configuration beyond
  the working diff.
- Use the agreed plan and explicit user decisions as the feature requirements
  and acceptance criteria. Resolve material conflicts or missing scope before
  proceeding; do not infer requirements from incomplete implementation or require
  the whole MVP to finalize one feature.
- Use the requested iteration file, otherwise the latest numbered existing file
  under `docs/iterations/`. Only that latest iteration may be open. If the target
  is closed or conflicts with this convention, clarify the target. Do not create
  or reopen iteration files automatically.
- Preserve unrelated working-tree and staged changes. A regular commit does not
  establish completion or authorize documentation reconciliation.

## 2. Review implementation and test coverage

For each relevant requirement, identify its implementation and classify it as
**verified**, **partial**, **missing**, or **unverified**, with evidence.

Inspect affected state transitions, navigation, lifecycle cleanup, cancellation,
rapid interactions, error recovery, assets, persistence, and architectural
boundaries. Check dependency/SDK/JDK/Gradle compatibility, configuration,
versioning, and migrations where affected. Do not upgrade dependencies or add
systems just to satisfy a checklist.

Designs are guidance unless an explicit user requirement makes an aspect
mandatory. Report ordinary layout/style differences as observations; a coherent
alternative is acceptable. Visual defects block only when they break usability,
rendering, navigation, essential controls, or an explicitly mandatory requirement.
Do not restyle working UI merely to reproduce an outdated mockup.

Review whether important behavior is meaningfully tested, not just the test
count or coverage percentage. Add missing tests where reliable automation is
practical, using existing conventions and the lowest suitable level: pure logic,
state/ViewModel, repository, or Android/Compose integration. Cover relevant edge
cases and reported regressions without duplicating coverage or testing decorative
implementation details. Do not add a test framework without explicit approval.

Fix confirmed in-scope issues when the resolution is clear. If a fix requires an
ambiguous product decision or expanded requirements, stop and report it. Never
weaken requirements or remove, weaken, or bypass tests merely to pass verification.

## 3. Run checks and inspect the UI

Use configured tools, JDK/SDK, Gradle wrapper, and caches; offline mode is suitable
when dependencies are cached. For application changes, normally run:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain
```

Run relevant configured formatting/static-analysis/module checks. Discover actual
tasks; do not install optional tooling just to satisfy this workflow. When device
tests are relevant and a suitable device is available, run:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest --console=plain
```

Inspect actual reports. Cached/up-to-date results are not newly executed coverage.
Unavailable checks are **unverified**, never passed. Diagnose environmental
failures and use a targeted retry when justified; do not retry indefinitely or
silently dismiss failures as flaky.

For user-facing changes, verify essential flows on-device when available:
happy path, Back/cancellation, rapid actions, errors, lifecycle, and completion
where relevant. Audio, animation, gestures, and system integrations need runtime
evidence when tests alone cannot establish correctness. Avoid unrelated flows
and external resources.

For new or materially changed previewable screens and meaningful reusable visual
components, ensure useful `@Preview` coverage. Represent relevant content, empty,
loading, error, completion, or disabled/selected states without redundant previews
for trivial wrappers. System-owned UI needs no application preview.

Previews must compile, resolve assets, use appropriate theme/state, and avoid real
navigation, storage, audio, or network dependencies. Prefer existing stateless
content composables and small fixtures; avoid abstractions solely for previews.
Render and inspect previews when tooling permits, checking missing assets,
clipping, overlap, and mandatory design requirements. Otherwise inspect definitions,
compile, and use device rendering as appropriate. State exactly what was rendered;
compilation alone is not visual verification.

## 4. Apply the verification gate

Block finalization for missing required functionality, feature regressions,
incompatible configuration, significant usability/rendering defects, unmet mandatory
visual requirements, failed required
checks attributable to the feature, or essential behavior without sufficient
verification. Missing meaningful previews or practical automated coverage for
essential feature behavior also block finalization.

Pre-existing issues are non-blocking unless critical. Critical examples include
application crashes, data loss/corruption, a broken build, or unusable core flows,
even if they predate this feature. Before treating a failure as non-blocking,
establish with baseline evidence that it is pre-existing, unrelated, and not
critical; verify the feature through adequate focused checks. Record the failed
check and rationale, never label it passed or suppress it. Unknown cause is not
proof of a pre-existing issue.

An unavailable check blocks only when essential correctness cannot be established
through adequate alternative evidence. Report other limitations and warnings by
impact. Fix clear in-scope blockers and rerun affected checks. If any blocker
remains, stop before documentation synchronization, phase reconciliation, or
committing; report evidence and the next required action. Do not hide blockers
by changing acceptance criteria or moving required work into the future.

## 5. Synchronize documentation and reconcile the phase

Only after the gate passes:

- Update affected existing documents by topic: `README.md` describes verified
  capabilities/architecture; `docs/content-assets.md` describes asset/storage
  conventions; `docs/mvp-spec.md` defines requirements, not implementation status.
  Correct outdated claims, preserve useful constraints, and link rather than
  duplicate. Do not copy plans, speculative features, diaries, or detailed change
  logs into permanent documentation. Do not create extra files without a request.
- Append the next concise functional phase to the selected iteration, or update
  the existing phase when re-finalizing the same step. Record working behavior,
  verification, and limitations; preserve earlier phases. No commit hashes,
  per-commit phases, exhaustive QA reports, or Git-based completion criteria.
- Preserve unfinished requirements separately in an appropriate existing upcoming
  record or a clearly separated upcoming-work section. Do not invent scope or
  defer a blocker to pass the gate.
- Keep the full iteration open until the user explicitly finishes it and required
  verification passes. A finalized feature phase does not close its iteration.
- Briefly mark the current plan's verified outcome while preserving agreed
  requirements and acceptance criteria; replace obsolete planning details with a
  factual summary where appropriate. Include this plan in the completion commit.
  Do not introduce the next feature plan here; update it later, after the current
  verified plan is committed and the next scope is agreed.

## 6. Review, commit, and report

Review final code/documentation consistency and rerun checks only when changes
invalidate previous evidence. Stage only reviewed feature paths/hunks, including
relevant implementation, tests, previews, current plan, iteration record, and
permanent documentation. Preserve unrelated staged work; do not use blanket
`git add .` or `git add -a`. Check for secrets, machine-specific configuration,
generated artifacts, unrelated edits, and accidental scope expansion.

Create a concise Conventional Commit after the gate passes, unless review only
was requested. Do not bypass failing hooks, push, rewrite history, or make empty
commits. If safe isolation or committing is impossible, leave changes for review
and explain the pending step. Inspect post-commit status.

Report concisely: functionality verified; fixes/tests/previews added; actual check
and rendering results; documentation and phase updated; unresolved limitations;
iteration status; commit hash or reason pending; and whether the plan is ready
for the next agreed feature. Do not create a separate report file.
