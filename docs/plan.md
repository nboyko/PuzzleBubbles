# Plan 005 — Forgiving image selection

Status: **Verified and finalized** as Phase 4 of the still-open Iteration 2.
Iteration 1 remains closed. The acceptance criteria below describe the completed
improvement; no next feature has been agreed.

## Goal

Let young children select a What's That? image with either a tap or an imprecise
swipe, reusing existing answers, feedback, animation, and round transitions.

## Acceptance criteria

1. A normal tap and accessible click still select an image exactly once.
2. A swipe starting on either image bubble, in any direction, selects that image
   once it crosses the normal touch threshold, even when it leaves the bubble.
3. A swipe crossing the other image submits only the starting image's answer.
4. Correct and incorrect swipes use existing feedback and retry/advance behavior.
5. Input during feedback and a finger held into the next round cannot submit
   another answer. Cancellation before the movement threshold selects nothing.
6. Back, Replay, game audio, and layout remain usable. Existing screen previews
   compile; inspect the gesture and feedback on a device when available.
7. Add focused Compose gesture tests and run build, unit tests, lint, affected
   device tests, and game/navigation regressions using existing tooling.

No change to Matching, correctness data, scoring, or other controls is required.

## Verified outcome

Taps, accessible clicks, and swipes select the starting image once through the
existing answer logic. Incorrect feedback permits retry; correct feedback advances
normally. Crossing another image, input during feedback, held gestures across
rounds, and cancellation below the threshold are covered by gesture tests.

Debug build, 14 unit tests, and 21 affected emulator tests passed; lint reports
zero errors and 18 warnings. Existing Compose previews compiled. Rendered emulator
checks covered correct/incorrect swipes, retry, progression, Replay, and Back;
physical-phone gesture verification and IDE preview rendering remain unverified.
