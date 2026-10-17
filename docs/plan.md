# Plan - Library Navigation and Empty Screens

Status: **Verified implementation; awaiting manual iteration closure**.
The agreed requirements and acceptance criteria below are retained. This plan
can be replaced with the next agreed feature after its completion commit.

## Goal

Build a small Library UI slice with a **My Library** landing screen and dedicated
empty Photos and Sounds screens. No sample content or real media management is
included.

## 1. My Library

Use the existing Library destination for the **My Library** screen; do not add an
intermediate landing route.

Show two large cards:

- **Photos**, using the existing image icon source;
- **Sounds**, using the existing sound icon.

Each card has a title, an icon, and a large tappable area that opens its screen.
Reuse the current typography, colors, rounded shapes, and bubble styling.
Do not show counts, sample previews, or inactive Add/Edit controls.

## 2. Photos and Sounds

Each child screen contains:

- a large Back control and its title, **Photos** or **Sounds**;
- an appropriate existing icon;
- a simple empty placeholder: **No photos yet** or **No sounds yet**.

Use string resources for UI text. Keep the layouts clear and suitable for large
text and small screens.

There are no sample photos, sound rows, preview playback controls, or selection
interactions in this step. Collection grids, audio lists, and populated states
can be designed when adding media becomes part of an agreed feature.

## 3. Navigation

Follow the [shared navigation constraints](#shared-navigation-constraints) below.
Keep My Library's title and Back arrow, without a logo, top tabs, or bottom Back
button.

Extend the existing Compose NavHost:

```text
Home <-> My Library
              |-- Photos -- Back --> My Library
              |-- Sounds -- Back --> My Library
```

- The Library navigation item opens My Library through the existing Library route.
- Photos and Sounds keep Back to My Library and the fixed Home/Library navigation
  bar with Library selected. Home returns to Home; Library returns to My Library.
- Visible Back and system Back return from either child to My Library.
- Home/Library switching preserves the existing Home scroll behavior.
- Navigation survives activity recreation without duplicating destinations.
- Existing game navigation and What's That? gameplay remain unaffected.

## 4. Assets and Media IDs

Use stable asset IDs and the existing image-rendering boundary for UI artwork.
Export the existing `assets/source/icons/image.svg` to an Android Vector
Drawable when wiring the Photos icon. Reuse the existing sound icon for Sounds.

Future user-added photos and sounds will receive app-generated unique UIDs,
independent of filenames and Android resource IDs. UID creation belongs to the
later media-addition feature. This empty shell needs no UID generator, sample
models, seeded media, or persistence.

Do not place Android resource IDs, paths, or URLs in screen/domain content models.
Do not change the IDs of existing bundled game assets.

## 5. Implementation Boundaries

Keep this step focused on presentation and navigation.

- Reuse existing buttons, icons, theme, and navigation.
- Share the Photos/Sounds card structure where useful for the two current cards.
- Keep screen-specific layouts in their screens.
- Do not create photo-thumbnail or sound-row components before there is content
  to display.
- Do not introduce user-media repositories, generic media-management abstractions,
  Room, filesystem storage, or new dependencies.

Before implementation, align the relevant Library paragraph of
`docs/mvp-spec.md` with this agreed shell scope. Actual media management remains
deferred.

## 6. Out of Scope

- Sample content and sample sound playback.
- Adding, importing, recording, selecting, cropping, resizing, editing, or
  deleting media.
- Add Photo/Add Sound controls and their behavior.
- Photo/audio permissions and Android Photo Picker.
- UID generation, databases, and file persistence.
- User-created games, parental gates, networking, and backend services.
- Changes to either game mechanic.

## 7. Verification and Acceptance

The implementation is ready for finalization review when:

1. My Library contains visually consistent Photos and Sounds cards.
2. Each whole card opens its dedicated destination.
3. Photos shows its title, Back control, icon, and empty placeholder.
4. Sounds shows its title, Back control, icon, and empty placeholder.
5. Both media screens display fixed Home/Library navigation with Library selected.
6. Visible Back and system Back return from both children to My Library.
7. Home/Library switching works and the bottom navigation stays visible while scrolling.
8. Navigation survives activity recreation and Home scroll restoration still works.
9. No sample media, playback controls, inactive media actions, permissions,
   UID-generation logic, storage, or new dependencies are introduced.
10. Existing What's That? gameplay remains unaffected.
11. The project builds, lint succeeds, and relevant existing tests pass.

Extend the existing focused navigation tests for the two child destinations,
Back behavior, and recreation. Manually check the empty layouts on a small screen
and with enlarged text. Do not add screenshot-test infrastructure for this step.

## 8. Finalization and Completion

Passing checks or making a regular commit does not mark the feature or iteration
finished. Run final verification, documentation synchronization, and iteration
reconciliation only through an explicit `$finalize-iteration` invocation.
Feature/iteration completion still requires the user's manual confirmation.

## Shared navigation constraints

Home and Library use a fixed, full-width bottom navigation bar without top tabs
or a floating container. Reuse the existing games and library icons with small,
matching-color labels. The active icon sits in a raised soap bubble within a
close-fitting, smoothly rounded cutout. Touch feedback affects only the icon
and label, with no background ripple. Keep the bar visible while scrolling,
preserve Home scroll restoration, and show it on Photos/Sounds with Library
selected. Hide it inside games and task screens such as photo preview. Keep the two current destinations; do not add future items.

## Verified outcome

My Library provides Photos and Sounds cards, dedicated empty destinations, and
Back to My Library. Home/Library navigation remains fixed at the bottom on Home,
My Library, Photos, and Sounds. Stable icon IDs resolve through the existing rendering boundary.
No media data, storage, permissions, dependencies, or game mechanics were added.

Fresh debug build, six unit tests, 15 device tests, and lint passed (zero errors,
17 non-blocking warnings). Device checks cover Back, recreation, Home scroll
restoration, existing game/audio behavior, and empty layouts at 320dp wide with
150% text. No required work in this Library shell is deferred. Broader API/device
coverage remains unverified. Iteration 1 stays open pending explicit acceptance.
