# Plan 003 — Image Crop Editor

Status: **Agreed scope; not implemented**. This is the next feature phase of
Iteration 2. The full iteration remains open until the user explicitly finishes it.

## Goal

Extend the existing photo-library flow with an interactive image editor.

The parent can choose exactly which square area of an image Puzzle Bubbles should save by:

- dragging the image with one finger;
- zooming with a pinch gesture.

The crop area itself always remains a fixed square.

This editor is used in two places:

```text
new photo
Photo Picker
    ↓
Image Editor
    ↓
Save new photo

existing photo
Photos Library
    ↓
Tap saved photo → Image Editor directly
    ↓
Replace existing photo
```

The application still stores only the processed **1024 × 1024** image.

The original gallery image is not retained.

---

## 1. Editor Modal

The image editor is a modal overlay on Photos, using a rounded panel over a
dimmed background. Keep it in the existing Photos flow; do not add a separate
page or navigation destination. The panel adapts to smaller screens and rotation.

Do not display bottom navigation.

Conceptually, for a new photo:

```text
                           ×

┌────────────────────────────┐
│                            │
│       darkened image       │
│                            │
│     ┌──────────────┐       │
│     │              │       │
│     │ visible crop │       │
│     │     area     │       │
│     │              │       │
│     └──────────────┘       │
│                            │
│       darkened image       │
│                            │
└────────────────────────────┘

            Save
```

The exact visual styling should follow the existing Puzzle Bubbles design.

For a **new, unsaved photo**, show a full-width Save button at the bottom and an accessible
X close control at the top right. Do not show Delete or a header Back arrow.

For a **saved photo**, show Delete / Save at the bottom and an accessible X
close control at the top right. X cancels unsaved edits. There is no bottom
Cancel button and no intermediate viewer or separate Edit action.

Both states cover and block the Photos page and bottom navigation. There is no
visible editor title. Tapping the backdrop cancels editing, like Back and X in
section 11; dismissal is blocked during Save/Delete.

---

## 2. Fixed Crop Area

The saved area is always:

```text
1 : 1
```

The crop square:

- remains centered on screen;
- does not move;
- does not resize;
- represents exactly what will be saved.

These constraints apply within the current layout: there are no user resizing
handles. After rotation or a screen-size change, the viewport may adapt to the
available editor area while preserving the selected source region.

The **image moves underneath the crop area**, rather than moving the crop rectangle over the image.

This gives the interaction a simple mental model:

```text
crop window stays still
        +
photo moves behind it
```

---

## 3. Crop Overlay

Make the crop area visually obvious.

The image should remain visible across the editor, but everything **outside the crop square** should have a semi-transparent dark overlay.

Conceptually:

```text
          dark overlay
             ↓
┌─────────────────────────┐
│█████████████████████████│
│████┌───────────────┐████│
│████│               │████│
│████│ saved content │████│
│████│               │████│
│████└───────────────┘████│
│█████████████████████████│
└─────────────────────────┘
```

Inside the square:

- image remains at normal brightness;
- no tint should alter the preview.

Outside the square:

- apply a translucent dark scrim.

Optionally add a subtle border around the crop square if it improves clarity.

Avoid a heavy grid or professional photo-editor UI.

This should remain visually simple and friendly.

---

## 4. Initial Image Position

### New image from gallery

When an image first enters the editor:

1. orient it correctly;
2. scale it so the crop square is completely filled;
3. center the image inside the crop square.

This produces the same **center crop** currently used by the application.

Saving without interaction should produce the same centered crop and visual
composition as the current automatic behavior; byte-identical WebP encoding is
not required.

The current processor crops and normalizes before showing the preview. Adapt
this pipeline to keep a sampled, correctly oriented, **uncropped** editing
source temporarily, so panning can reveal areas outside the initial square.
Do not feed an already center-cropped 1024 × 1024 draft into the new-photo editor.
Produce the final normalized crop only on Save; do not retain the original afterward.

### Existing library image

Existing library images are already:

```text
1024 × 1024
```

When editing one:

- show the existing saved image;
- initially fill the crop square exactly;
- use it as the new editing source.

Because the original photo is not retained, subsequent edits operate on the already processed image.

Some quality loss after repeated zooming/cropping is accepted for this MVP.

---

## 5. Drag / Pan

Allow the parent to move the image using one-finger dragging.

Example:

```text
finger →→→

crop stays fixed

image →→→
```

Movement should feel direct:

- no inertia;
- no fling;
- no snapping between positions.

The image follows the finger.

### Movement constraints

The image must always completely cover the crop square.

The user must never be able to create:

```text
┌──────────────┐
│ IMAGE        │
│ IMAGE        │
│ IMAGE   EMPTY│
│         EMPTY│
└──────────────┘
```

When the image reaches its valid movement boundary, additional dragging in that direction should simply stop moving it.

---

## 6. Pinch to Zoom

Support standard two-finger pinch zoom.

```text
pinch inward  → zoom out
pinch outward → zoom in
```

Zoom should feel continuous rather than stepping through predefined levels.

### Minimum zoom

The minimum zoom is the scale at which the image still completely covers the crop square.

The parent cannot zoom out beyond this point.

For example, for a landscape image:

```text
┌───────────────────────────┐
│                           │
│     ┌─────────────┐       │
│     │ crop square │       │
│     └─────────────┘       │
│                           │
└───────────────────────────┘
```

the initial center-crop scale is also the minimum scale.

For an already saved square image, its initial full-square view is the minimum zoom.

### Maximum zoom

Apply a reasonable maximum zoom so accidental gestures cannot create absurd magnification.

Something around:

```text
4× relative to minimum scale
```

is a sensible initial limit.

This value can be adjusted after testing.

---

## 7. Combined Gestures

Dragging and zooming should work naturally together.

A two-finger gesture may change:

```text
scale
+
position
```

at the same time.

Rotation should be ignored.

Do not rotate the image in response to twisting gestures.

Jetpack Compose provides `transformable` / transform gesture APIs for exactly this kind of pan and zoom interaction.

Use rendering transforms rather than recomputing the bitmap continuously while the parent is moving it. Compose's `graphicsLayer` supports scale and translation at draw time, which is appropriate for interactive preview transformations.

---

## 8. Saving

Pressing **Save** converts the current editor state into the final library image.

The saved result must correspond exactly to the visible crop square.

Conceptually:

```text
original / current source image
        +
zoom
        +
translation
        +
square viewport
        ↓
render crop
        ↓
1024 × 1024
        ↓
WebP
```

Do not save the entire transformed image.

Only the area visible inside the crop square becomes the output.

---

## 9. Saving a New Photo

For a newly selected gallery image:

```text
Photo Picker
    ↓
Image Editor
    ↓
Save
    ↓
create new stable photo ID
    ↓
write 1024 × 1024 WebP
    ↓
persist metadata
    ↓
Photos
```

The gallery original is only a temporary input.

After successfully producing the Puzzle Bubbles image, do not retain a private copy of the original.

---

## 10. Editing an Existing Photo

Tapping a saved photo in the grid opens the same editor directly.

The source is the currently stored:

```text
1024 × 1024 WebP
```

When Save is pressed:

- keep the same photo/media ID;
- preserve creation time and grid position;
- produce a new 1024 × 1024 WebP;
- replace the previous processed file;
- keep references from future game content stable.

Do not create a second library item merely because an existing photo was edited.

If the selected region is unchanged, Save simply closes the editor without
rewriting the image, changing metadata, or introducing another lossy encode.

Use a safe replacement strategy so a failed encode/write cannot destroy the existing image.

Conceptually:

```text
existing.webp
      ↓
edit
      ↓
write temporary file
      ↓
success?
   ├── yes → replace existing.webp
   └── no  → keep existing.webp untouched
```

Serialize replacement with saves, deletion, and reconciliation. Validate and
finish the temporary output before replacing the current file; never delete or
truncate the previous image first. Recovery after process interruption must
leave a complete valid old or new version, with the same stable ID, and clean
up incomplete temporary/backup files safely. A failed replacement keeps the
previous photo intact and leaves the editor open for explicit retry.

After success, publish the changed image and refresh grid thumbnails and future
editor loads immediately. The current renderer reloads by asset ID only; the
replacement path must invalidate stale image state without changing the photo ID.

Delete is available only for a saved photo. It removes that Library record and
its owned image, discards any unsaved edits, and returns to Photos. Preserve the
existing deletion recovery behavior; never delete the gallery original or
bundled artwork. A failure keeps the editor open with a friendly retry message.

---

## 11. Cancel / Back

Tapping the backdrop or the top-right **X**, or pressing system Back, returns
without saving changes.

System Back behaves the same way.

For a newly selected image:

```text
Cancel
→ discard temporary selection
→ Photos
```

For an existing image:

```text
Cancel
→ keep existing image unchanged
→ Photos
```

No confirmation dialog is required if no persistent change has happened yet.

While Save or Delete is in progress, disable and guard all conflicting actions,
including Back/Cancel/X and further gestures. Accept at most one operation;
rapid taps and activity recreation must not duplicate Save/Delete or navigation.
After failure, preserve the edit and allow an explicit retry once cleanup finishes.

---

## 12. Editor State

The editor should conceptually maintain:

```text
source image
crop viewport size
minimum scale
current scale
maximum scale
translation X
translation Y
```

The transformation state is UI/editor state.

Do not write a new bitmap to disk while the parent is dragging or zooming.

Generate the final bitmap only when Save is pressed.

Preserve the temporary source, selected source region, zoom, and any in-progress
operation across activity rotation. Store crop coordinates relative to the
source rather than raw screen pixels; keep bitmap work off the main thread and
bitmaps out of saved-state bundles. Recalculate layout bounds without resetting
the crop or saving automatically.

After process death, reopen Photos without restoring or saving unsaved edits.
For saved-photo edits, keep the stored version unchanged unless Save or Delete
had already committed; reconcile interrupted operations before exposing Library content.
Do not automatically retry an interrupted Save or Delete.

---

## 13. Transformation Constraints

Every time scale or translation changes:

1. calculate the transformed bounds of the image;
2. ensure those bounds completely contain the crop square;
3. clamp invalid translation;
4. clamp scale between minimum and maximum.

This rule must hold continuously during interaction.

The crop viewport must never expose the background.

---

## 14. Image Quality

### New gallery image

Use enough source resolution to produce a high-quality final:

```text
1024 × 1024
```

Avoid unnecessarily loading huge full-resolution camera bitmaps into memory.

Choose bounded decoding appropriate to the crop, output resolution, and up to
4× zoom, rather than reusing the previous 1024-only decode target. Preserve the
uncropped aspect ratio and EXIF orientation. Do not promise additional detail
that a small source does not contain or archive originals for later editing.

### Existing image

The available source is already 1024 × 1024.

If the parent zooms into it and saves again, some quality reduction is expected.

This tradeoff is intentional because Puzzle Bubbles does not retain original gallery files.

Do not introduce an original-image archive solely to avoid this limitation.

---

## 15. Reuse

Use the **same image editor implementation** for:

```text
new gallery image
existing library image
```

Do not build separate crop UIs for these cases.

The difference should only be:

```text
input source
+
save behavior
```

---

## 16. No External Crop Library Required

For this version, do not introduce an external image-cropping library unless implementation reveals a concrete blocker.

The required functionality is deliberately small:

- fixed 1:1 viewport;
- pan;
- pinch zoom;
- constrained bounds;
- render final crop.

Compose already provides the gesture and transformation primitives required for the interactive portion.

Keeping this custom also allows the editor to visually match Puzzle Bubbles instead of embedding a generic third-party crop screen.

---

## 17. Explicitly Out of Scope

Do not add:

- movable crop rectangle;
- resize handles;
- different aspect ratios;
- free-form crop;
- image rotation;
- flip/mirror;
- filters;
- brightness/contrast controls;
- automatic face/object detection;
- undo history;
- original-photo storage;
- arbitrary output dimensions;
- multi-image editing.

---

## 18. Acceptance Criteria

The image editor is complete when:

1. Selecting a new photo opens the editor before saving it.
2. The editor displays a fixed centered square crop area.
3. The area outside the crop square is visibly darkened.
4. The area inside the crop square shows the image at normal brightness.
5. A newly selected image initially uses a centered crop.
6. Saving a new photo without interaction produces the current center-crop composition, without requiring byte-identical encoding.
7. The image can be dragged with one finger.
8. The image can be zoomed using a two-finger pinch.
9. Pan and zoom can occur naturally in the same interaction.
10. Rotation gestures do not rotate the image.
11. Zoom cannot go below the minimum required to cover the crop square.
12. Zoom is limited to a reasonable maximum.
13. Panning cannot expose an empty area inside the crop square.
14. The crop square has no user move/resize controls; layout changes preserve the selected source region.
15. Save produces exactly the region visible in the crop square.
16. Saved output remains exactly 1024 × 1024.
17. Saved output uses the existing WebP storage pipeline.
18. Cancel does not persist any changes.
19. System Back behaves like Cancel except while Save/Delete is committing.
20. Tapping a saved photo opens the same editor directly, with Delete/Save at the bottom and X at the top right.
21. Editing an existing photo keeps its stable media ID.
22. Saving an edit safely replaces the existing processed image.
23. Failure while replacing an image does not destroy the previous version.
24. The original gallery image is not retained after a new photo is saved.
25. Existing Library and game functionality remains unaffected.
26. Saving an unchanged existing photo closes without rewriting files or metadata.
27. Editing preserves creation time and grid position, and successful replacement refreshes thumbnails immediately.
28. Interrupted replacements recover a complete old or new version without losing the photo or changing its ID.
29. Rotation preserves the crop and busy operation; rapid actions cannot duplicate Save/Delete or navigation.
30. Process death discards unsaved edits without automatically retrying an operation.
31. Failed Save/Delete remains recoverable through an explicit retry with a friendly error.
32. New photos have a full-width Save button without Cancel or Delete; X cancels without saving. Saved-photo X cancels without changing the stored image.
33. Saved-photo Delete removes only that Library item, and deleting the last photo restores the empty state.

---

## 19. Focused Verification

Add focused tests for:

- Minimum/maximum zoom, pan clamping, source-to-viewport crop conversion, and
  combined pan/zoom for landscape, portrait, square, and small images.
- EXIF rotation/mirroring, initial center-crop composition, and preview/export
  agreement at viewport edges and maximum zoom. Output stays 1024 × 1024 WebP.
- Rotation and viewport resizing preserve the selected source region; process
  death discards unsaved changes without mutating the saved photo.
- Direct saved-photo entry, new versus saved controls, Cancel/Back/X, and Delete.
  Underlying Photos and bottom navigation remain blocked during editing.
- Rapid Save/Delete, conflicting close actions, and recreation during operations
  result in one operation and one return to Photos.
- Unchanged saved-photo Save preserves bytes and metadata. Changed Save retains
  ID/creation time/order, replaces the image, and refreshes thumbnails and re-editing.
- Encode/write/replace/cleanup failures and process interruption around replacement;
  reconciliation is repeatable and preserves a complete old or new image.
- Existing import, deletion/recovery, Library navigation, and game regression tests.

Manually check drag/pinch behavior and crop/output agreement using representative
images, including small screens, enlarged text, and rotation. Exercise Save,
re-edit, Cancel, deletion, errors, and restart recovery. Run the debug build,
unit tests, lint, and relevant device tests; do not add screenshot infrastructure.

---

## Result

The parent now controls exactly which part of a personal photo becomes Puzzle Bubbles content:

```text
choose photo
     ↓
center crop appears
     ↓
drag + pinch
     ↓
preview exactly what will be kept
     ↓
Save
     ↓
1024 × 1024 library image
```

This phase also supports re-editing a previously edited photo through the same
editor. Each edit starts from the currently stored normalized image; no original
gallery image or edit history is retained.
