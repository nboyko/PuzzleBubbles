# Iteration 2: Library functionality

Status: **In progress**. Phases 1–5 are verified; the full iteration is not finished
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

### Phase 3: feat record and manage Library sounds

Sounds has an Add Sound overlay with on-demand microphone permission, a visible
timer, manual Stop, and native five-second auto-stop. Valid AAC/MPEG-4 drafts can
be named, played/paused, cancelled, or explicitly saved. Selected `Track N`
defaults and custom/duplicate names survive rotation and foreground Save failures.
Saved rows show names, duration/progress, playback, and confirmed Delete. The modal
blocks background navigation; its controls remain reachable with the keyboard.

Stable IDs, private audio files, and Room metadata preserve Photos through additive
migrations. Saves/deletes and reconciliation recover interrupted operations without
duplicate entries. Rotation retains active work; true backgrounding discards
unsaved drafts, including after a failed pending Save. Process restart removes
abandoned drafts without recording or saving automatically. Recorder/UI limits
remain 5,000 ms, with the agreed 100 ms native-output tolerance and actual duration
preserved; output beyond 5,100 ms is rejected. Backup/transfer exclusions cover
recordings and metadata together.

Verified: debug build, 14 unit tests, and lint (zero errors, 18 warnings). The full
physical Android 10 run had 72 passes and one pre-existing Android 16-only picker
assumption reported as a failure; the test is unchanged from baseline and cannot
run on this device. All 14 Sounds screen tests passed again after the final
lifecycle and keyboard fixes. Native microphone/codec tests cover manual/immediate
Stop and maximum duration; storage tests cover output tolerance, migration,
persistence, and failure recovery. Existing Photos/game regressions passed.

Phone checks covered permission denial/grant, auto-stop, naming, preview playback,
Save/restart/Delete, cancellation, rotation, keyboard layout, background cleanup,
and process-restart recovery. Representative screen states were rendered and
inspected on-device; Compose preview definitions compiled but were not rendered
in the IDE. Broader device coverage, subjective speech quality, and actual disk-full
operation remain unverified; injected storage failures are automated.

### Phase 4: feat forgiving image selection

What's That? accepts a swipe starting on either image bubble in any direction,
including movement outside it. Each gesture selects its starting image once,
using existing correct/incorrect feedback. Taps and accessible clicks remain
available. Crossing another image, swiping during feedback, or holding a finger
into the next round cannot submit extra answers; a cancelled touch below the
movement threshold selects nothing.

Verified: debug build, 14 unit tests, 21 emulator tests (seven new gesture tests
plus game audio/navigation regressions), and lint with zero errors and 18 warnings.
Emulator input and rendered screenshots confirmed correct/incorrect swipes,
retry, round progression, Replay, and Back. Existing round/feedback previews
compiled; they were not rendered in the IDE. Physical-phone verification of this
gesture change remains unverified because the connected phone was locked.

### Phase 5: feat Sounds empty-state recording action

Empty Sounds replaces its message with an Add Record button below the sound
icon, following Photos. The header plus remains; both open the same idle modal
without requesting permission or recording. They share existing action guards.
The extra button disappears with saved content and returns after the last
deletion. The icon shrinks in limited vertical space to keep the button visible.

Verified: debug build, 14 unit tests, 33 initial Sounds/Photos/navigation emulator
tests, and all 16 Sounds tests after the landscape fix. Lint has zero errors and
18 warnings. Portrait/landscape screens were rendered and inspected on the emulator,
and Add Record opened the idle modal. Existing previews compiled; IDE rendering
and physical-device verification of this change remain unverified.
