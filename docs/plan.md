# Plan 002 — Add Photo to Library

Status: **Verified — Iteration 2, Phase 2**. The full iteration remains in progress
until the user explicitly finishes it. The agreed requirements below are retained
with this feature; replace this plan only when the next feature is agreed.

## Verified outcome

Single-image selection opens an explicit Save/Cancel preview, respects EXIF
orientation, and saves a centered 1024 × 1024 WebP with generated IDs and Room
metadata. Photos displays saved images; Delete removes only the Library copy,
while saved-photo Save/X close unchanged. Rotation, rapid actions, persistence,
and interrupted-operation reconciliation are covered.

Follow-up decisions: Add Photo is an icon-only plus on the right of the header.
The affected Android 16 Google picker build uses the system document picker
because Preview → Select returns cancellation without an image; other builds
retain the AndroidX picker flow. See [README](../README.md) for the exact workaround.

Verification: debug build, six unit tests, 36 device tests, and lint passed with
zero errors and 24 non-blocking warnings. Manual checks covered native picker
selection, the compatibility fallback, save/delete, restart, unsaved-preview
process death, and small-screen/enlarged-text layout. Older-device fallback and
actual disk-full behavior remain unverified manually; injected write/rename,
metadata, and cleanup failures are tested. No critical blockers remain.

## Goal

Allow a parent to add one photo from the Android device to the Puzzle Bubbles photo library.

The selected image is:

1. selected using the Android system Photo Picker;
2. shown on a dedicated preview screen;
3. automatically center-cropped to a square;
4. normalized to **1024 × 1024**;
5. saved into Puzzle Bubbles app-owned storage;
6. displayed in the Photos library.

This iteration does **not** include manual crop, pan, or zoom controls.

---

## 1. Photos Screen Header

Update the existing Photos screen header.

It should contain:

```text
←    Photos                 Add Photo
```

or the equivalent layout matching the current Puzzle Bubbles design.

Keep the existing Back arrow and Photos title; no parrot or new mascot is required.

Add a clearly visible **Add Photo** button near the title area.

The button should use the established Puzzle Bubbles button style rather than introducing a new control style.

The Add Photo button is available from the normal Photos library screen.

---

## 2. Launch Android Photo Picker

When **Add Photo** is pressed:

- launch the Android system Photo Picker;
- allow image selection only;
- allow selection of exactly **one image**.

Use Android's single-item `PickVisualMedia` flow.

If Photo Picker is unavailable, the contract's automatic system document-picker
fallback is supported and must still select exactly one image. No custom
gallery or broad storage permission is needed. See the
[Android Photo Picker documentation](https://developer.android.com/training/data-storage/shared/photo-picker).

Do not:

- build a custom gallery browser;
- request broad photo/storage permission;
- allow multiple selection in this iteration.

If the user closes the picker without selecting anything, remain on the Photos screen and make no changes.

---

## 3. Selected Photo Processing

After the user selects an image, do not save the original photo directly into the library.

Read enough information to:

- verify that the selected content is a valid supported image;
- preserve the source image's intended visual orientation, including EXIF rotation/mirroring;
- determine its dimensions;
- prepare it safely for preview and normalization.

Avoid unnecessarily decoding a very large camera image at its full resolution when a smaller sampled decode is sufficient.

Normal modern phone photos should not be rejected simply because the original file is large.

The important size constraint applies to the **stored Puzzle Bubbles image**, not the source image.

---

## 4. Photo Modal

After a valid image is selected, present a full-screen modal/task overlay above
Photos. It covers the Photos content and bottom navigation and prevents interaction
with both. Reuse the existing app styling; do not introduce another navigation bar.

For an unsaved selection, show **Add Photo**, a large square preview, and only
**Cancel** and **Save**. There is no Delete action for an unsaved photo.

Tapping a saved photo in the grid opens the saved-photo modal with **Delete**,
**Save**, and a top-right **X** close control. There is no bottom Cancel action
in this state. X closes the modal without changing the saved photo.
No modal Back arrow, Home shortcut, or background media actions are available.
Outside taps and system Back do not dismiss the modal; dismissal is explicit.

The preview represents the exact centered square crop that will be saved, after
respecting the source's orientation. Landscape images lose equal left/right
margins; portrait images lose equal top/bottom margins. No pan, zoom, or manual
rotation is available. Small images are still normalized to exactly 1024 x 1024.
Use the same prepared normalized image for preview and Save, avoiding a second
source read that could produce a different result. No original is retained.

Preserve the selected preview and any in-progress operation across activity
rotation. Rotation must not reset the selection, save automatically, or start
another operation. Keep bitmap work off the main thread and out of saved-state
bundles.

If Android kills the process during an unsaved preview, reopen Photos without
restoring or saving that preview. Do not automatically retry an interrupted Save.
Previously committed photos remain available; reconcile incomplete writes first.

---
## 5. Modal Controls

### New, unsaved photo

- **Cancel** discards the unsaved selection and returns to Photos without saving.
- **Save** accepts the displayed center crop and explicitly starts normalization
  and persistence. Nothing is saved merely by opening or rotating the preview.
- **Delete** is not shown.

### Existing, saved photo

- Tapping a Library grid photo opens its saved-photo modal using the stable ID.
- **Delete** actually deletes that saved Library photo and its app-owned image.
  It never deletes the original gallery photo or bundled game artwork. On success,
  close the modal and remove the item immediately; restore the empty placeholder
  when the last photo is deleted.
- **Save** closes the modal and returns to Photos without rewriting the image,
  changing metadata/ID, or creating a duplicate. There are no edits to persist
  in this phase. Future editor functionality will use this same Save action to
  persist explicit edits; replacement selection/editing is not implemented now.
- A large, accessible top-right **X** closes the modal without saving or deleting.
  It is the cancel/close control for saved photos; do not add a bottom Cancel.

Allow at most one operation at a time. Lock conflicting controls immediately when
Save/Delete/Cancel/X is accepted; guard handlers as well as disabling
controls. Repeated taps must not create duplicate operations or navigation events.
Keep operation state during rotation. After a failed Save/Delete, remain in the
modal, show a friendly error, and allow an explicit retry once cleanup completes.
Closing the modal never triggers an automatic save, update, or delete.

Do not implement dragging, pinch zoom, crop handles, manual rotation, or filters.
Manual editing can extend this modal in a separately agreed future phase.

---
## 6. Saved Image Format

When Save is pressed:

1. crop the source to the centered square represented by the preview;
2. scale the result to exactly:

```text
1024 × 1024
```

3. encode the result as WebP;
4. save only this normalized result.

Do **not** keep the original imported image.

The resulting library should therefore contain predictable, reasonably small images regardless of the original camera resolution.

Conceptually:

```text
source:
4032 × 3024 JPEG
        ↓
center crop
        ↓
resize
        ↓
1024 × 1024 WebP
        ↓
Puzzle Bubbles library
```

Use reasonable lossy image quality appropriate for a children's game/photo thumbnail use case.

A quality around **80–85** is a sensible target unless testing shows a reason to adjust it.

---

## 7. Local File Storage

Store processed images in app-owned persistent storage.

Use internal persistent storage, `filesDir/library/images/`, for example:

```text
library/
└── images/
    ├── <photo-id>.webp
    ├── <photo-id>.webp
    └── ...
```

Use generated stable IDs rather than names derived from the original gallery filename.

Do not expose filesystem paths directly to presentation/game logic.

Android app-specific storage does not require additional storage permissions and is suitable for files that belong to the application.

---

## 8. Photo Metadata

Introduce the minimum persistent metadata required for the Photos library.

Room is appropriate now because the feature contains real persistent structured data and later library features will also need persistence. Android recommends Room over working directly with SQLite.

Keep the first model small.

Conceptually:

```kotlin
PhotoEntity(
    id: String,
    fileName: String,
    createdAt: Long
)
```

The exact schema may follow existing project conventions.

Width and height do not need to be stored if every library image is guaranteed to be 1024 × 1024.

Do not store:

- the bitmap itself;
- the original gallery URI as the canonical image;
- the original image;
- category;
- tags;
- game associations.

Binary image data belongs in the filesystem, not Room.

---

## 9. Save Transaction / Failure Handling

A photo should only appear in the library after both:

- the normalized file was written successfully;
- its metadata was persisted successfully.

Avoid leaving orphan files or database records when part of the operation fails.

### File/database consistency

A filesystem write and a Room transaction are not one atomic operation. Use a
small, serialized save pipeline rather than claiming cross-storage atomicity:

1. Allocate one stable photo ID for the accepted Save operation.
2. Encode to a temporary file under the private Library image directory, close
   it successfully, then rename it to `<photo-id>.webp` on the same filesystem.
3. Insert metadata in a Room transaction only after the final image exists.
   Publish the new photo only after that transaction commits.
4. On ordinary failure, remove this operation's temporary file and any final file
   that has no committed metadata. Never delete an already committed photo during
   retry or cleanup. Re-enable actions only after cleanup is complete.
5. Before loading/exposing Library content after startup, run reconciliation
   serialized with saves: remove stale staging files and unreferenced final files;
   remove metadata for missing/unreadable files so broken items are not displayed.
   Restrict cleanup to the owned Library directory. If reconciliation fails,
   report a recoverable error rather than exposing inconsistent data.

A crash after metadata commits preserves the photo. A crash before commit leaves
no visible photo after reconciliation. Do not add a background worker framework
or a generic transaction system for this one operation.


### Saved-photo deletion consistency

Serialize deletion with saves and startup reconciliation. First remove the photo
metadata in a Room transaction, then remove its app-owned file. If the database
transaction fails, leave the file/photo intact and report the failure. If file
removal fails after metadata commits or the process stops between these steps,
the file is unreferenced and startup reconciliation retries cleanup; never
restore a deleted entry just because its file remains. Restrict file removal to
the selected stable ID inside the owned image directory. Do not delete source
URIs, bundled artwork, or unrelated files. Repeated deletion is safe and cannot
remove another photo.
Examples:

### Processing failure

```text
Couldn't add this photo.
Try another image.
```

### Storage failure

```text
Couldn't save this photo.
Check available device storage and try again.
```

Do not display stack traces, MIME details, URI information, or technical Android errors to the parent.

---

## 10. Successful Save

After Save completes successfully:

1. return to the Photos screen;
2. display the new image in a photo grid introduced by this feature;
3. make it immediately visible without restarting the app;
4. optionally show lightweight confirmation:

```text
Photo added
```

Use a Snackbar or equivalent non-blocking feedback rather than a confirmation dialog.

---

## 11. Persistence

Saved photos must survive:

- navigation away from Photos;
- switching between Home and Library;
- app restart;
- process recreation.

On startup/re-entry, the Photos screen should load the persisted photo metadata and resolve the corresponding app-owned files.

The current Photos screen is empty: there are no sample photos or existing grid. Introduce the real photo grid and retain the empty placeholder when no records exist.

Do not add sample content or seed user-photo records from bundled game artwork.

---

## 12. Image Validation

Do not impose a small source-file limit such as:

```text
maximum 5 MB
```

because the source is normalized before storage.

Instead validate that:

- selected content can actually be decoded as an image;
- dimensions are valid;
- orientation can be handled;
- processing succeeds;
- enough storage is available for the normalized output.

Protect against corrupt or unreasonable input without rejecting normal high-resolution smartphone photos.

---

## 13. Memory Safety

Do not assume that the source photo is small.

A source image may be:

```text
4000 × 3000
6000 × 4000
or larger
```

Do not keep an unnecessarily full-resolution bitmap in memory.

The implementation should use an appropriately sampled decode / image-loading strategy for preview and final 1024 × 1024 generation.

The goal is:

```text
inspect source dimensions
        ↓
decode at appropriate scale
        ↓
apply orientation
        ↓
center crop
        ↓
render 1024 × 1024
```

rather than blindly decoding enormous images first.

---

## 14. Navigation

Expected flow:

```text
Library
   ↓
Photos
   ↓ Add Photo
System Photo Picker
   ↓ select
Photo Preview
   ├── Cancel → Photos
   └── Save   → Photos + new image
```

Both modal states require explicit actions. Unsaved photos use Save/Cancel; saved photos use Delete/Save and the top-right X. System Back and outside taps do nothing while either modal is open.

The bottom navigation remains hidden on the preview screen.

Back from Photos still returns to Library according to the established navigation design.

Photos retains the fixed Home/Library bottom navigation with Library selected. Home returns directly to Home; Library returns to My Library. The modal covers and disables these controls until it closes.

---

## 15. Architecture

This feature introduces the first real user-managed media persistence.

Keep responsibilities separated:

```text
presentation
    Photos screen
    Preview screen

domain/application
    Add photo operation

data
    photo metadata persistence
    app-owned image files
    image processing
```

Do not put file-writing and bitmap-processing logic directly into Compose UI.

Do not introduce a generic media framework merely because audio will be added later.

Create reusable abstractions only where this feature already needs them.

---

## 16. No External Crop Library Yet

Do not add uCrop or another cropping dependency in this iteration.

The crop behavior is currently deterministic:

```text
center square
→ resize to 1024 × 1024
```

This can be implemented with normal Android image processing.

A crop library or custom Compose crop editor should be evaluated in the next iteration when manual pan/zoom editing is introduced.

---

## 17. Explicitly Deferred

Do not implement yet:

- multiple photo selection;
- manual cropping;
- drag/pan;
- pinch zoom;
- rotation;
- image filters;
- retaining original images;
- manual editing or replacement of existing photos;
- photo labels;
- categories;
- search;
- cloud backup/sync;
- backend;
- using custom photos inside games.

---

## 18. Acceptance Criteria

This feature is complete when:

1. Photos has an **Add Photo** button near its title/header.
2. Pressing Add Photo launches the Android system Photo Picker.
3. Only one image can be selected.
4. Canceling the system picker changes nothing.
5. Selecting a valid photo opens a dedicated preview screen.
6. Bottom navigation is not shown on the preview screen.
7. The preview displays a centered square crop.
8. The preview accurately represents what will be saved.
9. Unsaved photos have Save/Cancel only; Cancel returns without saving or changing any saved photo.
10. Save generates a 1024 × 1024 image.
11. The stored format is WebP.
12. Only the processed image is kept; the original is not copied permanently.
13. The stored image has a stable generated ID.
14. Image metadata is persisted locally.
15. Binary image data is not stored in Room.
16. The newly saved photo appears immediately in the Photos grid.
17. The photo remains available after restarting the application.
18. High-resolution normal phone photos do not cause excessive memory use.
19. Invalid/corrupt images fail gracefully.
20. Ordinary failures are cleaned up; process-interrupted saves are reconciled before Library data is exposed.
21. Existing Library navigation continues to work.
22. Existing games remain unaffected.
23. The project builds and relevant tests pass.
24. Photos keeps bottom navigation; the modal covers it and blocks background interaction.
25. Preview and operation state survive rotation; no automatic or duplicate Save/Delete occurs.
26. Process death during an unsaved preview returns to Photos without restoring or saving it.
27. System picker fallback is supported without broad media permissions.
28. System Back/outside taps cannot dismiss either explicit-action modal.
29. Tapping a saved photo opens Delete/Save controls and a top-right X; X closes without changes.
30. Delete removes only the selected saved Library photo, updates the grid immediately, and survives restart.
31. Failed/interrupted deletes recover consistently without duplicate deletion or broken visible entries.
32. Save on a saved photo closes without modifying files, metadata, IDs, or creating another photo.

---

## Focused verification

Add targeted tests for:

- Center crop coordinates and 1024 x 1024 output for landscape, portrait, square,
  and smaller input; preview matches saved output.
- Preservation of the source's displayed orientation, including rotated/mirrored
  metadata, without manual rotation controls.
- Invalid/corrupt inputs and sampled decoding of normal high-resolution photos.
- Repeated Save/Delete taps, mutual exclusion with Cancel, rotation during preview
  and saving, and exactly one resulting photo/navigation event.
- Saved-photo Save/X closing without changes, deletion of one/last photo,
  repeated Delete/Save taps, and database/file failure or interruption during deletion.
- Picker cancellation, automatic system-picker fallback, explicit modal dismissal,
  and blocked underlying Photos/bottom controls.
- Write, rename, metadata, and cleanup failures; interruption before/after rename
  and database commit; reconciliation is safe to repeat and preserves valid photos.
- Persistence after restart/process recreation, unsaved-preview discard after
  process death, empty-to-grid updates, and Home/Library/Back regression coverage.

Manually verify on a small screen and enlarged text, a device with Photo Picker
and one using the fallback, representative phone photos, low-storage failure,
and restart recovery. Run the build, unit tests, lint, and focused device tests.
No large screenshot-test framework is needed.

---
## Result of This Phase

At the end of this feature phase, a parent can perform a complete useful action:

```text
Choose one personal photo
        ↓
see exactly what Puzzle Bubbles will use
        ↓
save it
        ↓
find it permanently in their Photo Library
```

The next photo iteration can extend the preview into an actual editor with pan and zoom while keeping the same import and persistence pipeline.
