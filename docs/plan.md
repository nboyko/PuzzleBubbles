# Plan 004 — Record and Manage Library Sounds

Status: **Verified and finalized** as Phase 3 of the still-open Iteration 2.
The requirements and acceptance criteria below are retained for this completed
feature, including the agreed 100 ms native-output tolerance. Iteration closure
remains manual; the next feature scope has not yet been agreed.

Verified outcome: parents can record, preview, name, save, play/pause, and delete
private Library sounds. Rotation, foreground/background cleanup, interrupted
storage recovery, and additive migrations are covered. The keyboard remains
usable on the physical Android 10 device. Build and 14 unit tests passed; lint
has zero errors and 18 warnings. The full device run had 72 passes and the
unchanged Android 16-only picker assumption reported as a failure on Android 10;
the final 14 Sounds screen tests passed after the lifecycle/keyboard fixes.
See [Iteration 2](iterations/002-iteration.md) for rendering and verification limits.

## Goal

Allow a parent to create short audio recordings directly inside Puzzle Bubbles and manage the saved recordings from the **Sounds** library.

This feature phase covers the complete flow:

```text
Sounds
   ↓
+ Add Sound
   ↓
Recording modal
   ↓
Record
   ↓
recording for max 5 seconds
   ↓
Preview
   ↓
Save
   ↓
saved sound appears in Sounds list
   ↓
Play / Delete
```

This feature phase supports **recording only**.

Importing an existing audio file from the device is deferred to a later feature.

---

## 1. Sounds Screen

Keep the existing Sounds library screen.

Add a **plus / Add Sound** button in the top-right area of the header, following the same pattern as the Photos screen.

Conceptually:

```text
←    [icon] Sounds                  +
```

Tapping `+` opens the recording experience. Preserve the fixed Home/Library bottom
navigation on Sounds, with Library selected. The modal covers and blocks the
underlying screen and navigation; do not add another navigation destination.

Keep recording in an overlay on the existing Sounds route, without a separate
full-screen destination.

The recording experience should visually behave like the existing photo preview/editor:

- displayed as a modal/overlay;
- focused on one temporary task;
- background content remains visually behind it;
- modal can be closed with an `X`.

---

## 2. Recording Modal

Initial state:

```text
┌─────────────────────────────┐
│                         ✕   │
│                             │
│        Record Sound         │
│                             │
│           0:00              │
│                             │
│        [ ● Record ]         │
│                             │
└─────────────────────────────┘
```

The exact styling should follow the existing Puzzle Bubbles visual system.

The modal has several explicit states:

```text
IDLE
  ↓
RECORDING
  ↓
PREVIEW
  ↓
SAVED / CANCELLED
```

Do not mix recording and playback states implicitly.

---

## 3. Microphone Permission

Do not request microphone permission:

- when the application starts;
- when Library opens;
- when Sounds opens;
- when the recording modal opens.

Request `RECORD_AUDIO` only when the parent presses **Record**.

Flow:

```text
tap Record
    ↓
permission already granted?
    ├── yes → start recording
    └── no  → request microphone permission
                    ↓
                granted?
                ├── yes → start recording
                └── no  → stay idle
```

If permission is denied:

- recording must not start;
- keep the recording modal usable;
- provide clear, non-technical feedback.

For example:

```text
Microphone access is needed to record a sound.
```

Do not crash or close the modal.

If Android no longer allows the permission prompt to be displayed, provide a reasonable route to the application's system settings.

---

## 4. Recording State

Once recording starts, the modal changes to the recording state.

Conceptually:

```text
┌─────────────────────────────┐
│                         ✕   │
│                             │
│        Recording…           │
│                             │
│         0:03 / 0:05         │
│                             │
│         [ ■ Stop ]          │
│                             │
└─────────────────────────────┘
```

Show a clearly visible elapsed timer.

The maximum recording duration is:

```text
5 seconds
```

The recording stops in either of two ways:

### Manual stop

The parent presses **Stop** before five seconds.

### Automatic stop

The recording reaches five seconds.

The UI should then automatically move to Preview.

The implementation should use the recorder's maximum-duration capability as a hard recording limit and correctly handle its asynchronous completion behavior.

Keep the recorder maximum and visible timer at 5,000 ms. Allow up to 100 ms of
native AAC/container output overhead: the actual validated file duration must
be positive and at most 5,100 ms. Preserve that actual duration in metadata and
playback; do not clamp it or trim/transcode the audio. Reject longer output.

---

## 5. Temporary Recording

While recording and previewing, the audio is temporary.

Conceptually:

```text
temporary recording
      ↓ Save
permanent library file
```

Do not create a permanent library database entry immediately when recording starts.

If the parent:

- closes the modal;
- presses Cancel;
- leaves before saving;
- encounters an unrecoverable recording error;

delete the temporary recording file.

Temporary recordings must not accumulate in application storage.

---

## 6. Audio Format

Record locally using:

```text
container: MPEG-4
codec: AAC
extension: .m4a
```

Optimize settings for short spoken audio rather than music-quality recording.

Mono audio is sufficient.

Do not store raw PCM/WAV files for user recordings.

The exact bitrate/sample-rate settings may follow Android/device capabilities, but they should produce clear speech without unnecessarily large files.

Saved recordings should live in app-owned storage.

Conceptually:

```text
library/
├── images/
│   └── ...
│
└── audio/
    ├── <audio-id>.m4a
    ├── <audio-id>.m4a
    └── ...
```

Use stable generated IDs rather than names supplied by the recorder or temporary filesystem.

---

## 7. Recording Preview

After manual or automatic stopping has finalized a valid recording, transition
to Preview. Invalid or failed output returns to a usable idle/error state, never
a playable or saveable preview.

Conceptually:

```text
┌─────────────────────────────┐
│                         ✕   │
│                             │
│        Your Recording       │
│                             │
│   ▶ ─────────────── 0:03    │
│                             │
│     Cancel        Save      │
│                             │
└─────────────────────────────┘
```

The parent can listen to the complete temporary recording before deciding whether to keep it.

Show a single-line Name field in Preview, prefilled with `Track N`, where N is
the current number of saved recordings plus one. Initially focus the field and
select its text so typing replaces the default. Names need not be unique; deleting
recordings may reuse a default number. Preserve the entered name and selection
through rotation, and the name through Save failures. Trim surrounding whitespace
on Save; a blank name uses the original default for that draft. Save is explicit
and the field is disabled while saving. Keep controls reachable with the keyboard.

### Playback control

Show:

- Play / Pause control;
- simple playback progress track;
- duration.

Do **not** implement a waveform.

The progress track represents playback position only:

```text
▶  ━━━━━━━●─────────  0:03
```

It does not need to display audio amplitude.

---

## 8. Preview Playback

Pressing Play:

- starts playback from the current position;
- changes Play to Pause while playing;
- updates the progress indicator.

When playback reaches the end:

- stop playback;
- reset the control to Play;
- reset playback position to zero, ready for another Play.

Only one instance of the recording may play at a time.

Repeated taps must not layer multiple playback instances.

If the parent closes the modal during playback:

- stop playback;
- release playback resources;
- delete the temporary recording unless it has already been saved.

---

## 9. Preview Actions

Once recording exists, the original Record action is replaced by:

```text
Cancel     Save
```

### Cancel

Cancel:

1. stops preview playback if active;
2. deletes the temporary recording;
3. closes the modal;
4. leaves the Sounds library unchanged.

The `X`, system Back, and a backdrop tap have the same cancel result in any
non-committing modal state, including stopping/discarding an active recording.
Taps inside the panel do not dismiss it. During Save/Delete, block conflicting
actions and dismissal until the operation finishes.

### Save

Save:

1. stops playback if necessary;
2. creates a stable library sound ID;
3. moves or safely copies the temporary file into permanent app-owned audio storage;
4. persists its metadata;
5. closes the modal;
6. immediately shows the saved recording in the Sounds list.

Show lightweight confirmation if appropriate:

```text
Sound saved
```

Do not show a blocking success dialog.

---

## 10. Sound Metadata

Persist only the metadata required by the library.

Conceptually:

```kotlin
LibrarySound(
    id: String,
    assetId: String,
    name: String,
    durationMs: Long,
    createdAt: Long
)
```

Follow the Photos metadata boundary: generate an app-owned ID and expose the
stable rendering/playback ID `library-sound:<id>`, name, duration, and creation time to
Compose through a repository. Keep filenames, filesystem paths, Android resource
IDs, and Room entities out of screen/domain models. The persistence entity stores
ID, internal `<id>.m4a` filename, name, actual validated duration, and creation time.

Extend the existing Room database with a sounds table and an additive migration
that preserves existing Photos records. Do not rename the database, use destructive
migration, or reorganize unrelated photo code. Show sounds newest first, matching
Photos; do not add sample entries or require user-supplied names. Use meaningful
accessible labels such as "Recording 1, 3 seconds", with stable IDs for identity.
Add a non-destructive migration for already-saved unnamed recordings, assigning
`Track 1`, `Track 2`, etc. in oldest-first order without changing IDs or audio files.

Do not store binary audio inside Room.

Do not store:

- waveform data;
- transcript;
- category;
- game association.

The audio file itself belongs in app-owned filesystem storage.

---

## 11. Saved Sounds List

Saved recordings appear on the existing Sounds screen.

Each row should contain:

```text
┌──────────────────────────────────┐
│ ▶   ────────────────  0:03    🗑 │
└──────────────────────────────────┘
```

Specifically:

- saved name;
- Play/Pause button at the start;
- playback progress track;
- duration;
- Delete button at the end.

A waveform is not required.

Names are editable only before the initial Save. Saved recordings have no Rename
or Edit action in this phase. Names never determine identity or filesystem paths.

---

## 12. Playing Saved Sounds

Tapping Play on a saved recording:

1. begins playback;
2. changes its Play icon to Pause;
3. updates its progress track;
4. stops/reset appropriately when playback completes.

Only one sound may play at a time.

If another sound is already playing:

```text
Sound A playing
      ↓
tap Sound B
      ↓
stop Sound A
      ↓
play Sound B
```

Do not allow multiple recordings to play simultaneously.

Leaving the Sounds screen should stop active playback and release playback resources.

---

## 13. Playback Progress

The progress UI is intentionally simple.

Use a normal progress line:

```text
▶  ━━━━━●────────────  0:02 / 0:04
```

This is a **playback progress indicator**, not an audio waveform.

For this feature phase it does not need to be seekable.

The parent only needs to see:

- whether audio is playing;
- how far through the recording playback has progressed;
- the recording duration.

Seeking can be added later if there is an actual need.

---

## 14. Delete Saved Sound

Each saved sound row includes a Delete action.

When Delete is confirmed:

1. stop playback if that sound is currently playing;
2. delete its persistent metadata transactionally;
3. delete the app-owned audio file;
4. immediately remove the row from the Sounds list.

Use a lightweight confirmation before permanently deleting the recording.

For example:

```text
Delete this sound?

Cancel    Delete
```

This avoids accidental deletion because there is no recovery/undo mechanism yet.

Deletion must clean up both:

```text
database metadata
+
audio file
```

Follow the Photos deletion/recovery approach. A failed file removal must not
resurrect the deleted item; reconciliation retries cleanup of the unreferenced
owned file. A metadata failure preserves the existing item and file. Keep a
friendly error and explicit retry available; never delete unrelated files.

---

## 15. Existing Sounds

There is no editor/details screen for an existing sound in this feature phase.

Tapping or interacting with an existing recording only supports the actions exposed directly in its row:

```text
Play / Pause
Delete
```

Do not add:

- opening the sound in a modal;
- re-recording;
- replacing;
- renaming;
- trimming;
- editing.

---

## 16. Recording Lifecycle

Recording is allowed only while the app is actively in the foreground.

- True backgrounding (Home, locking, or switching apps) stops recording/playback,
  releases resources, discards any unsaved recording or preview, and closes the
  modal. Returning to Sounds does not restart recording or playback.
- Rotation and ordinary activity recreation are not abandonment: retain the modal,
  temporary recording/preview, selected state, and any in-progress operation. An
  active recording continues only within its original five-second limit; rotation
  must not restart its timer or create another recorder. No automatic Save.
- Process death discards unsaved work. Reopen Sounds without a restored draft,
  automatic recording, playback, or Save; clean interrupted temporary files before
  exposing persisted content. Already committed items remain saved.
- System permission UI is not a cancellation by itself. A permission result may
  start recording only for the still-open requesting modal, the current Record
  request, and an active foreground screen. Ignore stale results after cancellation
  or genuine backgrounding; returning from Settings requires another Record tap.
- Once an explicit Save/Delete is committing, let that single bounded operation
  finish safely even if the app backgrounds. Never discard a committed file as if
  it were an unsaved draft. Reconcile interruptions on next load; do not auto-retry.

No foreground service is needed. Release `MediaRecorder` resources when recording
finishes or is abandoned. Unfinished previews remain temporary until explicit Save;
auto-stop and lifecycle restoration never persist them automatically.

---

## 17. Error Handling

Handle recording failures gracefully.

Possible cases include:

- microphone unavailable;
- permission denied;
- recorder initialization failure;
- recorder stop failure;
- temporary-file creation failure;
- storage write failure;
- playback failure.

Use parent-friendly feedback.

Example:

```text
Couldn't record this sound.
Please try again.
```

Do not expose stack traces, codec errors, file paths, or Android implementation details.

A failed recording must not create a broken library item.

---

## 18. State Model

The modal should have explicit state.

Conceptually:

```text
IDLE
  ↓ Record
RECORDING
  ├── Stop
  └── 5 second limit
         ↓
PREVIEW
  ├── Play / Pause
  ├── Cancel
  └── Save
         ↓
SAVED
```

Possible failure transitions should return to a usable state rather than leaving recording controls stuck.

Do not derive recorder state only from visible buttons. Represent pending
permission, starting, stopping/finalizing, saving, and deleting explicitly enough
to guard transitions; the Kotlin representation need not mirror every diagram.

Acquire action guards synchronously before launching asynchronous work. Disable
and ignore duplicate Record, Stop, Save, Delete, permission requests, and conflicting
close/navigation actions. Manual Stop, the maximum-duration callback, and recorder
errors must converge on one finalization/cleanup path for that recorder session.
Ignore callbacks from cancelled or replaced sessions. Do not impose a blanket delay
on Play/Pause: serialize those transitions and maintain one playback instance.

Configure the recorder maximum to 5,000 ms, not just a UI timer. Treat the maximum-
duration callback as asynchronous stopping, not proof that the file is ready.
Finalize/validate the playable AAC/MPEG-4 file and read its actual duration before
Preview or Save. Immediate Stop may yield no valid data: delete that output and
return to idle with a friendly retry message. Do not add trimming or conversion.

---

## 19. Reuse Existing Audio Infrastructure

Before adding new playback code, inspect the existing What's That? audio implementation.

If there is already a suitable audio playback abstraction, reuse or extend it where appropriate.

Do not create two unrelated playback systems if the existing one can safely support local library files.

Recording-specific behavior remains separate from playback. The current player
supports bundled resources and has no pause/progress API. Reuse useful player
mechanics while adding the minimal local-asset resolution, pause/resume, duration,
progress, and completion/error handling required here. Do not leak paths to screens
or change the existing game playback contract unnecessarily.

Pause preserves position; Play resumes it. Completion resets to zero. Starting
another saved row resets/stops the previous row. Opening the recording modal stops
saved-row playback; recording and preview/list playback never overlap. Leaving or
backgrounding Sounds stops playback and releases resources. Handle audio-focus
loss/interruption by stopping or pausing safely; do not resume automatically.

---

## 20. Explicitly Out of Scope

Do not implement in this feature phase:

- selecting an existing audio file from the device;
- audio trimming;
- waveform visualization;
- waveform generation;
- seeking;
- renaming saved sounds;
- editing an existing recording;
- replacing an existing recording;
- re-recording;
- sound categories;
- transcription;
- noise reduction;
- filters;
- cloud upload;
- backend synchronization;
- associating sounds with custom games.

---

## Persistence, Documentation, and Verification

File writes and Room transactions are not atomic together. Stage a valid recording,
then publish its permanent file and metadata through serialized Save/reconciliation.
On ordinary failure, remove incomplete output and retain a valid temporary preview
for explicit retry where possible. Reconcile interrupted saves before listing items:
preserve committed valid files; clean temporary/orphaned files and invalid metadata.
Never duplicate entries or automatically retry interrupted mutations.

Store recordings under `filesDir/library/audio/<id>.m4a`, separate from bundled
assets. Temporary files stay in an app-owned draft/staging location and are cleaned
on cancel, failure, and startup recovery, without deleting a live draft on rotation.
Verify cloud-backup and device-transfer rules exclude audio files and their Room
metadata together, following Photos. Bundled source/runtime conventions do not change.

The following documentation and configuration work is required in this current
sound-recording feature phase of Iteration 2, not deferred to another phase:

- Extend `docs/mvp-spec.md` with parent-recorded audio and the Sounds modal flow.
- Extend `docs/content-assets.md` with private user-recording storage at
  `filesDir/library/audio/<id>.m4a`, stable IDs, metadata, and recovery conventions.
  Preserve all bundled audio source/runtime conventions.
- Verify and update backup rules where needed so private recording files and
  their metadata are excluded together from cloud backup and device transfer;
  document those exclusions. Existing exclusions may already cover the paths.
- Document that fixed Home/Library navigation remains visible on Sounds and is
  covered and non-interactive underneath the recording modal.

Permanent docs and README must not claim recording exists before it is verified.
Reconcile the phase only on explicit `$finalize-feature`; do not open a new
iteration or close Iteration 2 automatically.

Add focused verification using existing tooling:

- State/clock tests for permission handling, stale callbacks, manual/automatic Stop,
  one finalization per session, short/invalid recordings, and the five-second limit.
- Rotation preserves recording deadline, preview, and mutations; backgrounding and
  process death discard unsaved work without saving or restarting anything.
- Rapid and conflicting actions cannot create duplicate recorders, players, saves,
  deletion requests, or navigation. X/Back/backdrop and busy guards behave consistently.
- Save/delete failures, file/metadata mismatches, and process interruption recover
  idempotently; migration preserves existing Photos; restart retains valid sounds.
- Pause/resume, progress, completion reset, row switching, modal opening, and leaving
  the screen maintain one player and release resources correctly.
- Empty/list states, permission denial/settings route, confirmation, retry messages,
  and representative Compose previews use no real microphone/storage in previews.
- Build, unit tests, lint, affected device tests, and Photos/game regressions.

Manually verify microphone recording, immediate Stop, five-second auto-stop,
permission denial, rotation/backgrounding, playback, Save/Delete, and restart on a
physical device. Mocks/emulator UI checks do not establish real microphone or codec
behavior. Report unavailable checks accurately and preserve known prior test failures
for investigation rather than treating them as passed.

---
## 21. Acceptance Criteria

The feature is complete when:

1. Sounds has a `+` / Add Sound action in its header.
2. Pressing Add Sound opens the recording modal.
3. Opening the modal itself does not request microphone permission.
4. Pressing Record requests microphone permission when necessary.
5. Permission denial does not crash or leave the modal unusable.
6. Granting permission starts recording.
7. Recording state visibly indicates that the microphone is active.
8. A visible timer updates while recording.
9. Recording can be stopped manually.
10. Recording automatically stops at five seconds.
11. The recorder and UI deadline remain 5,000 ms; finalized output allows at most 100 ms of native overhead (5,100 ms total), with actual duration preserved and longer output rejected.
12. Recording creates only a temporary file until Save.
13. After stopping, the modal enters Preview state.
14. The temporary recording can be played and paused.
15. Preview shows a playback progress line.
16. Preview shows recording duration.
17. No waveform is generated.
18. Cancel deletes the temporary recording and closes the modal.
19. `X` behaves as Cancel.
20. Save persists the recording as an app-owned audio file.
21. Saved audio uses MPEG-4/AAC (`.m4a`).
22. Binary audio is not stored in Room.
23. Saved metadata survives application restart.
24. The new recording appears immediately in the Sounds list.
25. Each saved recording can be played from its list row.
26. Playback progress is shown while a saved sound plays.
27. Only one sound can play at a time.
28. Leaving Sounds stops active playback.
29. Each recording has a Delete action.
30. Delete removes both the audio file and its persistent metadata.
31. Existing recordings do not have an Edit action.
32. Closing/abandoning an unsaved recording leaves no temporary files behind.
33. Recorder and player resources are released correctly.
34. Recording failures do not create invalid library entries.
35. Existing Photos and game functionality remain unaffected.
36. The project builds and relevant tests pass.
37. Rotation preserves active work/deadline; backgrounding discards unsaved work and process death restores Sounds without a draft or automatic action.
38. Rapid/conflicting taps and stale callbacks cannot duplicate recorder/player instances or persistent operations.
39. Save/delete recovery and the additive Room migration preserve valid content, including existing Photos.
40. Fixed Home/Library navigation stays visible on Sounds, with Library selected; the modal covers and blocks it. Back/backdrop match X outside committing operations.
41. Playback completion resets to zero, and recording never overlaps preview or saved-row playback.
42. This phase includes scoped MVP/asset documentation for private recordings and verified backup exclusions for files and metadata, preserving bundled conventions. Meaningful previews accompany the feature; Iteration 2 remains open until explicitly finished.
43. Preview offers a selected `Track N` name default based on saved count + 1; custom and duplicate names persist and appear in rows. Blank input falls back to the draft's default, and names survive rotation and failed Saves.
44. Existing unnamed recordings receive default names through a migration preserving Photos, sound IDs, metadata, and audio files. Saved recordings cannot be renamed yet.

---

## Expected Result of This Feature Phase

A parent can complete the entire recording workflow:

```text
Sounds
   ↓
+
   ↓
Record
   ↓
up to 5 seconds
   ↓
Stop / automatic stop
   ↓
Play preview
   ↓
Save
   ↓
▶ ───────────── duration  🗑
```

Sounds contains persistent parent-created recordings. Associating them with
custom games remains outside this feature.
