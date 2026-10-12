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
│   │   └── match_it_up.png
│   └── gameplay/
│       ├── cat.png
│       ├── dog.png
│       └── ...
├── icons/
│   ├── back.svg
│   ├── speaker.svg
│   ├── replay.svg
│   ├── settings.svg
│   ├── home.svg
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

Current source inventory: two PNG covers, 15 PNG gameplay images, ten MP3 prompts
in `audio/content/`, and `audio/sfx/pop.wav`. The icons folder is currently empty:
existing control shapes are drawn in Compose. No separate game-complete sound is
currently supplied. Preserve unreferenced gameplay source images; source inventory
does not imply that every image is already packaged or used by a game.

## Android runtime structure

```text
app/src/main/res/
├── drawable/
│   ├── ic_back.xml
│   ├── ic_speaker.xml
│   ├── ic_replay.xml
│   ├── ic_settings.xml
│   ├── ic_home.xml
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
ten `content_*.mp3` prompts, and `sfx_pop.wav`. These format exceptions are
intentional. Existing launcher assets remain in Android's drawable/mipmap folders;
the control Vector Drawable examples above have not yet been created.

| Stable asset ID | Current source | Current runtime |
| --- | --- | --- |
| `choose-it-cover` | `images/game-covers/whats_that.png` | `drawable-nodpi/game_cover_whats_that.webp` |
| `matching-cover` | `images/game-covers/match_it_up.png` | `drawable-nodpi/game_cover_match_it_up.webp` |
| `cat-image` | `images/gameplay/cat.png` | `drawable-nodpi/cat.png` |
| `cat-audio` | `audio/content/cat.mp3` | `raw/content_cat.mp3` |
| `sfx-pop` | `audio/sfx/pop.wav` | `raw/sfx_pop.wav` |

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
assets/source/icons/settings.svg
    → app/src/main/res/drawable/ic_settings.xml
```

Use lowercase snake_case filenames and the `ic_` runtime prefix. Preserve the
viewport, aspect ratio, paths, colors, and strokes. Check the Android rendering
against the source at the actual control size. Unsupported SVG filters, masks,
or other effects require a practical Android-compatible raster fallback; preserve
the original SVG and do not force a lossy vector approximation.

Do not invent unused controls merely to populate the example tree. Existing
Compose-drawn controls remain unchanged by asset-folder cleanup.

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

Normal UI text lives in `res/values/strings.xml`, separate from gameplay artwork.
Current local content is a Kotlin preset exposed through `GameRepository`.
Correct choices and matching relationships must be explicit in game data.
Presentation randomization must not define correctness. Stable IDs remain
independent of physical storage; asset cleanup does not require a new repository,
manifest system, persistence layer, or content taxonomy.
