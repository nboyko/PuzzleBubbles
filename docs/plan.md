# What's That? — Game Specification

Status: **Implementation verified** on 2026-10-14. This plan includes the approved
animation and visual refinements. Iteration closure still requires the user's
explicit confirmation; the next feature has not been selected.

## 1. Overview

**What's That?** is the first playable game in Puzzle Bubbles.

The child hears an audio prompt and chooses the matching image from two image options.

The game does not need to understand what kind of audio it is. It may be a spoken word, animal sound, transport sound, song, or another audio cue.

The mechanic only needs to know:

```text
play this sound
show these two images
this image is correct
```

No categories, topics, sound modes, difficulty levels, or other content classification are required for the first iteration.

---

## 2. Session Structure

A complete session contains **10 rounds**.

The game uses a local preset of **10 target items**.

Each target item contains:

- one image;
- one audio asset.

Example:

```text
cat
├── image: cat-image
└── audio: cat-audio
```

For the first iteration:

- each of the 10 target items appears exactly once;
- target order is shuffled when the session starts;
- each round contains exactly two image choices;
- one image is correct;
- one image is an incorrect distractor selected from another target item;
- correct and incorrect image positions are randomized.

The game must not infer correctness from filenames, labels, or screen position.

---

## 3. Entering the Game

When the child selects **What's That?** from the Home screen:

1. navigate directly to the game screen;
2. create the 10-round session;
3. display the first round immediately;
4. automatically play the current round's audio.

There is no intermediate configuration or category-selection screen.

---

## 4. Screen Layout

The screen contains three main areas.

### Top Bar

Contains:

- Back button;
- title: **What's That?**

Use the shared `BubbleButton` for Back: borderless, without neon, in the same blue
as the title. UI icon sources are colorable SVG shapes with Android vector exports.

Back returns directly to the Home screen.

No confirmation dialog is required.

### Gameplay Area

The main area occupies most of the screen.

It contains two large image choices.

Each image is displayed inside a large **bubble**, consistent with the Puzzle Bubbles visual style.

Requirements:

- large touch targets;
- the entire bubble is tappable;
- generous spacing between choices;
- minimal text;
- toddler-friendly layout.

### Bottom Audio Control

The bottom area contains a large replay button.

The replay control should follow the bubble visual language, for example:

```text
bubble
  └── speaker icon
```

Pressing the button replays the current round's audio.

Use a circular `BubbleButton` with a thin border and restrained neon glow, in the
same blue as the game title. Shared buttons support single/multiple colors,
optional icon/text content, and independent border and neon options.

If the audio is already playing, restart it instead of playing multiple simultaneous copies.

---

## 5. Round Flow

```text
round appears
    ↓
audio plays automatically
    ↓
child chooses an image
    ├── wrong → error feedback → same round continues
    └── correct → success animation → next round
```

When a new round appears, its audio plays automatically.

There is no **Next** button.

---

## 6. Wrong Answer Feedback

When the child taps the incorrect image:

1. keep the current round active;
2. animate the selected bubble with a short, gentle shake/wobble;
3. show several small decorative bubbles around it;
4. return the selected bubble to normal;
5. allow another attempt immediately.

Suggested animation duration:

```text
250–400 ms
```

The feedback should feel playful rather than punitive.

Do not use:

- large red X;
- harsh error colors;
- loud buzzer;
- “Wrong!” text;
- score penalties.

A separate wrong-answer sound effect is not required for the first iteration.

Repeated taps must not start overlapping copies of the error animation.

---

## 7. Correct Answer Feedback

When the child taps the correct image:

1. disable additional answer taps temporarily;
2. hide the incorrect choice immediately;
3. start moving the selected bubble toward the center, growing it, and playing
   the success/pop sound together;
4. center it over about 450 ms and grow it over about 1.2 seconds, capped at
   130% of its original size and constrained to the gameplay area's bounds;
5. wait for both the growth and the complete sound;
6. briefly fade/pop the selected bubble over about 140 ms, then continue to the next round.

Visual sequence:

```text
selected bubble; other choice disappears
    ↓
centering + bounded growth + success sound start together
    ↓
animation and audio finish
    ↓
brief fade/pop → next round
```

Suggested duration:

```text
roughly 1.2 seconds of growth, followed by a 140 ms fade;
longer if needed to finish the sound
```

The animation should originate from the correct bubble so the feedback reinforces which answer was correct.

Do not add yet:

- confetti;
- score;
- stars;
- textual praise.

---

## 8. Next Round Transition

After the correct bubble pops:

1. load the next round;
2. display the next two bubbles;
3. optionally use a short entrance animation;
4. automatically play the new round's audio.

A simple entrance animation may be:

```text
slightly smaller + transparent
        ↓
normal size + visible
```

Suggested duration:

```text
200–300 ms
```

---

## 9. Audio

There are two different audio roles.

### Content Audio

Content audio belongs to the round data.

Examples:

```text
cat-audio
dog-audio
pig-audio
```

The game does not care whether this contains:

- a spoken word;
- an animal sound;
- a transport sound;
- a song;
- another audio cue.

### Gameplay Sound Effects

Gameplay SFX are application feedback and are separate from round content.

Initial examples:

```text
sfx_pop
sfx_game_complete
```

The pop sound can also serve as the success sound for a correct answer.

A separate generic success sound is not required.

---

## 10. Game Completion

After round 10 is completed correctly:

1. finish the normal correct-answer pop animation;
2. do not create another round;
3. show the success screen;
4. keep it visible for **3 seconds**;
5. automatically return to Home.

---

## 11. Success Screen

The first version of the success screen should remain simple.

Use the supplied `bubbles-bg.png` artwork as the full-screen background. Place
the trophy/cup vector inside its large central bubble, with a yellow-gold gradient
and restrained neon halo. Keep the icon aligned with that bubble as the
background scales/crops to the screen.

Animate it using a continuous **pulse / breathing animation**:

```text
normal size
    ↓
slightly larger
    ↓
normal size
    ↓
repeat
```

There is no separate game-complete sound in this slice; the final correct answer
already plays the success/pop sound. An additional completion effect remains optional.

Do not add yet:

- score;
- stars;
- statistics;
- achievements;
- replay button;
- progress history.

The final visual design of this screen can be refined later.

---

## 12. Interaction State

Gameplay state should explicitly prevent conflicting interactions caused by rapid tapping.

Conceptually:

```text
WAITING_FOR_ANSWER
    ├── wrong
    │      ↓
    │  WRONG_FEEDBACK
    │      ↓
    │  WAITING_FOR_ANSWER
    │
    └── correct
           ↓
       CORRECT_FEEDBACK
           ↓
       NEXT_ROUND
```

After the final round:

```text
CORRECT_FEEDBACK
    ↓
GAME_COMPLETE
    ↓
3 seconds
    ↓
HOME
```

Important behavior:

- prevent double-processing of correct answers;
- prevent overlapping wrong-answer animations;
- prevent multiple audio instances from piling up;
- clean up audio, timers, and coroutines when leaving the screen.

---

## 13. Suggested Content Model

The first iteration only needs a small model.

```kotlin
data class WhatsThatItem(
    val id: String,
    val imageAssetId: String,
    val audioAssetId: String
)
```

A generated round may conceptually look like:

```kotlin
data class WhatsThatRound(
    val targetItemId: String,
    val correctImageAssetId: String,
    val wrongImageAssetId: String,
    val audioAssetId: String
)
```

Static content and gameplay/session state should remain separate.

---

## 14. Initial Content

The MVP uses a local preset of 10 items.

No backend, Room database, remote loading, or content-management system is required for this iteration.

Assets may later be replaced without changing game logic as long as stable asset IDs are preserved.

---

## 15. Out of Scope

Do not implement yet:

- categories;
- topics;
- sound types or modes;
- difficulty levels;
- scoring;
- progress tracking;
- achievements;
- user profiles;
- backend/networking;
- remote content;
- analytics;
- configurable number of choices;
- more than two image choices;
- adaptive learning;
- Match It Up gameplay.

---

## 16. Definition of Done

The first What's That? implementation is complete when:

1. What's That? opens directly from Home.
2. A 10-round session is created.
3. All 10 target items appear once per session.
4. Round order is randomized.
5. Every round shows exactly two image bubbles.
6. One image is correct and one is incorrect.
7. Image positions are randomized.
8. Audio plays automatically when each round starts.
9. Audio can be replayed.
10. Wrong selection triggers shake + small-bubble feedback.
11. Wrong selection does not advance the round.
12. Correct selection triggers the grow-and-pop animation.
13. Correct feedback cannot be double-triggered.
14. The pop sound plays with the correct-answer animation.
15. The next round starts automatically.
16. After round 10, the success screen appears.
17. The trophy/cup uses a continuous pulse animation.
18. The success screen stays visible for 3 seconds.
19. The app automatically returns Home afterward.
20. Back navigation works correctly.

## Verification outcome

Debug assembly, six unit tests, seven device tests, and lint passed (zero errors,
18 non-critical warnings). Reviewed session/data separation, resource mappings,
audio disposal, feedback cancellation, and completion timing. Emulator checks
covered rapid replay/answer taps, Back, all ten sequential rounds with two choices,
the gold trophy completion state, and automatic return Home.

This verification used the medium-phone emulator. Broader device/API and enlarged
text visual coverage remain optional follow-up checks; no required functionality
from this step was deferred. Match Up gameplay remains outside this plan.
