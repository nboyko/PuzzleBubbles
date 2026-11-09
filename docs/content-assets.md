# Puzzle Bubbles — Content & Asset Conventions

Assets are organized by their use, not by semantic categories. Source files are
editable masters; Android resources contain only assets used by the application.
Game definitions reference stable asset IDs, not paths or resource identifiers.
See [README.md](../README.md) for application behavior and architecture.

## Source structure

```text
assets/source/
├── images/
│   ├── game-covers/
│   │   ├── whats_that.png
│   │   ├── match_it_up.png
│   │   └── bubbles-bg.png
│   └── gameplay/
│       ├── cat.png
│       ├── dog.png
│       └── ...
├── icons/
│   ├── back_arrow.svg
│   ├── sound.svg
│   ├── library.svg
│   ├── play.svg
│   ├── games.svg
│   ├── trophy.svg
│   └── ...
└── audio/
    ├── content/
    │   ├── cat.wav
    │   ├── dog.wav
    │   └── ...
    └── sfx/
        ├── pop.wav
        ├── game_complete.wav
        └── ...
```

This defines the folder roles and naming convention, not a list of required assets.
Keep these folders flat for the MVP. Do not add semantic image categories or
nested icon folders without a concrete need.

| Folder | Role |
| --- | --- |
| `images/game-covers/` | Large artwork for Home game-selection cards |
| `images/gameplay/` | Images used inside game mechanics |
| `icons/` | Reusable UI controls, stored as editable SVG sources |
| `audio/content/` | Audio belonging to game content/question data |
| `audio/sfx/` | UI/gameplay feedback sounds, separate from content data |

Current source inventory: two PNG covers, the supplied completion background
`images/game-covers/bubbles-bg.png`, 15 PNG gameplay images, sixteen SVGs (including
the logo, Photos, close, plus, microphone, pause, stop, delete, replace, and edit icons), ten MP3 prompts in `audio/content/`, and the effects
`audio/sfx/pop.wav` and `audio/sfx/yay.mp3`. Preserve unreferenced
gameplay source images; source inventory does not imply that every image is
already packaged or used by a game.

## Android runtime structure

```text
app/src/main/res/
├── drawable/
│   ├── ic_back_arrow.xml
│   ├── ic_sound.xml
│   ├── ic_library.xml
│   ├── ic_play.xml
│   ├── ic_games.xml
│   ├── ic_trophy.xml
│   └── ...
├── drawable-nodpi/
│   ├── game_cover_whats_that.webp
│   ├── game_cover_match_it_up.webp
│   ├── cat.webp
│   ├── dog.webp
│   └── ...
└── raw/
    ├── content_cat.ogg
    ├── content_dog.ogg
    ├── sfx_pop.ogg
    ├── sfx_game_complete.ogg
    └── ...
```

These are preferred formats/names for new exports. Retain working assets without
unnecessary re-encoding or duplicate exports. Never change an extension without
converting the actual format.

The current runtime contains two WebP covers, ten 512px gameplay PNGs,
the unchanged 887 × 1774 completion background `bubbles_bg.png`, sixteen UI/logo vectors,
ten `content_*.mp3` prompts, `sfx_pop.wav`, and `sfx_game_complete.mp3`. These
format/size exceptions are intentional. Existing launcher assets remain in
Android's drawable/mipmap folders.

| Stable asset ID | Current source | Current runtime |
| --- | --- | --- |
| `whats-that-cover` | `images/game-covers/whats_that.png` | `drawable-nodpi/game_cover_whats_that.webp` |
| `matching-cover` | `images/game-covers/match_it_up.png` | `drawable-nodpi/game_cover_match_it_up.webp` |
| `cat-image` | `images/gameplay/cat.png` | `drawable-nodpi/cat.png` |
| `cat-audio` | `audio/content/cat.mp3` | `raw/content_cat.mp3` |
| `sfx-pop` | `audio/sfx/pop.wav` | `raw/sfx_pop.wav` |
| `sfx-game-complete` | `audio/sfx/yay.mp3` | `raw/sfx_game_complete.mp3` |
| `completion-background` | `images/game-covers/bubbles-bg.png` | `drawable-nodpi/bubbles_bg.png` |
| `ui-back` | `icons/back_arrow.svg` | `drawable/ic_back_arrow.xml` |
| `ui-sound` | `icons/sound.svg` | `drawable/ic_sound.xml` |
| `ui-library` | `icons/library.svg` | `drawable/ic_library.xml` |
| `ui-image` | `icons/image.svg` | `drawable/ic_image.xml` |
| `ui-close` | `icons/close.svg` | `drawable/ic_close.xml` |
| `ui-plus` | `icons/plus.svg` | `drawable/ic_plus.xml` |
| `app-logo` | `icons/bubbles_logo.svg` | `drawable/ic_bubbles_logo.xml` |
| `ui-play` | `icons/play.svg` | `drawable/ic_play.xml` |
| `ui-pause` | `icons/pause.svg` | `drawable/ic_pause.xml` |
| `ui-stop` | `icons/stop.svg` | `drawable/ic_stop.xml` |
| `ui-microphone` | `icons/microphone.svg` | `drawable/ic_microphone.xml` |
| `ui-delete` | `icons/delete.svg` | `drawable/ic_delete.xml` |
| `ui-replace` | `icons/replace.svg` | `drawable/ic_replace.xml` |
| `ui-edit` | `icons/edit.svg` | `drawable/ic_edit.xml` |
| `ui-games` | `icons/games.svg` | `drawable/ic_games.xml` |
| `ui-trophy` | `icons/trophy.svg` | `drawable/ic_trophy.xml` |

Source paths in this table are relative to `assets/source/`; runtime paths are
relative to `app/src/main/res/`. Other packaged gameplay items follow the same
item-name convention. Resolvers map IDs to resources; filenames do not determine
correct answers. Renaming a resource must not change domain IDs or relationships.

## Images

- Keep raster masters, normally 1024 × 1024 PNG, outside Android resources.
- Gameplay artwork should be square, centered, and visually consistent, with
  roughly 10–15% padding and a transparent background where appropriate. Avoid
  cropped subjects, embedded text, decorative frames, and unnecessary scenery.
- Game covers retain their existing artwork/backgrounds. Show the complete square
  above the card's title bar; the bar must not crop or overlap the artwork.
- Prefer runtime images up to 512 × 512 WebP under `drawable-nodpi/`, preserving
  aspect ratio and transparency. Current cover exports use WebP quality 90.
  Keep existing gameplay PNGs unless an optimization is separately needed.
- Do not convert large raster artwork into vectors.

## Icons: SVG → Vector Drawable

Keep editable sources directly under `assets/source/icons/`. Export/import
supported paths into Android Vector Drawables in `res/drawable/`, for example:

```text
assets/source/icons/library.svg
    → app/src/main/res/drawable/ic_library.xml
```

Use lowercase snake_case filenames and the `ic_` runtime prefix. Preserve the
viewport, aspect ratio, paths, colors, and strokes. Check the Android rendering
against the source at the actual control size. Unsupported SVG filters, masks,
or other effects require a practical Android-compatible raster fallback; preserve
the original SVG and do not force a lossy vector approximation.

Control SVGs use colorable `currentColor` strokes and fills. Their Android exports
use white shapes as a tint/gradient mask; the logo retains its source colors.
The Photos icon exports directly to `ic_image.xml` without unsupported effects.
Preserve path transforms as well as geometry.
Compose supplies button colors and optional neon treatment. The completion trophy
uses a yellow-gold gradient. Blur-based halos render on Android 12+; older devices
retain the sharp colored shapes. Decorative bubble artwork is still drawn in Compose.

## Audio

- Content audio is associated with targets/rounds through stable IDs such as
  `cat-audio`. The engine does not need to interpret the sound.
- SFX such as `sfx-pop` are application feedback, not round/domain content.
- Prefer new source masters as mono WAV, 44.1 or 48 kHz, 16-bit PCM.
- Prefer new Android exports as mono OGG Vorbis, 44.1 kHz, around 96–128 kbps.
  Keep supplied MP3 prompts without another lossy transcode. Keep the existing
  trimmed runtime WAV effect; its full source is `audio/sfx/pop.wav`.
- Trim unnecessary leading/trailing silence from runtime exports, avoid clipping
  and noise, and keep perceived volume consistent. A practical mastering target
  is about −16 LUFS with peaks below −1 dBFS.
- Keep source and optimized runtime copies separate; they serve different roles.
  Do not store binary audio or images in Room.

## Text and metadata

### User-imported photo storage convention

Imported Library photos use private persistent storage at
`filesDir/library/images/<photo-id>.webp`, separate from bundled assets and
editable repository sources. Keep only the normalized 1024 × 1024 WebP; do not
copy imported originals into `assets/source/` or `res/`. Respect EXIF orientation
before the square crop editor; the selected crop is encoded as lossy WebP at quality 85.

Generate an app-owned stable photo ID. Store only ID, internal filename, and
creation time in Room (`photos.db`); never image blobs or a gallery URI as the canonical asset.
The stable rendering ID is `library-photo:<photo-id>`. Unsaved previews stay in memory.
Resolve IDs through the data/rendering boundary rather than exposing paths,
URIs, or Room entities to screens/domain models. Temporary staging files are not
Library entries and must be cleaned after cancellation/failure or interruption.
Reconcile file/metadata consistency before exposing persisted photos. Saved-photo
deletion removes the metadata first, then the owned image file; reconciliation
cleans up any unreferenced file after a failed/interrupted removal. Never delete
the source gallery photo or bundled artwork. Imported files and their database
are excluded together from cloud backup and device transfer. Bundled asset IDs,
formats, and source/runtime folder conventions above remain unchanged.

### Parent-recorded sound storage convention

Library recordings are private user content at
`filesDir/library/audio/<sound-id>.m4a`, separate from bundled assets. Capture
mono AAC/MPEG-4 with a five-second recording limit (currently 44.1 kHz, 64 kbps).
The recorder maximum and UI deadline remain 5,000 ms. Permit at most 100 ms of
native output overhead: validate positive actual duration up to 5,100 ms before
Preview/Save and during reconciliation. Keep the actual duration in metadata
and playback; reject longer output without trimming, transcoding, or clamping.
The only re-encode is loudness normalization of the draft before Preview: decode,
apply one gain so the active RMS (50 ms windows within 20 dB of the loudest)
reaches the common target 0.08 (about -22 dBFS), limited to -1 dBFS peaks and
+20 dB, then write the same AAC/MPEG-4 format. Its output must pass the same
duration validation; otherwise the original draft is kept.

Bundled audio uses the same measurement: each asset is decoded once at startup
and played at the volume that brings it down to the target (effects 6 dB lower).
Playback cannot boost, so bundled exports must not be quieter than the target;
an instrumented test enforces this for every asset.
Do not copy recordings into `assets/source/` or Android resources, transcode them
into bundled formats, or store binary audio in Room. Bundled audio conventions
above remain unchanged.

Room's `sounds` table in the existing `photos.db` stores ID, internal filename,
display name, actual duration, and creation time. Schema version 2 adds this table
through a non-destructive 1 → 2 migration preserving Photos. Version 3 adds names
and assigns `Track N` defaults to existing recordings in oldest-first order,
preserving audio files and IDs. Display names may repeat and never become filenames.
Screens/domain models expose
stable `library-sound:<sound-id>` asset IDs and metadata, never filesystem paths.

Unsaved recordings use leased `sound-draft:<id>` IDs and temporary `.m4a` files
under `library/audio/drafts/`. Rotation retains the live draft; cancellation,
backgrounding, and startup recovery remove abandoned drafts. Save copies valid
audio to a synced `<id>.m4a.tmp`, promotes it, and commits metadata. Reconciliation
preserves valid committed files and removes incomplete/orphaned output and invalid
entries. Ordinary Save failure preserves a usable draft for explicit retry where
possible; nothing is saved or retried automatically.

Deletion removes metadata first and then the owned file. Failed file cleanup
cannot resurrect the entry; subsequent reconciliation retries removal. Never
delete bundled audio or unrelated files. Existing legacy-backup, cloud-backup,
and device-transfer exclusions already cover the whole `library/` subtree and
`photos.db` with its journal/WAL/SHM files, excluding recordings and metadata together.

### Custom-game references

Room schema version 4 adds `custom_games`, `game_images`, and `game_questions`
through an additive 3 → 4 migration. A saved game stores its generated
`whats-that-<UUID>` ID, name, creation time, ordered photo pool, and explicit
question photo/sound IDs. Transactions commit these together; foreign keys and
repository checks reject missing references and protect media from deletion.
Game deletion releases only its metadata/references and keeps Library files.
Editing updates the same game row's name and replaces its associations within
one transaction, retaining its ID and creation time. Removed media references
are released only on commit; failures roll back the old game and references.
Editing adds no schema change beyond version 4 and never copies media files.
Referenced photos remain crop-editable with the same stable ID. No binary data,
resource IDs, file paths, or duplicated media are stored in the game definition.
The existing database and `library/` backup/transfer exclusions cover games and
their media together.

### Bundled content

Normal UI text lives in `res/values/strings.xml`, separate from gameplay artwork.
Current local content is a Kotlin preset exposed through `GameRepository`.
Correct choices and matching relationships must be explicit in game data.
Presentation randomization must not define correctness. Stable IDs remain
independent of physical storage; asset cleanup does not require a new repository,
manifest system, persistence layer, or content taxonomy.
