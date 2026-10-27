# Plan 006 — Sounds empty-state recording action

Status: **Verified and finalized** as Phase 5 of the still-open Iteration 2.
No next feature has been agreed.

## Goal

Replace the empty Sounds page's "No sounds yet" message with an **Add Record**
button beneath the sound icon, following the Photos empty-state pattern.

## Acceptance criteria

1. With no saved sounds, show the existing sound icon above Add Record.
2. Keep the header plus action with its accessible Add Sound label.
3. Both actions open the existing idle recording modal. Opening it neither
   requests microphone permission nor starts recording; Record retains that role.
4. Share existing loading, cleanup, error, busy, and modal guards between both
   entry points. Background actions remain blocked while a modal is open.
5. Hide the extra button when saved sounds exist; restore it after deleting the
   last recording. Keep existing recording, list, and navigation behavior.
6. Compile existing previews, inspect portrait/landscape empty states on an available device,
   and run build, unit tests, lint, and affected Sounds/Photos/navigation tests.

No new recording route, asset, persistence behavior, or content model is needed.

## Verified outcome

Add Record replaces the empty-state message beneath the sound icon. It shares
the header plus action and guards, opens the same idle modal, disappears with
saved sounds, and returns after the last deletion. The icon can shrink to keep
the button visible in landscape.

Debug build, 14 unit tests, 33 initial affected emulator tests, and all 16 Sounds
tests after the landscape fix passed. Lint reports zero errors and 18 warnings.
Portrait/landscape emulator screens were rendered and inspected, including opening
the modal from Add Record. Existing previews compiled; physical-device verification
of this change and IDE preview rendering remain unverified.
