# Create Custom What's That? Game

Verified on 2026-11-06: the single-page editor, media selectors, custom playback,
protected Library references, game deletion, requested styling, and consistent
`WhatsThat` naming meet this scope. Build, 18 unit tests, 48 affected emulator
checks across the rename/finalization runs, and lint (0 errors, 18 warnings)
passed. See Iteration 2, Phase 7 for runtime/rendering evidence and limitations.
The requirements below remain the agreed scope; no next feature is planned here.

## Goal and scope

Allow a parent to create, play, and delete custom What's That? games using media
already stored in the Puzzle Bubbles Library. Each saved custom game has 1–10
questions, played once each as 1–10 rounds. Animals retains its ten-round behavior.

The game-list plus opens one full-screen Create Game editor. Opening it never
requires enough Library media. Keep the Library-styled blue title, without a
header Back arrow. Cancel is sufficient. There is no Continue button, step count,
second editor page, or bottom navigation.

Use `WhatsThat` consistently for game-specific types, methods, filenames, and
tests, `whatsthat` for its presentation package, and `WHATS_THAT` for its enum.
Gameplay uses `whats-that/play/{gameId}` and the cover uses `whats-that-cover`.
Preserve the existing `whats-that` built-in ID, `whats-that-*` custom IDs, Room
schema, and Library references. Verify naming changes with build, unit, asset,
gameplay, navigation/recreation, and custom-game persistence checks.

## Single-page editor

Use one naturally scrolling form containing:

- Game name: initially empty, with the placeholder New Game. Trim before validation
  and saving; reject blank names. A simple technical limit of 100 characters is
  sufficient. Duplicate names, including Animals, are allowed; names are not IDs.
- Images in this game: selected photo thumbnails and a Select photos link. Once
  any photos are applied, change its text to Edit photos selection.
  Explain immediately that at least two photos are required for two choices.
  There is no product-level maximum for the photo pool.
- Questions: initially no rows. Show Add Question only after the applied photo
  pool contains at least two photos. This does not require a valid name or any
  available sounds. There is no separate upfront sound-selection pool.
- Cancel and Save actions. Preserve usable name editing with the keyboard in
  landscape and with enlarged text; controls may return after keyboard Done.

Add Question explicitly creates one empty row. Allow up to ten rows, counting
incomplete rows. Show the count/limit and disable adding at ten. A row contains
one correct image and one sound; show which field is missing. Complete rows use
compact cards with the photo, sound name, preview/progress/duration, and a clear
way to replace either media choice. Put a small grey X at the top right
of each question card, positioned relative to the card without increasing the
heading row height. Use Select sound for an
empty sound choice. After selection, use a small two-arrow replacement icon at
the top right of the sound area instead of a Replace sound text button. Its 20dp
artwork must not add space between the name and progress line. Rows can be deleted, including the last row;
zero draft rows is valid editing state but cannot be saved.

Images and sounds may be reused across questions, including identical pairs.
Do not infer semantic uniqueness or preselect distractors in the editor.

Select photos / Edit photos selection and Add Question share an underlined text
link style with 16sp text, no added button padding or filled background. Keep the
name input close to the title, with reduced header and editor top padding. Use that style for
the initial question media-selection links as well. Wizard and game-list controls,
buttons, links, fields, progress, dialogs, and navigation use blue shades rather
than violet. The grey question X and the targeted game deletion background
are deliberate exceptions. Keep other Home/Library screen palettes outside this scope.

## Media selector modals

Select photos / Edit photos selection, Choose correct image, and Select sound open modals over the same
editor, using matching rounded panels, dimmed backdrop, title, top-right X,
Cancel, and Done. Do not navigate to separate selection screens or open Android
Photo Picker. Block interaction with the editor behind a modal.

- The photo-pool selector shows all existing Library photos and supports multiple
  selection. Reopening preselects the applied pool. Show selection using a blue
  image outline/highlight, without checkbox graphics on photos.
- Correct-image selection shows only photos from the applied game pool and allows
  one selection. Reopening preselects that question's current photo. Use the same
  highlight-only style, without checkbox graphics.
- Sound selection shows the existing Sounds Library; there is no game sound pool.
  Use thin rows with a separate Play/Pause preview, name, non-seekable progress,
  duration, and a checkbox on the right. Do not use a large Select sound button.
  Only one sound can be checked; checking another replaces the pending choice.
  Playing a sound must not select it. Reopening checks the question's sound.

Done applies pending selections to the editor/question. Cancel, X, system Back,
and backdrop dismissal discard only pending modal changes and retain the editor.
Done is disabled for a single-choice selector until a choice exists. Once chosen,
a correct image or sound stays selected until replaced or its question is deleted;
the last single selection cannot be unchecked to clear the field.

For the photo pool, selection can begin below two. As soon as a pending selection
reaches two, deselection cannot take it below two within that modal. An applied
pool of two or more remains protected on reopening. Cancelling discards pending
choices and their temporary minimum state. Done may apply an initial zero/one-photo
pool; Add Question and Save remain unavailable until two photos are applied.

Photos referenced by any existing question cannot be deselected from the pool.
Explain that the parent must change or delete those questions first. Replacing a
question's photo releases the old photo only if no other question references it;
the two-photo minimum still applies. The same reference rule applies to sounds,
without introducing a sound pool or a sound-deselection control.

Empty Library selectors explain what is missing and where to add it. They do not
crash, redirect, or close the editor. No inline import/recording or Library shortcut
is required. Missing sounds must not prevent adding a question after two photos
are selected.

At most one preview may play. Stop it on selector exit, replacement, question
removal, Save/Cancel, leaving the editor, or backgrounding. Preserve drafts and
pending modal selections through rotation; do not preserve active playback.

## Validation, persistence, and recovery

Save is enabled only when the trimmed name is nonblank, at least two unique pool
photos are selected, 1–10 questions exist, every question has a correct photo from
the pool, and every question has a Library sound. Incomplete fields must explain
what is missing; avoid a generic Invalid game message.

Generate `whats-that-<UUID suffix>` once per draft, independently of its name.
Persist name, creation time/order, photo-pool IDs, and question image/sound
relationships atomically in Room. Preserve existing Photos/Sounds through an
additive migration. Do not store media blobs or duplicate files. Keep Room
entities, resource IDs, paths, and URLs out of screens/domain models; use the
repository and stable Library asset IDs through the rendering/audio boundary.

Validate referenced media at commit, coordinating Save with media deletion so
no dangling references can be committed. Guard repeated Save and conflicting
editing/navigation while committing. A failure retains the draft for explicit
retry and must not create duplicate games. Never save automatically.

Successful Save closes the editor and immediately refreshes the game list. Saved
games survive restart. Incomplete drafts are not saved games. Process-death draft
restoration is not required; ordinary activity recreation retains all editor and
modal state. Backgrounding stops playback but retains the in-memory draft.

## Cancel and discard

Cancel or system Back from the editor exits to Game Selection. An untouched draft
exits without confirmation. Meaningful changes (name entry, applied photos, added
or edited questions) require Discard this game? with Keep Editing and Discard.
Keep Editing preserves all state; Discard drops the draft. Dismissing a media
selector is not leaving the editor and never prompts to discard the whole game.
No header Back arrow is shown on Create Game.

## Game list, playback, and deletion

Keep the existing puzzle-icon row style, matching Library list typography,
padding, and controls. No custom covers. Predefined rows retain explicit built-in
styling and are first in their existing order. Custom games follow newest first,
with stable ID as a tie-breaker; names may repeat.

Game Selection keeps fixed Home/Library navigation, with neither item active and
no selected bubble/cutout. Home returns Home; Library opens My Library without
leaving an extra selection destination. Add opens the editor.

Play loads the chosen game by stable ID. Play every question once in shuffled
order. Each round uses its explicit photo/sound pairing and one random distractor
from the pool excluding the correct photo, with shuffled presentation. Repeated
pairs remain separate rounds. Do not persist generated distractors. Keep Animals,
answer gestures, feedback, Replay, completion timing/audio, recreation, and fresh
sessions intact. Gameplay Back returns to selection; completion clears gameplay
and selection and returns Home. Start the first prompt once after the entry
transition completes, avoiding early playback/Replay races. Stop audio and pending
work when leaving gameplay; guard rapid Play against duplicate destinations.

Custom rows show Delete immediately before Play; predefined games have no Delete.
Use the existing delete asset and an accessible label. Delete opens Cancel/Delete
confirmation and must not trigger Play. Confirmed deletion atomically removes
only that game's metadata/references, updates the list, and preserves Library
files. Cancellation/failure keeps the game; failures allow retry. Guard repeated
and conflicting Delete/Play actions. Identify targets by stable ID, not name.
Only the game targeted for deletion receives a pale red background with no red
border, while the
confirmation is open and during committing. Other rows keep their regular white
or predefined blue backgrounds even when their actions are temporarily disabled.
Cancel restores the targeted row; successful deletion removes it. Dialog buttons
use the same blue palette as the list.

Block Library deletion of photos/sounds referenced by any saved custom game,
including distractor-only pool photos, at both UI and storage boundaries. Use the
generic message This photo/sound is used by a custom game. Delete the game first.
Do not name games or redirect automatically. The parent manually deletes games
from the list, then deletes media in Library only after no saved references remain.
Cropping referenced photos remains allowed, retaining identity and changing the
image used by games. Other Library behavior remains unchanged. Do not cascade-delete
games when removing media, and do not duplicate media to avoid dependencies.

## Out of scope

Saved-game editing, draft auto-save/process-death restoration, custom covers,
extra descriptions/categories/tags/difficulty, game duplication/sharing/networking,
inline photo import/audio recording, configurable distractors, question sorting or
drag-and-drop, question labels, and review/summary stages remain outside this feature.

## Acceptance and verification

Verify the single editor has no Back arrow, step count, Continue, second page, or
bottom navigation. Add Question is hidden below two applied photos and visible
with two even when name/sounds are missing. No initial row is added automatically;
1–10 complete questions are required only for Save, and all draft rows can be deleted.

Verify all three selectors share modal styling, block background actions, preselect
current choices, preserve pending choices on rotation, apply only on Done, and
cancel via Cancel/X/Back/backdrop without changing the applied draft. Check thin
sound rows, preview without selecting, right-side checkboxes, single choice,
replacement, referenced-photo locks, the two-photo floor, and shared references.

Test validation and question limits/reuse, round generation, discard/recreation,
Save failure/retry/idempotency, persistence/migration, concurrent media deletion,
list refresh/order, Delete cancellation/failure, shared-reference release, custom
playback, native audio, and navigation/gesture/completion regressions. Run build,
unit tests, lint, and affected device tests. Provide useful initial/ready/completed,
empty/modal/selected and enlarged-text/landscape previews. Render and inspect key
editor/modal/gameplay layouts on-device when available, including keyboard use.
Keep known environmental verification limitations explicit; do not suppress failures.
