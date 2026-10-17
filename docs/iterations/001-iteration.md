# Iteration 1: Home and What's That?

Status: **Finished**. Both phases are verified, and the user explicitly confirmed
that the full iteration is finished.

## Verified phases

### Phase 1: feat home page

Home displays large image cards for What's That? and Match Up, with direct game
routing and Back navigation. It uses the app's logo, typography, and bubble
styling. Fixed Home/Library navigation highlights the active destination and
preserves Home scroll position. Match Up remains a placeholder.

### Phase 2: feat What's That? game

What's That? provides ten randomized local rounds with two choices, explicit
correctness, automatic/replayable audio, forgiving wrong-answer feedback, and
bounded success animation that waits for the full pop sound. Completion shows
a pulsing gold trophy for at least three seconds and waits for the yay sound
before returning Home. Static content, session state, rendering, and audio remain
separate; leaving the game cancels its effects and playback.

## Verification and limits

Recorded checks include the debug build, six unit tests, device coverage for game
entry, Back, recreation, scroll restoration, completion audio, and cancellation,
and lint without errors. Broader device/API coverage remains unverified; glow
uses Android 12+ blur with sharp fallbacks on older APIs.

Library functionality is tracked in [Iteration 2](002-iteration.md).
Match Up gameplay is outside these verified phases. See [README](../../README.md)
for current behavior and [asset conventions](../content-assets.md) for storage.
