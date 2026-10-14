# Iteration 1: Home and playable What's That?

Status: **In progress**. The What's That? implementation step is verified;
the user has not explicitly closed the iteration.

## Verified functionality

Home provides two image cards with direct game routing and Back navigation.
What's That? opens a randomized ten-round local session with two image bubbles
per round, explicit correctness, and automatic/replayable content audio.
Wrong answers wobble without advancing. Correct answers hide the other choice,
center/grow within bounds, and wait for success audio before the next round.

Back and Replay use configurable `BubbleButton` controls. Completion displays
a pulsing yellow-gold trophy in the supplied bubble background for three seconds,
then returns Home. Static content, session state, rendering, and playback stay
separate; no persistence or network services are involved.

## Verification

Debug build, six unit tests, seven device tests, and lint passed with zero errors
and 18 non-critical warnings. Resource mappings and UI vectors were checked.
Medium-phone emulator verification covered rapid replay/answers, Back, all ten
rounds, completion artwork, and automatic Home return. Screen-disposal cleanup
was reviewed in the code.

See the retained [current plan](../plan.md) for acceptance criteria,
[README.md](../../README.md) for current behavior/architecture, and
[asset conventions](../content-assets.md) for storage and icon rendering.

## Remaining and upcoming work

User acceptance and explicit iteration closure remain pending. Match Up is still
a placeholder and is outside the verified What's That? step. A next feature or
iteration has not been selected; update the plan only after that choice.
Broader device/API and enlarged-text visual checks remain non-critical follow-up
coverage. Blur-based glow is limited to Android 12+; sharp icons and rims remain
available on older supported versions.
