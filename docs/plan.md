# Manage Existing Custom Games

Verified on 2026-11-08: swipe/accessibility management, single-page editing,
atomic updates, preserved identity/order, discard behavior, and the agreed
icon/color/corner refinements meet this scope. Build, 18 unit tests, lint
(0 errors, 18 warnings), and affected emulator checks passed. Six swipe and
end-to-end checks passed again during finalization. See Iteration 2, Phase 8
for verification details and limitations. No next feature is planned here.

## Goal

Allow parents to manage already-created custom **What's That?** games without adding permanent management controls to every game row.

The normal game row remains focused on its primary purpose:

**Play the game.**

Management actions are revealed by swiping the row from right to left.

---

## 1. Normal Game Row

A custom game initially appears as a simple row.

Conceptually:

```text
┌───────────────────────────────┐
│ 🧩   My Animals               │
└───────────────────────────────┘
```

Use:

- Puzzle icon on the left;
- game name;
- existing Puzzle Bubbles row styling.

Do not permanently show Edit or Delete buttons.
Remove the separate Play icon from both custom and predefined rows. Normal rows
show only the puzzle icon and name; the whole row is the Play target.

---

## 2. Tap Behavior

Tapping the normal row starts the game immediately.

```text
tap game row
    ↓
open What's That? gameplay
```

This remains the primary interaction.

Do not open an edit/details screen when tapping the row.

---

## 3. Swipe Gesture

Allow a custom game row to be swiped:

```text
right → left
```

As the user swipes, the row moves horizontally to the left.

The management actions are revealed underneath it from the right side.

Conceptually:

```text
before

┌───────────────────────────────┐
│ 🧩   My Animals               │
└───────────────────────────────┘


during swipe

┌───────────────────────┐
│ 🧩   My Animals       │ [Edit] [Delete]
└───────────────────────┘


opened

┌───────────────────────┬────────┬────────┐
│ 🧩   My Animals       │  Edit  │ Delete │
│                       │ green  │  red   │
└───────────────────────┴────────┴────────┘
```

The row itself should remain visible.

Do not swipe the row completely off screen.

---

## 4. Swipe Threshold

A small accidental horizontal movement should not open the actions.

Use a reasonable swipe threshold.

If the gesture does not pass the threshold:

```text
row returns to normal position
```

If the threshold is passed:

```text
row settles in the open position
```

The final open position should expose both action buttons completely.

---

## 5. Only One Open Row

Only one game row may have management actions open at a time.

If Game A is open and the user swipes Game B:

```text
Game A closes
Game B opens
```

Tapping elsewhere in the list should close the currently open row.
While any row is open, tapping its main area or another game row only closes
the actions and does not start gameplay. A subsequent tap plays the game.

This prevents multiple rows from remaining partially shifted.

---

# Edit

## 6. Edit Action

The first revealed action is **Edit**.

Use an icon-only pencil action with a very light, almost transparent mint-green
background and a moderately bright, cool green icon that harmonizes with blue.
Do not show a visible Edit text label; retain its accessible Edit game label.
Green Edit and red Delete are deliberate exceptions to the blue palette.
Keep the editor and other game-list controls blue.

Conceptually:

```text
[ Edit ]
 green
```

The pencil icon identifies Edit independently of its color. Text in the diagrams
is a description of the action, not a required visible label.

Pressing Edit opens the existing **Create Game wizard** in editing mode.

---

## 7. Reuse the Existing Wizard

Do not create a separate game editor.

Reuse the same single-page editor already used for game creation:

```text
Game name
Game image pool
Questions
```

The difference is that the wizard is initialized from the saved game.
Use the title Edit Game, with the existing Cancel/Save actions, media-selection
modals, validation, and photo-reference/minimum-selection protections. Do not
introduce steps, a Continue button, or a header Back arrow.

For example:

```text
saved game

name:
My Animals

images:
cat
dog
pig

questions:
cat + cat sound
dog + dog sound
```

opens the wizard with all of those values already selected.

---

## 8. Edit Mode

The wizard should know whether it is handling:

```text
Create
```

or:

```text
Edit existing game
```

In Edit mode:

- preserve the game's existing stable ID;
- load its current name;
- load its image pool;
- load all questions;
- allow exactly the same validation rules as during creation.

Do not duplicate the game when Save is pressed.

---

## 9. Saving Changes

When editing an existing game:

```text
Edit
 ↓
change data
 ↓
Save
```

Save updates the existing game definition.
Preserve its original creation time and existing list position: editing an older
game must not make it appear newly created or move it to the top of the list.
Update the game and all media associations in one transaction. Removed references
are released only after Save succeeds; failed Save leaves the stored game intact.
On failure, show an error and keep the editor open. Do not add persistent draft
storage or restoration after the editor is closed or the process is lost. Keeping
the current form visible on failure does not require a separate saved draft.

Keep:

```text
same game ID
```

Replace/update:

```text
name
image associations
questions
```

Do not duplicate photos or sounds.

Continue referencing Library media using their stable IDs.

After Save:

```text
return to What's That? game list
```

The row should immediately reflect changes such as a renamed game.

---

## 10. Cancel Editing

If the parent opens Edit but makes no changes:

```text
Back / Cancel
→ return to game list
```

No confirmation is necessary.
Compare the current values with the originally loaded game. Changes that are
fully reverted must not trigger discard confirmation.

If meaningful changes were made and the parent tries to exit, reuse the same discard protection as game creation.

For example:

```text
Discard changes?

Your changes won't be saved.

Keep Editing     Discard
```

Do not overwrite the saved game until Save succeeds.

---

# Delete

## 11. Delete Action

The second revealed action is **Delete**.

Use an icon-only trash action with a very light, almost transparent rose-red
background and a moderately bright, cool rose icon that harmonizes with blue.
Do not show a visible Delete text label; retain its accessible Delete game label.

Conceptually:

```text
[ Delete ]
   red
```

Use the existing Delete/Trash icon. Text in the diagrams describes the action
rather than a visible label.

Color should reinforce the destructive action, but the icon/action must still be understandable independently of color.

---

## 12. Delete Confirmation

Delete buttons in game, photo, and sound modals use a consistent moderately
bright red, matching the visual strength of blue Save buttons. Keep the pale
icon-only swipe actions unchanged.

Do not delete the game immediately when Delete is tapped.

Show confirmation:

```text
Delete "My Animals"?

This game will be removed.
Your photos and sounds will stay in Library.

Cancel           Delete
```

This distinction is important.

Deleting a custom game removes only:

- custom-game metadata;
- image associations;
- question definitions.

It must **not delete any Library photos or sounds**.

---

## 13. After Delete

After successful deletion:

```text
close confirmation
    ↓
remove game from list
```

The row should disappear immediately.

Any media that was protected from deletion only because of this game should become deletable if no other games reference it.

---

# Default Game

## 14. Built-In Default Game

Animals and every future predefined game cannot be edited or deleted. Identify
predefined games using the existing built-in flag, not a particular name or ID.
References to Default Game below apply to all predefined games.

Preferred behavior:

```text
Default Game
    ↓
tap → Play
```

Do not enable swipe management actions for the Default Game.

Only parent-created custom games support:

```text
swipe → Edit / Delete
```

---

# Gesture and Tap Interaction

## 15. Avoid Gesture Conflicts

Horizontal swipe and normal tap must coexist cleanly.

A horizontal drag should not accidentally launch the game.

A simple tap should not move the row.

Conceptually:

```text
tap
→ Play

horizontal drag
→ reveal actions
```

Once actions are open, tapping any game row closes the actions without launching
gameplay. Only a subsequent tap on a normal row starts a game.

Use consistent behavior across all custom-game rows.

Expose Edit game and Delete game as Android accessibility custom actions on each
custom-game row, so TalkBack users can manage a game without swiping the row.
These actions do not add visible buttons or an overflow menu. Edit opens the
same editor; Delete opens the same confirmation dialog. Predefined rows expose
neither management action. Preserve the row's accessible Play action.

---

## 16. Animation

Use a short natural horizontal animation when:

- opening actions;
- closing actions;
- snapping back after an incomplete swipe.

Avoid springy or exaggerated motion.

The management gesture should feel like a normal list interaction, not like part of the children's gameplay animation language.

---

# Persistence

## 17. Editing

Editing updates the existing persisted custom-game definition.

Do not:

- create a new ID;
- duplicate media;
- create a copy of the game.

---

## 18. Deleting

Deleting removes all persistence records owned by the game:

```text
CustomGame
GameImage associations
Questions
```

Library media remains untouched.

The deletion operation should not leave orphaned game-question or association records.

---

# Explicitly Out of Scope

Do not implement:

- duplicate game;
- reorder games;
- custom covers;
- archive;
- undo delete;
- swipe in both directions;
- additional overflow menus;
- editing the Default Game;
- deleting the Default Game.

Keep management to exactly:

```text
Edit
Delete
```

---

# Acceptance Criteria

The feature is complete when:

1. A custom game row normally shows only its icon and name.
2. Tapping the normal row starts that custom game.
3. Swiping a custom-game row right-to-left moves the row left.
4. The swipe reveals exactly two actions.
5. Edit uses a pencil icon, a very light mint-green background, and no visible text.
6. Delete uses a trash icon, a very light rose-red background, and no visible text.
7. An incomplete swipe returns the row to its original position.
8. A completed swipe settles with both actions visible.
   While the row is shifted, its right corners are square to meet the action
   buttons cleanly. Restore the rounded right corners when fully closed.
9. Only one row may remain open at once.
10. Opening another row closes the previous one.
11. Tapping outside an open row closes it.
12. Horizontal swiping does not accidentally start gameplay.
13. Edit opens the existing custom-game wizard.
14. Edit mode pre-populates the existing game name.
15. Edit mode pre-populates the existing image pool.
16. Edit mode pre-populates all existing questions.
17. Editing uses the same validation rules as creation.
18. Saving edits keeps the same game ID.
19. Saving edits updates the existing game rather than creating another one.
20. Unsaved editing does not modify the stored game.
21. Exiting after meaningful edits asks whether to discard changes.
22. Delete shows a confirmation dialog.
23. Confirmation clearly states that Library photos and sounds are retained.
24. Confirmed deletion removes the custom game.
25. Deleting a game does not delete its Library media.
26. The Default Game cannot be swiped for Edit/Delete.
27. The Default Game remains playable.
28. Existing creation and gameplay functionality remains unaffected.
29. The project builds and relevant tests pass.
30. Neither custom nor predefined rows show a separate Play icon.
31. Edit uses the single-page editor with the title Edit Game.
32. Saving edits preserves creation time and list position.
33. Failed Save shows an error, keeps the editor open, and leaves stored data intact.
34. Removed media references are released only after a successful atomic update.
35. Reverting all edits removes the need for discard confirmation.
36. When a row is open, tapping any game row closes it without starting gameplay.
37. Custom rows expose accessible Edit game and Delete game actions without requiring a row swipe or adding visible controls; predefined rows do not.
38. Accessible Delete opens confirmation, and accessible Edit opens the same editor as the revealed Edit button.

---

# Result

Custom games now have two interaction modes:

```text
TAP
 ↓
PLAY


SWIPE ←
 ↓
┌───────────┬────────┬────────┐
│ Game      │ Edit   │ Delete │
│           │ green  │ red    │
└───────────┴────────┴────────┘
```

This keeps the normal game list visually simple while still giving parents access to management actions when they need them.
