# Puzzle Bubbles — MVP Specification

This document defines intended MVP requirements, including unimplemented work.
It is not a record of completed functionality. See [README.md](../README.md) for
the current application and `docs/iterations/` for functional milestones and pending work.
The next agreed implementation step and its acceptance criteria live in
[plan.md](plan.md); they must remain consistent with these product requirements.

## 1. Product

**Puzzle Bubbles** is a simple educational game for young children.

The first version contains two simple game mechanics:

1. **What's That?** — the child hears a sound and chooses the matching image.
2. **Matching** — the child matches images from one side with images on the other side.

The product will be developed iteratively.

The first version should contain only enough functionality to establish the core architecture and make these two mechanics playable.

Categories, topics, difficulty levels, sound types, and other content organization may be introduced later when needed.

## 2. First Iteration Scope

The first iteration is a **local-only Android application**.

There is:

- no backend;
- no authentication;
- no user accounts;
- no synchronization;
- no analytics;
- no remote content management;
- no content categories;
- no game subcategories;
- no sound-mode selection.

All game content is available locally.

Local content consists of:

1. **Structured game data**
   - rounds;
   - image choices;
   - matching relationships;
   - asset IDs;
   - labels and other metadata when needed.

2. **Binary assets**
   - images;
   - sounds.

Binary image/audio files should **not be stored as blobs in Room**.

Initially:

- optimized images are stored as Android resources;
- optimized audio files are stored as Android resources;
- structured game data may be stored as bundled JSON or similar local data;
- Room may be introduced later if persistent structured storage becomes useful.

The data model must remain independent from local persistence and asset-storage details so that content can later move to a backend/CDN without redesigning the game logic.

## 3. Platform

Frontend:

- Native Android
- Kotlin
- Jetpack Compose

Local data:

- Android resources for image and audio files;
- bundled JSON or similar structured data for initial static game data;
- Room only when persistent structured local storage is actually needed;
- Room must not be used to store image/audio blobs.

Game/domain models must not depend directly on:

- Room entities;
- Android resource IDs;
- filesystem paths;
- URLs.

Use stable asset IDs at the domain level.

Example:

```text
cat-image
cat-audio
```

The data layer is responsible for resolving these IDs to their current physical location.

## 4. Navigation

The Figma home design uses the child-facing names **What's That?**
and **Match Up** for the two mechanics. Code uses `WhatsThat`, the package
`whatsthat`, and `GameType.WHATS_THAT` for the first mechanic. Its built-in
game/catalog ID is `whats-that`; Match Up retains `matching`. Display names and asset
IDs such as `whats-that-cover` are independent of those game IDs. The home uses the
corresponding `game-covers` artwork as card backgrounds with colored title bars.
Home and Library use a fixed, full-width bottom navigation bar without top tabs
or a floating container. Icons have small, matching-color labels underneath;
Home uses the existing games icon. The active icon sits in a raised soap bubble
with a close, smoothly rounded cutout. Touch feedback highlights only the icon
and label, with no background ripple. The bar stays visible while scrolling.
Library opens **My Library**, with a game-style title and Back arrow, without
the logo, top tabs, or bottom Back button. Photos and Sounds cards lead to
separate destinations with empty placeholders when needed. Those child screens keep Back to My Library and the
fixed Home/Library navigation bar with Library selected. Home returns to Home;
Library returns to My Library without adding destinations. Do not add sample
Library content or change existing game audio.

The agreed Add Photo phase extends Photos with a single-image system picker
(including its document-picker fallback) and an explicit-action preview modal.
Use an icon-only plus button on the right of the Photos title, with the accessible
label Add Photo. A system document-picker compatibility fallback is also permitted
for a picker build that cannot reliably return a selected image.
Photos retains bottom navigation; the modal covers it. Save stores only a
center-cropped 1024 × 1024 WebP in private app storage, with generated stable IDs
and Room metadata. Unsaved photos have Save/Cancel; tapping a saved photo opens
Delete/Save and a top-right X to close. Delete removes only the app-owned saved
photo, never the gallery original. Save on a saved photo currently closes without
changes in the initial Add Photo phase. Library audio and custom-photo gameplay
remain outside that phase.

The agreed Crop Editor phase replaces this preview with a shared modal editor
over Photos, with a rounded panel and dimmed background rather than a separate
page. It supports new and saved photos: a fixed square viewport, constrained pan/pinch zoom up to 4×,
and 1024 × 1024 WebP output. Tapping a saved photo opens the editor directly,
with Delete/Save below and X at the top right to cancel. New photos use
a full-width Save button with a top-right X and have no Cancel, Delete, or header Back arrow. The editor
has no visible title. Back or tapping the dimmed backdrop cancels editing;
all conflicting actions are blocked during Save/Delete. Unchanged saved photos
close without rewriting. Changed photos retain their ID, creation time, and
order; safe replacement and recovery preserve a valid image after interruption.
Re-editing uses the currently stored image, without retaining originals.
These paragraphs define requirements, not implementation status.

The agreed **Record and Manage Library Sounds** feature is another phase of
Iteration 2. Sounds retains the fixed Home/Library navigation, with Library
selected; its recording modal covers and blocks the screen and navigation.
The header plus opens the modal without requesting microphone access. When Sounds
is empty, show the sound icon with a duplicate **Add Record** button beneath it,
following the Photos empty-state pattern, instead of a "No sounds yet" message.
Both entry points use the same modal and action guards; the extra button is hidden
when saved recordings exist. Request
`RECORD_AUDIO` only on Record. Handle denial and offer app settings when needed,
without starting automatically on return.

Record mono AAC in an MPEG-4 (`.m4a`) container, with a five-second recorder limit,
manual Stop, and a visible timer. Keep the recorder and UI limit at 5,000 ms;
allow at most 100 ms of native AAC/container output overhead (5,100 ms total).
Validate that bound, preserve the actual duration, and reject longer output
without trimming or transcoding. Preview only valid finalized recordings. Preview
supports Play/Pause, a non-seekable progress line, Cancel, and explicit Save; X,
Back, and backdrop taps cancel outside committing operations. Never save
automatically. Rotation preserves the current session and its original deadline;
true backgrounding discards unsaved work. Process death restores Sounds without
a draft and reconciles incomplete storage operations.

Preview also offers a Name field, initially focused with the complete `Track N`
default selected (N = saved recording count + 1). Allow duplicate names; trim
surrounding whitespace on Save and use the draft's default for blank input.
Preserve the name through rotation and Save failures, and show it in saved rows.
Naming is available before initial Save only; renaming saved sounds remains deferred.

Persist generated IDs, names, duration, and creation time through a repository, keeping
filenames and Room entities out of screens/domain models. Store recordings in
private app-owned files, not Room blobs or bundled assets. Preserve Photos through
an additive database migration. Saves and metadata-first deletion require cleanup
and repeatable reconciliation after failures/interruption. Exclude recordings
and their metadata together from backup and transfer.
Give existing unnamed recordings default names through migration without changing
their IDs, timestamps, durations, or audio files. Names are not unique identifiers.

Saved rows support Play/Pause, progress, duration, and confirmed Delete. Only one
recording may play at a time; recording and playback cannot overlap. Completion
resets playback to the beginning; leaving/backgrounding stops it. Guard duplicate
actions and stale callbacks. Import, waveform generation, seeking, editing,
renaming, re-recording, and custom-game association are outside this phase.
See [the current plan](plan.md) for the agreed next feature's acceptance criteria.

The home/navigation step alone does not fulfill the playable MVP definition of
done in section 16. Acceptance is evaluated against the selected iteration step;
the current implementation status is maintained separately from these requirements.

The first screen is a list of available game mechanics.

Initial items:

1. **What's That?**
2. **Matching**

Each item is represented by a large child-friendly card with:

- title;
- cover image.

Selecting What's That? opens its Game Selection screen. Selecting the built-in
**Animals** row starts existing gameplay. Match Up continues to open directly.
The selection screen has no header, title, or header Back arrow. It uses the fixed
Home/Library bottom navigation, with neither destination selected on Game Selection.
Home returns to Home; Library
opens My Library without leaving an extra selection destination behind it.
A filled circular Add Game plus button floats at the bottom right above navigation,
with list padding so it cannot cover the last row. Its accessible label is
Add Game, and it opens the custom-game editor described below.

The repository supplies the list and explicitly identifies the built-in entry.
That row uses a slightly tinted background and bold accent-colored text. Other
entries use white backgrounds and regular-weight text. Rows show the puzzle icon
and name without a separate Play icon; tapping a normal row starts gameplay.
Only the predefined Animals entry is required now;
additional predefined games may be added later. Custom examples belong only in
previews and tests.

Selection system Back returns to Home. Gameplay's Back arrow and system
Back return to the previous selection screen. Automatic game completion retains
its existing timing/audio and returns to Home, removing gameplay and selection
from the back stack. Guard repeated Play taps and preserve destinations through
activity recreation. Opening selection does not play gameplay audio; leaving
gameplay stops audio and pending work, and starting again creates a fresh session.

The built-in game has ID `whats-that`. Custom games use
`whats-that-<unique suffix>`, generated once and retained through edits; random UUID
suffixes are recommended rather than hashes of mutable names or content.
Custom-game creation, persistence, and gameplay extend this navigation as
described below, including management of existing custom games.

The subsequent agreed **Create Custom What's That? Game Wizard** feature enables
creation and playback from this list. Its full-screen single-page editor has a
name, Library photo pool, and 1–10 explicit image/sound questions. It has Cancel
instead of a header Back arrow, no step indicator, and no bottom navigation.
Question rows start empty; Add Question appears after at least two photos have
been selected, independently of the name or availability of sounds. Sounds are
chosen per question, without a sound pool. All three media selectors use matching
modals with Done/Cancel/X and Back dismissal; pending choices apply only on Done.
Sound choices use compact preview rows with a single-selection checkbox at the
right. Once a photo selection reaches two, it cannot be reduced below two.
Photo selectors use only an image outline/highlight, without checkbox graphics.
Select photos changes to Edit photos selection after applying photos; it and
Add Question use underlined text links without added button padding. The empty
name placeholder is New Game. Links use compact 16sp text, and the name input
sits closer to the title. Question cards have a small grey X anchored to the
top right independently of the heading, a Select sound link when empty, and a
20dp two-arrow replacement icon at the top right of selected sound content that
does not increase the gap above the progress line. Wizard and game-list controls use blue
shades, including dialogs and list navigation, except for icon-only Edit and
Delete actions revealed by swiping custom rows. Use almost transparent mint/rose
backgrounds and moderately bright cool green/rose pencil/trash icons that harmonize
with blue. Preserve accessible text labels without visible action text.
Delete buttons inside game, photo, and sound modals use a consistent red with
brightness and saturation comparable to the blue Save action.
Only the game targeted for deletion
receives a pale red deletion background without a border; other disabled rows retain their normal backgrounds.
Question-referenced photos cannot be deselected from the pool until all those
references are deleted or replaced. Single-choice photo/sound selections can be
replaced but not cleared; deleting a question releases its references. All draft
rows may be deleted, but Save still requires at least one complete question.
Custom sessions play each question once in shuffled order, with one distinct
distractor from the selected image pool; Animals retains ten rounds. Media may
be reused across questions. Persist complete games and stable Library references
atomically in Room without copying files. Referenced pool photos and question
sounds cannot be deleted; photo cropping remains allowed. Use a generic
"Delete the game first" explanation without game names. Custom-game rows reveal
Edit followed by Delete on a right-to-left swipe, without leaving the screen.
An incomplete swipe snaps closed; a completed swipe exposes both actions and
keeps part of the main row visible. Square the shifted row's right corners to
join the actions, restoring rounding when closed. Only one row may be open. Tapping any game
row while actions are open closes them without launching gameplay; tapping
elsewhere in the list also closes them. Vertical scrolling and horizontal swipes
must not launch a game. TalkBack custom actions expose Edit/Delete without
requiring a row swipe or adding visible controls. Predefined rows have no management
gesture or accessibility actions. Delete requires named confirmation that Library
media is retained. Deletion removes the game and its
references atomically, preserving Library files; media can then be deleted manually
only when no saved game still references it. Predefined games cannot be deleted.
Edit Game reuses the single-page editor and preloads all saved values. Save
atomically updates the same game and its references, preserving creation time and
list position. Release removed media references only after commit; failure shows
an error and keeps the editor open with the stored game unchanged. Compare the
form with its loaded values so reverted edits do not require discard confirmation.
No persistent editing draft is required. Preserve drafts through rotation, confirm discarding meaningful
changes, and retain drafts after Save failure for explicit retry. Process-death
draft restoration is not required. See [the plan](plan.md) for acceptance criteria.

Navigation:

```text
HomeScreen
│
├── What's That?
│   └── WhatsThatGameSelection
│       └── WhatsThatGameScreen
│
└── Matching
    └── MatchingGameScreen
```

There is no intermediate category-selection screen in the first iteration.

## 5. Game Type 1 — What's That?

### Purpose

The child hears a sound and chooses the image that corresponds to that sound.

The game engine does not need to understand what kind of sound it is.

The sound may represent:

- a spoken word;
- an animal sound;
- a vehicle sound;
- music;
- another audio cue.

For the MVP, the exact content is simply provided as local game data.

The mechanic only needs to know:

```text
play this sound
show these images
this image is correct
```

### Example

Audio:

```text
cat_audio
```

Images:

- cat
- dog
- pig

Correct answer:

- cat

The game must not infer why the sound matches the image.

The relationship is explicit in the data.

### Game Flow

1. A round starts.
2. The round's audio is played.
3. Several image choices are displayed.
4. The child taps an image or starts a swipe on its bubble.
5. The application shows whether the answer is correct.
6. If correct, the game proceeds to the next round.

For the first playable What's That? version, a session contains **10 rounds**.
Each of the 10 local target items appears exactly once in randomized order.
Every round contains exactly **two image choices**: the explicit correct image
and one randomized incorrect image from another target item. Choice order is
randomized independently from correctness.

### Interaction

The screen should contain:

- a large play/replay button;
- two large selectable image bubbles;
- feedback after selection.

Accept taps and swipes on either image bubble, using the same explicit answer
data and feedback. A swipe in any direction selects its starting bubble once
movement crosses the normal touch threshold, even if the finger leaves it.
Crossing another image does not select a second answer. Keep accessible click
activation, ignore input during feedback, and do not carry a held gesture into
the next round. An interrupted touch before that threshold selects nothing.

If the child selects an incorrect image:

- give simple visual feedback;
- keep the round active;
- allow another choice.

If the child selects the correct image:

- give positive visual feedback;
- hide the other choice immediately and start the success sound, movement toward
  the gameplay area's center, and growth together;
- grow the selected bubble smoothly over roughly 1.2 seconds to at most 130%,
  keeping it within the gameplay area's bounds;
- wait for both growth and the complete success sound, then briefly fade/pop the
  bubble before advancing;
- move to the next round.

The sound should play once when the round opens and should be replayable.

No scoring is required in the first iteration.

### Example Content

```text
soundAssetId: cat-audio

choices:
  - cat
  - dog

correctChoiceId: cat
```

## 6. Game Type 2 — Matching

### Purpose

The child matches images from one side with corresponding images on the other side.

The game engine does not need to understand the semantic reason for the relationship.

For example, the supplied data might represent:

```text
rabbit → carrot
monkey → banana
dog → bone
```

or:

```text
bird → nest
bee → hive
fish → water
```

But the engine only needs to know which image IDs form valid pairs.

### Game Flow

The screen shows two groups of images.

Example:

Left:

- rabbit
- monkey
- dog

Right:

- banana
- bone
- carrot

The child matches each item from the left side with one item from the right side.

The display order may be randomized.

### Interaction

For the first iteration, use **tap-to-match**.

Example:

1. Child taps rabbit.
2. Rabbit becomes selected.
3. Child taps carrot.
4. If correct, the pair becomes completed.
5. If incorrect, visual feedback is shown and the pair remains unmatched.

The round is complete when all pairs are matched.

Drag-and-drop or line-drawing interactions may be considered later.

No score or timer is required.

### Example Content

```text
pairs:
  rabbit -> carrot
  monkey -> banana
  dog -> bone
```

The game does not need categories such as `animal`, `food`, or `home`.

Those concepts may exist later for content organization, but they are not part of the MVP game model.

## 7. Core Content Model

Content should be represented as generic domain data rather than hard-coded directly inside composables.

Binary assets and structured game data are separate concerns.

The domain model references assets using stable IDs.

Conceptually:

```kotlin
data class MediaAsset(
    val id: String,
    val type: MediaType
)

enum class MediaType {
    IMAGE,
    AUDIO
}
```

The domain model should **not** contain Android resource identifiers such as:

```kotlin
R.drawable.cat
R.raw.cat_audio
```

It should also not depend directly on filesystem paths or remote URLs.

The data/infrastructure layer resolves an asset ID to its actual source.

For the MVP:

```text
cat-image
    ↓
Android drawable resource

cat-audio
    ↓
Android raw resource
```

In a future backend-driven version:

```text
cat-image
    ↓
CDN URL

cat-audio
    ↓
CDN URL
```

Game definitions should remain unchanged regardless of where the binary asset is physically stored.

## 8. What's That? Model

Conceptually:

```kotlin
data class WhatsThatGame(
    val id: String,
    val title: String,
    val coverImageAssetId: String,
    val rounds: List<WhatsThatRound>
)

data class WhatsThatRound(
    val id: String,
    val soundAssetId: String,
    val choices: List<ImageChoice>,
    val correctChoiceId: String
)

data class ImageChoice(
    val id: String,
    val imageAssetId: String
)
```

The UI must not infer the correct answer from filenames, array positions, spoken text, or asset names.

The correct relationship must be explicit in the data.

## 9. Matching Model

Conceptually:

```kotlin
data class MatchingGame(
    val id: String,
    val title: String,
    val coverImageAssetId: String,
    val pairs: List<MatchingPair>
)

data class MatchingPair(
    val id: String,
    val leftImageAssetId: String,
    val rightImageAssetId: String
)
```

Example:

```text
pairs:
  rabbit-image -> carrot-image
  monkey-image -> banana-image
  dog-image -> bone-image
```

The UI is responsible for presentation order.

Stored data represents relationships, not layout positions.

## 10. Game Catalog

Top-level games should be discoverable from a common catalog.

Conceptually:

```kotlin
data class GameDefinition(
    val id: String,
    val title: String,
    val type: GameType,
    val coverImageAssetId: String
)

enum class GameType {
    WHATS_THAT,
    MATCHING
}
```

Initial catalog:

```text
whats-that
matching
```

The home screen should render these items from catalog data rather than containing individually hard-coded navigation buttons.

## 11. Architecture

Prefer a simple layered architecture.

```text
UI
↓
ViewModel / game state
↓
Domain
↓
Repository
↓
Local structured data + asset resolver
```

For example:

```text
presentation/
domain/
data/
```

Avoid adding architecture layers unless they solve a current problem.

Important principles:

**Compose UI must not know whether game metadata comes from JSON, Room, or a future backend.**

**Compose UI must not know whether an image/audio asset comes from Android resources, local files, or a future CDN.**

Example:

```kotlin
interface GameRepository {
    suspend fun getGameCatalog(): List<GameDefinition>
    suspend fun getWhatsThatGame(id: String): WhatsThatGame
    suspend fun getMatchingGame(id: String): MatchingGame
}
```

Asset resolution is a separate concern.

Conceptually:

```kotlin
interface AssetResolver {
    fun resolveImage(assetId: String): ImageSource
    fun resolveAudio(assetId: String): AudioSource
}
```

The exact implementation types can be decided during development and should not be generalized prematurely.

Initial architecture:

```text
GameRepository
    ↓
LocalGameRepository
    ↓
bundled JSON / optional Room metadata

AssetResolver
    ↓
Android drawable/raw resources
```

Possible future architecture:

```text
GameRepository
    ↓
RemoteGameRepository
    ↓
HTTP API

AssetResolver
    ↓
local cache / CDN URLs
```

The presentation and game logic should not need fundamental changes.

## 12. State

Game state is separate from game content.

For example, What's That? may contain:

```kotlin
data class WhatsThatUiState(
    val roundIndex: Int,
    val selectedChoiceId: String?,
    val result: AnswerResult?,
    val isSoundPlaying: Boolean
)
```

Matching may contain:

```kotlin
data class MatchingUiState(
    val selectedLeftId: String?,
    val completedPairIds: Set<String>
)
```

Do not mutate the original game content to represent progress.

## 13. UX Principles

The target user is a young child.

Therefore:

- large touch targets;
- very little text;
- images are the primary interaction mechanism;
- clear visual feedback;
- forgiving interactions;
- no complex menus;
- no unnecessary confirmation dialogs;
- no time pressure;
- wrong answers should not feel punitive.

Sounds, animation, haptics and richer visual feedback can be expanded in later iterations.

## 14. Explicitly Out of Scope

Do not implement yet:

- backend;
- networking;
- login;
- profiles;
- cloud synchronization;
- parental dashboard;
- progress history;
- achievements;
- scoring;
- leaderboards;
- ads;
- purchases;
- configurable difficulty;
- content download;
- content categories;
- content topics;
- sound-type categories;
- additional content-selection screens beyond the What's That? game list;
- complex animation framework.

Do not build abstractions solely for these future features.

## 15. Future Content Organization

Later versions may introduce additional organization such as:

- categories;
- topics;
- difficulty;
- sound types;
- themed groups;
- selectable content collections.

These are intentionally not modeled in the MVP.

They should be introduced only when a concrete product requirement makes them necessary.

## 16. Initial Definition of Done

The first usable version is complete when:

1. The app launches into the game list.
2. The list contains What's That? and Matching.
3. Each game has a title and cover image.
4. What's That? opens Game Selection, where the Animals row starts built-in gameplay.
5. Matching opens directly into gameplay.
6. What's That? supports multiple local rounds.
7. Each What's That? round can play and replay one supplied audio asset.
8. Each What's That? round displays two image choices.
9. The child can select an image.
10. Correct and incorrect selections produce clear feedback.
11. Matching displays multiple supplied image pairs.
12. The child can complete pairs using tap-to-match.
13. The game detects when all pairs are complete.
14. Game content is loaded through a repository abstraction.
15. Game content is not hard-coded inside Compose UI.
16. Domain models do not depend on Android persistence classes.
17. Domain models do not depend on Android resource IDs.
18. Images and audio are stored as files/resources rather than Room blobs.
19. Structured metadata may use JSON or Room independently of binary asset storage.
20. No category/topic/sound-mode abstraction is required for the MVP.
