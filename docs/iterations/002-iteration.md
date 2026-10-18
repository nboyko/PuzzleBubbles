# Iteration 2: Library functionality

Status: **In progress**. Phases 1 and 2 are verified; the full iteration is not finished
until the user explicitly confirms it.

## Verified phases

### Phase 1: feat library navigation and empty library screens

My Library has a title/Back header and large Photos/Sounds cards. Each opens a
separate empty screen with an icon, message, Back to My Library, and fixed
Home/Library navigation with Library selected. Home returns directly to Home;
Library returns to My Library without adding destinations. Back, Home scroll
restoration, and navigation across activity recreation are supported. The Photos
SVG resolves through the existing stable asset-ID rendering boundary.

Recorded verification: debug build, six unit tests, 15 device tests, and lint
passed with zero errors and 17 non-blocking warnings. Coverage includes media
navigation, Back, recreation, and existing game/audio behavior. Small-screen and
enlarged-text layouts were reviewed; broader device/API coverage is unverified.

### Phase 2: feat photo import and saved-photo library

Photos has an icon-only plus action and a persistent photo grid. Single-image
system selection opens a square Save/Cancel preview; processing respects EXIF
orientation and stores only a normalized 1024 × 1024 WebP with a generated ID
and Room metadata. Saved-photo Delete removes the Library copy; Save/X close
unchanged. The modal blocks background controls and system Back, preserves
state across rotation, and prevents duplicate operations. Unsaved previews are
discarded after process death. Serialized reconciliation recovers interrupted
saves/deletes. The affected Android 16 picker build uses a system document fallback.

Verified: debug build, six unit tests, 36 device tests, and lint (zero errors,
24 non-blocking warnings). Coverage includes crop/orientation, persistence,
failure recovery, rapid actions, recreation, navigation with saved content,
picker results, and existing games/audio. Manual checks covered picker import,
restart, deletion, unsaved-preview process death, small screens, and enlarged
text. Older-device picker fallback and actual disk-full operation remain
unverified manually; injected storage failures are covered by tests.

## Remaining work

Manual photo editing, Library audio, and custom-photo gameplay remain outside
the verified phases. No next feature is defined by this record; agree it in
[the plan](../plan.md) separately. Iteration closure requires explicit user confirmation.
