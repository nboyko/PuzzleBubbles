# Iteration 1: Home screen and game navigation

Status: **In progress**. Only the user's explicit confirmation closes this iteration.
This record has been reformatted; the current iteration has not been finalized.

## Initial planned scope

Create a child-friendly Home screen with two large image cards and direct
navigation to distinct game placeholders, with normal Back behavior.

The mechanic names are **Choose It** and **Matching**; their child-facing titles
are **What's That?** and **Match Up**. The initial Home milestone excludes game
mechanics, audio playback, matching interactions, scoring, progress, settings
behavior, Room, and networking.

## Recorded Home functionality

Home uses a local game catalog, stable cover asset IDs, full square artwork above
title bars, Baloo 2 headings, and Lilita One card titles. Its scrollable layout has
one column on narrow screens and two at widths of at least 700 dp. Navigation
uses separate `home`, `choose-it`, and `matching` destinations with Back to Home.

These are recorded implementation details, not a new finalization or acceptance
decision. Current behavior and architecture belong in [README.md](../../README.md);
asset storage belongs in [content-assets.md](../content-assets.md).

## Pending functionality: playable What's That?

The working checkout contains a playable implementation awaiting finalization
against an agreed [current plan](../plan.md) consistent with the
[MVP requirements](../mvp-spec.md). The plan's scope has not yet been selected;
the existing pending scope includes:

- Ten local target items, each appearing once in randomized rounds with two
  image choices and explicit correctness.
- Automatic and replayable content audio.
- Gentle wrong-answer feedback without advancing; correct-answer animation and
  success audio followed by the next round.
- A pulsing trophy after the final round and automatic return Home.
- Runtime image/audio resolution, session/engine tests, and navigation checks.

Match Up remains a placeholder. This pending functionality has not been declared
complete merely because code or individual checks exist.

## Remaining verification and acceptance

Explicit finalization must verify the current checkout against the agreed current
plan and reconcile these requirements. Earlier task results do not establish finalization.
Review small-screen discoverability, enlarged text, dark mode/system bars, audio,
rapid tapping, Back, all-round completion, and the timed return Home.
User acceptance and explicit iteration completion remain pending.

## Upcoming work

Keep any unfinished requirements identified during explicit finalization separate
from completed functionality and place them in the appropriate upcoming iteration.
No next iteration is currently defined; do not invent one or expand its scope here.
