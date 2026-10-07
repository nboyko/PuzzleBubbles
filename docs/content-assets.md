# Puzzle Bubbles — Content & Asset Specification

This document defines how Puzzle Bubbles assets are created, stored, named, and referenced.

It covers:

- images;
- sounds;
- text;
- game cover images;
- asset identifiers;
- local storage conventions;
- future migration to backend-hosted content.

The first iteration intentionally does **not** define categories, topics, sound modes, or other content taxonomies.

## 1. General Principles

Game content must not be hard-coded directly inside Compose screens.

All reusable assets should have stable asset IDs.

Examples:

```text
cat-image
dog-image
pig-image
cat-audio
matching-cover
choose-it-cover
```

The UI should work with asset IDs and domain models rather than assuming particular filenames or Android resource names.

Asset IDs should remain stable even if the physical file location later changes.

For example:

```text
cat-image
```

may initially resolve to:

```text
drawable-nodpi/cat.webp
```

and later to:

```text
https://cdn.puzzlebubbles.app/images/cat.webp
```

without changing game definitions.

## 2. Image Types

For the MVP, there are only two practical image roles.

### 2.1 Game Cover Images

These are shown on the first screen to represent the available games.

Examples:

```text
choose_it.png
matching.png
```

Source location:

```text
assets/source/images/game-covers/
```

### First-slice cover exception

The updated home design uses the existing artwork in
`assets/source/images/game-types/whats_that.png` and
`assets/source/images/game-types/match_up.png`. Preserve these square master files,
including their bubbles and existing backgrounds. Each complete square image
fits above its colored title bar. The title bar must not overlap or crop artwork.

Runtime exports use WebP quality 90, a maximum dimension of 512 px, and the
original aspect ratio. They live in `app/src/main/res/drawable-nodpi/`:

| Stable asset ID | Runtime file |
| --- | --- |
| `choose-it-cover` | `choose_it.webp` |
| `matching-cover` | `matching.webp` |

This exception applies only to these two existing game covers. Source folders
do not introduce categories or content taxonomies into the app.

### 2.2 Gameplay Images

These are images used inside the games.

Examples:

```text
cat
dog
pig
rabbit
carrot
nest
banana
```

Source location:

```text
assets/source/images/gameplay/
```

The MVP does not require semantic folders such as:

```text
animals/
food/
homes/
vehicles/
```

Those may be introduced later if they become useful for content organization.

## 3. Image Format

### Source Format

Master images should use:

```text
1024 × 1024 px
1:1 aspect ratio
transparent background
centered subject
approximately 10–15% padding
no text
no decorative frame
```

The complete subject should fit inside the image.

Avoid:

- cropped ears, tails, legs, etc.;
- unnecessary scenery;
- backgrounds;
- borders;
- shadows that extend far outside the subject;
- text embedded in the image.

The visual style should be reasonably consistent across the asset library.

### Android Runtime Format

Preferred:

```text
512 × 512 px
WebP
transparent background where appropriate
```

The `1024 × 1024` version remains the master source.

For raster game content that Compose sizes explicitly in `dp`, prefer:

```text
app/src/main/res/drawable-nodpi/
```

Example:

```text
source:
assets/source/images/gameplay/cat.png

runtime:
app/src/main/res/drawable-nodpi/cat.webp
```

## 4. Audio

For the MVP, audio is treated generically.

The game does not need to know whether a sound is:

- a spoken word;
- an animal sound;
- a vehicle sound;
- music;
- another audio cue.

The game only needs a stable sound asset ID.

Example:

```text
cat-audio
```

The relationship between that sound and the correct image is defined by game data.

### Source Format

Keep master audio files as:

```text
WAV
mono
44.1 kHz or 48 kHz
16-bit PCM
```

For short prompts, keep audio concise and easy to recognize.

### Runtime Format

Preferred Android format:

```text
OGG Vorbis
mono
44.1 kHz
approximately 96–128 kbps
```

Example:

```text
source:
assets/source/audio/cat.wav

runtime:
app/src/main/res/raw/cat.ogg
```

### Audio Quality Rules

Audio should:

- start quickly;
- contain little or no leading silence;
- contain little unnecessary trailing silence;
- avoid unnecessary background music unless music itself is the intended prompt;
- have minimal background noise;
- have reasonably consistent perceived volume across assets.

A practical loudness target is around:

```text
-16 LUFS
peak below -1 dBFS
```

Exact mastering values are less important than consistent perceived volume.

### Playback

For Choose It:

- play the supplied sound once when the round starts;
- provide a large replay button;
- do not continuously loop the sound.

## 5. Words and Text

Words should normally remain separate from gameplay images.

Do not embed text into ordinary gameplay artwork unless a future game specifically requires text to be part of the task.

Normal UI strings should use Android string resources.

Game logic must not depend on displayed text.

## 6. Asset IDs

Every reusable asset should have a stable identifier.

Examples:

```text
cat-image
dog-image
pig-image

cat-audio
dog-audio
pig-audio

choose-it-cover
matching-cover
```

Prefer descriptive IDs.

Avoid:

```text
image-001
sound-002
asset3
```

unless the content is genuinely anonymous.

File names and IDs may be similar, but they serve different purposes.

Example:

```text
Asset ID:
cat-audio

File:
cat.ogg
```

## 7. Recommended Repository Structure

Keep source/master assets separate from Android runtime assets.

Recommended structure:

```text
puzzle-bubbles/
│
├── AGENTS.md
│
├── docs/
│   ├── mvp-spec.md
│   └── content-assets.md
│
├── assets/
│   └── source/
│       ├── images/
│       │   ├── game-covers/
│       │   │   ├── choose_it.png
│       │   │   └── matching.png
│       │   └── gameplay/
│       │       ├── cat.png
│       │       ├── dog.png
│       │       └── ...
│       │
│       └── audio/
│           ├── cat.wav
│           ├── dog.wav
│           └── ...
│
└── app/
    └── src/
        └── main/
            └── res/
                ├── drawable-nodpi/
                ├── raw/
                └── values/
```

The `assets/source/` directory contains original content.

Android resource directories contain only files needed by the application at runtime.

## 8. Android Runtime Storage

### Images

Store optimized raster images in:

```text
app/src/main/res/drawable-nodpi/
```

This includes:

- game cover images;
- gameplay images.

Example:

```text
choose_it.webp
matching.webp
cat.webp
dog.webp
pig.webp
```

### Sounds

Store optimized audio files in:

```text
app/src/main/res/raw/
```

Example:

```text
cat.ogg
dog.ogg
pig.ogg
```

### Text

Store normal UI/localized strings using Android resources:

```text
app/src/main/res/values/strings.xml
```

## 9. Room and Binary Assets

Do **not** store image or audio binaries as Room blobs.

Room may be used for structured information such as:

- game definitions;
- rounds;
- matching relationships;
- asset metadata;
- progress/state that genuinely needs persistence.

Binary files themselves should live outside Room.

For the initial version they are Android resources.

A future version may use:

```text
Room metadata
      ↓
asset ID
      ↓
local file / cache / CDN URL
```

## 10. Asset Manifest

As the number of assets grows, maintain an asset manifest or equivalent structured registry.

Example:

```json
{
  "id": "cat-image",
  "type": "image",
  "localResource": "cat"
}
```

```json
{
  "id": "cat-audio",
  "type": "audio",
  "localResource": "cat"
}
```

Game definitions should reference logical IDs rather than Android filenames.

## 11. Choose It Content Example

```json
{
  "id": "round-001",
  "soundAssetId": "cat-audio",
  "choiceImageAssetIds": [
    "cat-image",
    "dog-image",
    "pig-image"
  ],
  "correctChoiceId": "cat-image"
}
```

The game does not care what kind of audio `cat-audio` contains.

It only knows that this round explicitly maps that audio to `cat-image`.

## 12. Matching Content Example

```json
{
  "pairs": [
    {
      "leftImageAssetId": "rabbit-image",
      "rightImageAssetId": "carrot-image"
    },
    {
      "leftImageAssetId": "monkey-image",
      "rightImageAssetId": "banana-image"
    }
  ]
}
```

The game does not need concepts such as `animal`, `food`, `home`, or `category`.

It only needs image pairs.

## 13. Future Backend Compatibility

When content moves to a backend, preserve the same logical IDs.

Local:

```text
cat-image -> drawable-nodpi/cat.webp
```

Remote:

```text
cat-image -> https://cdn.example.com/images/cat.webp
```

Local:

```text
cat-audio -> raw/cat.ogg
```

Remote:

```text
cat-audio -> https://cdn.example.com/audio/cat.ogg
```

Game definitions should not need to change.

## 14. Initial Asset Rules Summary

### Image

```text
Master: 1024×1024
Runtime: 512×512 WebP
1:1
transparent
centered
10–15% padding
no text
```

### Audio

```text
Master: WAV
Runtime: OGG
mono
44.1 kHz
96–128 kbps
trimmed silence
consistent volume
```

### MVP Content Model

```text
Choose It:
sound -> several images -> one correct image

Matching:
image <-> image
image <-> image
image <-> image
```

No category or content-taxonomy model is required for the first iteration.
