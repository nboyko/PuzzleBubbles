---
name: finalize-feature
description: On explicit invocation, verify implementation against docs/plan.md, synchronize documentation, add a verified functional phase to the existing iteration, and commit the implementation, current plan, and iteration record together.
---

# Finalize feature

Run only when the user explicitly invokes `$finalize-feature`. A request to
create/edit this skill, a coding task, or a Git commit is not an invocation.
Do not add hooks or implicit triggers. An invocation authorizes a scoped local
commit after verification succeeds, unless the user requests review only.
It does not authorize amending/rebasing history or pushing.
Do not use Git metadata to define iteration status or require it in iteration records.

Finalize one agreed feature as a phase within a larger, manually defined iteration.
Finalizing the feature does not finish that iteration.

## Establish scope

- Read applicable `AGENTS.md`, `docs/plan.md`, the selected iteration record, and relevant
  `docs/mvp-spec.md` and `docs/content-assets.md` sections.
- Use the user-selected iteration record; otherwise use the latest existing
  iteration by its numbered filename in `docs/iterations/`. Do not create a new
  iteration automatically. If the latest iteration is already closed, ask where
  to record the phase instead of reopening it or creating another file.
- Only the latest numbered iteration may be open. Do not add phases to closed
  earlier iterations or reopen them automatically. A new iteration requires the
  user's explicit closure of the current one and authorization to create the
  next record. If a selected record conflicts with this rule, clarify the target
  before finalizing rather than silently changing iteration statuses.
- Use `docs/plan.md` as the agreed step's expected feature, requirements, exclusions,
  and acceptance checks. If it is missing, undefined, or materially conflicts with
  the iteration/specification, stop and ask for the agreed scope; do not infer
  requirements from what happens to be implemented.
- Resolve the functional milestone from the user's request and current plan.
  Ask if scope or acceptance criteria are materially ambiguous. Do not require
  the entire MVP to finalize a smaller agreed milestone.
- Inspect the actual code, tests, assets, and configuration. Git diffs/history may
  help locate changes, but review implemented behavior beyond uncommitted diffs.
  Preserve unrelated changes and existing staged work.
- Keep the iteration open unless the user explicitly asks to finish it and the
  verification gate passes. A review-only invocation does not close it.

## A — Verify implementation

Compare each current-plan requirement with actual code and evidence. Keep its
acceptance criteria intact during verification. Inspect relevant
state transitions, navigation, lifecycle cleanup, assets, and layer boundaries.
Check applicable dependency/SDK/JDK/Gradle compatibility, configuration changes,
versioning, and migrations. Do not add persistence or upgrade dependencies just
to satisfy a checklist.

For application changes, relevant Android checks normally include:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain
```

Run `:app:connectedDebugAndroidTest` when relevant device tests and a device are
available. Use the configured JDK/SDK and caches; offline mode is appropriate
when dependencies are cached. Inspect results; old reports and up-to-date tasks
are not new coverage. Verify affected audio, animation, rapid taps, Back, and
completion behavior on-device when essential to the milestone.

Report requirements as verified, partial, missing, or unverified, with concise
evidence and limitations. Separate critical blockers from optional improvements.
Missing required functionality, regressions, incompatible configuration, failing
required checks, incorrect assumptions, or unverified essential behavior block
finalization. Unavailable checks are unverified, not passed. Assess existing
warnings by their effect on this scope.

**Gate:** Critical issues stop the workflow before documentation synchronization,
reconciliation, or committing. Report affected requirements, evidence, check results, and
the next necessary action. Do not weaken requirements, hide blockers in future
work, declare completion, or implement unrequested fixes.

## B — Synchronize permanent documentation

After A succeeds, update only existing documentation affected by verified behavior.
Here, `README.md` describes current features/architecture;
`docs/content-assets.md` holds asset conventions and actual storage; and
`docs/mvp-spec.md` defines requirements rather than implementation status.

Correct obsolete claims and document relevant decisions/constraints by topic.
Permanent documentation describes the current application, not future systems,
development diaries, copied plans, or detailed implementation history. Link to
the topic source instead of duplicating it. Distinguish format conventions from
actual packaged assets. Do not create extra reports or files without authorization.

Requirements change only through explicit product decisions, not to make
incomplete implementation appear successful. Preserve useful technical information
when shortening overlapping descriptions.

## C — Reconcile the functional milestone

Replace outdated completed planning detail with a brief description of working
functionality and a compact verification/limitations note. Do not create a phase
per commit or retain commit hashes, Git lookups, implementation diaries, or
detailed change logs.

After verification succeeds, append the next numbered functional phase to the
selected iteration record, for example `### Phase 2: Library empty screens`.
Include only a short description of verified behavior, relevant check results,
and any limitations. Preserve earlier phases; if the same functional step is
finalized again, update its phase rather than duplicating it. A phase represents
a verified feature step, not a Git commit. Do not add a verified phase when the
verification gate fails or for a review-only invocation; report findings instead.

Preserve unfinished requirements separately in the appropriate existing upcoming
iteration, preserving its intent. Do not silently defer a critical requirement to
pass the gate: a changed milestone boundary requires the user's explicit decision.
If no upcoming iteration exists, retain a clearly separate upcoming-work section
pending its placement; do not invent requirements or create a file without permission.

Closure reflects explicit user acceptance plus verification, not a commit.
When reviewing a step inside an open iteration, update only the selected scope
and keep unfinished work visible. Finalizing a phase does not finish the full
iteration. Keep its status in progress until the user explicitly says the
iteration is finished and the verification gate passes; invoking this skill alone
is not that confirmation.

Mark the current plan's verified outcome briefly, preserving the original agreed
requirements and acceptance criteria. Include that plan and the reconciled
iteration record with the resolved implementation in the completion commit.
Do not replace the plan with the next feature during this workflow. Update it
later, when the user agrees the next expected feature; do not invent that scope.

## D — Review, commit, and report

Review the documentation diff for consistency with verified code and the agreed
milestone. Ensure unrelated files and pending requirements were preserved. Rerun
checks only if changes invalidate previous evidence.

For a normal invocation, stage only reviewed paths/hunks belonging to the agreed
step, including its implementation, `docs/plan.md`, the selected iteration record,
and affected permanent documentation. Inspect the staged diff for unrelated edits,
secrets, machine-specific files, and generated build output. Never use blanket
`git add .` or `-a`. Preserve unrelated staged work; if the commit cannot be
isolated safely, leave changes for review and explain the limitation.

After all blockers are resolved and relevant checks pass, create a concise
Conventional Commit describing the working feature. Do not bypass failing hooks
or rewrite history. If committing fails or is prohibited, report that finalization's
commit step is pending. Review-only invocations leave all changes uncommitted.
Inspect post-commit status and report the actual hash in the response, not the
iteration record. If no changes remain to commit, report that rather than
creating an empty commit. Do not push.

Report what was verified, which documentation was synchronized, what remains
unfinished/unverified, and whether the iteration is closed or still open.
Keep the report concise and in the response, not a new file. Include the commit
result or reason it remains pending, and whether the plan is ready to be updated
for the next agreed feature.
