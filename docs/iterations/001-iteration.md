# Iteration 1: Home, What's That?, and Library shell

Status: **In progress**. The gameplay and Library shell steps are verified;
explicit user acceptance and iteration closure remain pending.

## Verified functionality

Home offers two game cards and fixed Home/Library bottom navigation with a
raised active bubble. What's That? has ten randomized local rounds, two choices,
explicit correctness, automatic/replayable audio, forgiving wrong feedback,
and bounded success animation that waits for the full pop sound. Completion
shows a pulsing gold trophy for at least three seconds and waits for the yay
sound before returning Home. Match Up remains a separate placeholder.

My Library has a title/Back header and large Photos/Sounds cards. Each opens its
own empty screen with an icon, message, Back to My Library, and fixed Home/Library
navigation with Library selected. Back, Home scroll restoration, and recreation
are supported.
There is no sample media, importing, Library playback, storage, or UID generation.
Static content, gameplay state, rendering, and audio remain separate.

## Verification and limits

Fresh debug build, six unit tests, 15 device tests, and lint passed with zero
errors and 17 non-blocking warnings. Device coverage includes navigation,
recreation, scroll restoration, game entry, completion audio, and cancellation.
Library and child layouts were checked at 320dp wide with 150% text. The Photos
SVG/vector and stable resource mapping were reviewed. Broader device/API coverage
remains unverified; glow uses Android 12+ blur with sharp fallbacks on older APIs.

See [the retained plan](../plan.md) for Library acceptance criteria,
[README](../../README.md) for current behavior/architecture, and
[asset conventions](../content-assets.md) for storage and rendering.

## Remaining and upcoming work

No required Library-shell functionality remains deferred. Manual iteration
acceptance is pending. Match Up gameplay and media management remain outside
this verified step; no next feature has been agreed. Keep upcoming requirements
separate and update the plan only after agreeing the next feature.
