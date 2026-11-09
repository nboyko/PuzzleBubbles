package eu.bfss.puzzlebubbles.presentation.games.whatsthat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import eu.bfss.puzzlebubbles.R
import eu.bfss.puzzlebubbles.domain.*
import eu.bfss.puzzlebubbles.presentation.components.BubbleButton
import eu.bfss.puzzlebubbles.presentation.components.AssetImage
import eu.bfss.puzzlebubbles.presentation.components.PhotoAssetImage
import eu.bfss.puzzlebubbles.ui.theme.GameTitleFontFamily
import eu.bfss.puzzlebubbles.ui.theme.HomeHeadingFontFamily
import eu.bfss.puzzlebubbles.ui.theme.PuzzleBubblesTheme

private val WizardAccent = Color(0xFF166CAF)
private val WizardInk = Color(0xFF18345B)

@Composable
fun CreateGameScreen(model: CreateGameViewModel, onExit: () -> Unit) {
    val photos by model.photos.collectAsState()
    val sounds by model.sounds.collectAsState()
    val playback by model.playback.collectAsState()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, model) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) model.stopPreview() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); model.stopPreview() }
    }
    LaunchedEffect(model.state.exit) { if (model.state.exit) onExit() }
    BackHandler { model.act(WizardAction.Back) }
    CreateGameContent(model.state, photos, sounds, playback, model::act)
}

@Composable
internal fun CreateGameContent(state: CreateGameState, photos: List<LibraryPhoto>, sounds: List<LibrarySound>,
    playback: SoundPlaybackState, act: (WizardAction) -> Unit,
) {
    BlueGameTheme {
    val covered = state.selector != null || state.discard
    val enabled = !state.loading && !state.saving && !covered && state.error != WizardError.LOAD
    val focus = LocalFocusManager.current
    val open: (WizardAction) -> Unit = { focus.clearFocus(); act(it) }
    Box(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize().then(if (covered) Modifier.clearAndSetSemantics {} else Modifier)
            .background(Color(0xFFF0F7FF)).safeDrawingPadding().imePadding()) {
            val compactEditing = maxHeight < 300.dp && WindowInsets.ime.getBottom(LocalDensity.current) > 0
            Column(Modifier.fillMaxSize().testTag("create-game")) {
                if (!compactEditing) Text(stringResource(if (state.editing) R.string.edit_game else R.string.create_game), fontFamily = GameTitleFontFamily,
                    fontSize = 32.sp, color = WizardAccent, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).semantics { heading() })
                if (state.loading || state.saving) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (state.error == WizardError.LOAD || state.error == WizardError.SAVE) {
                    Text(stringResource(if (state.error == WizardError.LOAD) {
                        if (state.editing) R.string.game_load_error else R.string.game_library_load_error
                    } else R.string.game_save_error),
                        color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                    if (state.error == WizardError.LOAD) TextButton(onClick = { act(WizardAction.Retry) }, enabled = !state.loading) { Text(stringResource(R.string.retry)) }
                }
                LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("game-editor"), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    item {
                        OutlinedTextField(state.name, { act(WizardAction.Name(it)) }, enabled = enabled,
                            label = { Text(stringResource(R.string.game_name)) }, placeholder = { Text(stringResource(R.string.game_name_hint)) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                            singleLine = true, modifier = Modifier.fillMaxWidth(), isError = state.name.isNotEmpty() && state.name.isBlank())
                    }
                    item { Text(stringResource(R.string.game_images), style = MaterialTheme.typography.titleLarge) }
                    item { Text(stringResource(R.string.game_min_photos)) }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(photos.filter { it.id in state.imageIds }, key = { it.id }) { photo ->
                                PhotoAssetImage(photo.assetId, Modifier.size(88.dp), thumbnail = true,
                                    description = stringResource(R.string.game_photo), revision = photo.revision)
                            }
                        }
                    }
                    item { GameLink(stringResource(if (state.imageIds.isEmpty()) R.string.choose_photos else R.string.edit_photos_selection),
                        enabled, { open(WizardAction.Photos) }) }
                    if (state.canAddQuestions) {
                        item { Text(stringResource(R.string.game_question_count, state.questions.size), style = MaterialTheme.typography.titleMedium) }
                        items(state.questions, key = { it.id }) { question ->
                            QuestionCard(question, state.questions.indexOf(question) + 1, photos, sounds, playback, enabled, open)
                        }
                        item {
                            GameLink(stringResource(R.string.add_question), enabled && state.questions.size < 10, { open(WizardAction.AddQuestion) })
                            if (state.questions.size == 10) Text(stringResource(R.string.game_question_limit), Modifier.padding(top = 8.dp))
                        }
                    }
                }
                if (!compactEditing) Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { open(WizardAction.Cancel) }, enabled = !state.saving && !covered) { Text(stringResource(R.string.cancel)) }
                    Button(onClick = { act(WizardAction.Save) }, enabled = enabled && state.valid) { Text(stringResource(R.string.save_photo)) }
                }
            }
        }
        if (state.selector != null) MediaSelectorModal(state, photos, sounds, playback, act)
        if (state.discard) AlertDialog(onDismissRequest = { act(WizardAction.Keep) },
            title = { Text(stringResource(if (state.editing) R.string.discard_changes_title else R.string.discard_game_title)) }, text = { Text(stringResource(R.string.discard_game_message)) },
            confirmButton = { TextButton(onClick = { act(WizardAction.Discard) }) { Text(stringResource(R.string.discard_game)) } },
            dismissButton = { TextButton(onClick = { act(WizardAction.Keep) }) { Text(stringResource(R.string.keep_editing)) } })
    }
    }
}

@Composable
private fun GameLink(text: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(text, modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        fontFamily = HomeHeadingFontFamily, fontSize = 16.sp, textDecoration = TextDecoration.Underline,
        color = WizardAccent.copy(alpha = if (enabled) 1f else .45f))
}

@Composable
private fun QuestionCard(question: QuestionDraft, number: Int, photos: List<LibraryPhoto>, sounds: List<LibrarySound>,
    playback: SoundPlaybackState, enabled: Boolean, act: (WizardAction) -> Unit,
) {
    val photo = photos.find { it.id == question.imageId }
    val sound = sounds.find { it.id == question.soundId }
    Card(Modifier.fillMaxWidth().testTag("question-${question.id}"), shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Box(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.game_question_number, number), Modifier.fillMaxWidth().padding(end = 32.dp),
                style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (photo != null) Box(Modifier.size(80.dp).clickable(enabled = enabled, role = Role.Button) { act(WizardAction.EditImage(question.id)) }) {
                    PhotoAssetImage(photo.assetId, Modifier.fillMaxSize(), thumbnail = true,
                        description = stringResource(R.string.change_correct_image), revision = photo.revision)
                } else GameLink(stringResource(R.string.choose_correct_image), enabled,
                    { act(WizardAction.EditImage(question.id)) }, Modifier.weight(1f))
                Column(Modifier.weight(1f)) {
                    if (sound != null) SoundPreviewRow(sound, playback, enabled, { act(WizardAction.Preview(sound.assetId)) },
                        onReplace = { act(WizardAction.EditSound(question.id)) })
                    else GameLink(stringResource(R.string.choose_sound), enabled, { act(WizardAction.EditSound(question.id)) })
                }
            }
        }
        WizardSmallIcon("ui-close", stringResource(R.string.delete_question_number, number), enabled,
            { act(WizardAction.RemoveQuestion(question.id)) }, Modifier.align(Alignment.TopEnd), Color(0xFF808890))
        }
    }
}

@Composable
private fun WizardSmallIcon(assetId: String, label: String, enabled: Boolean, onClick: () -> Unit,
    modifier: Modifier = Modifier, color: Color = WizardAccent,
) {
    Box(modifier.size(48.dp).semantics { contentDescription = label }
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        AssetImage(assetId, Modifier.size(20.dp), colorFilter = ColorFilter.tint(color.copy(alpha = if (enabled) 1f else .45f)))
    }
}

@Composable
private fun MediaSelectorModal(state: CreateGameState, photos: List<LibraryPhoto>, sounds: List<LibrarySound>,
    playback: SoundPlaybackState, act: (WizardAction) -> Unit,
) {
    val selector = state.selector ?: return
    val title = stringResource(when (selector) {
        GameSelector.PHOTOS -> R.string.choose_photos
        GameSelector.IMAGE -> R.string.choose_correct_image
        GameSelector.SOUND -> R.string.choose_sound
    })
    BoxWithConstraints(Modifier.fillMaxSize().background(WizardInk.copy(alpha = .45f)).testTag("game-media-backdrop")
        .pointerInput(Unit) { detectTapGestures { act(WizardAction.Cancel) } }
        .safeDrawingPadding().imePadding().padding(16.dp), contentAlignment = Alignment.Center) {
        Surface(Modifier.widthIn(max = 560.dp).fillMaxWidth().height(minOf(maxHeight, 640.dp))
            .testTag("game-media-modal").semantics { paneTitle = title; isTraversalGroup = true }
            .pointerInput(Unit) { detectTapGestures {} },
            shape = RoundedCornerShape(28.dp), color = Color(0xFFF0F7FF), shadowElevation = 12.dp) {
            Column {
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, Modifier.weight(1f).semantics { heading() }, color = WizardAccent, fontFamily = GameTitleFontFamily, fontSize = 24.sp)
                    BubbleButton(onClick = { act(WizardAction.Cancel) }, iconAssetId = "ui-close", accessibilityLabel = stringResource(R.string.close),
                        showBorder = false, colors = listOf(WizardAccent), diameter = 48.dp)
                }
                val error = when (state.error) {
                    WizardError.USED_PHOTO -> R.string.game_photo_used
                    WizardError.MIN_PHOTOS -> R.string.game_photo_minimum
                    else -> null
                }
                if (error != null) Text(stringResource(error), Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.error)
                if (selector == GameSelector.SOUND) {
                    if (sounds.isEmpty()) SelectorEmpty(R.string.game_no_sounds, Modifier.weight(1f))
                    else LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(sounds, key = { it.id }) { sound ->
                            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                                SoundPreviewRow(sound, playback, true, { act(WizardAction.Preview(sound.assetId)) },
                                    checked = state.pendingChoice == sound.id, onSelect = { act(WizardAction.Sound(sound.id)) })
                            }
                        }
                    }
                } else {
                    val available = if (selector == GameSelector.PHOTOS) photos else photos.filter { it.id in state.imageIds }
                    if (available.isEmpty()) SelectorEmpty(R.string.game_no_photos, Modifier.weight(1f))
                    else LazyVerticalGrid(GridCells.Adaptive(104.dp), Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(available, key = { it.id }) { photo ->
                            val selected = if (selector == GameSelector.PHOTOS) photo.id in state.pendingImages else photo.id == state.pendingChoice
                            Box(Modifier.aspectRatio(1f).border(if (selected) 3.dp else 1.dp, if (selected) WizardAccent else Color.LightGray, RoundedCornerShape(12.dp))
                                .selectable(selected, role = Role.Button) { act(WizardAction.Photo(photo.id)) }
                                .semantics { contentDescription = photo.id }.padding(6.dp)) {
                                PhotoAssetImage(photo.assetId, Modifier.fillMaxSize(), thumbnail = true, revision = photo.revision)
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { act(WizardAction.Cancel) }) { Text(stringResource(R.string.cancel)) }
                    Button(onClick = { act(WizardAction.Done) }, enabled = selector == GameSelector.PHOTOS || state.pendingChoice != null) { Text(stringResource(R.string.game_done)) }
                }
            }
        }
    }
}

@Composable
private fun SelectorEmpty(message: Int, modifier: Modifier) {
    Box(modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { Text(stringResource(message), color = WizardInk) }
}

@Composable
private fun SoundPreviewRow(sound: LibrarySound, playback: SoundPlaybackState, enabled: Boolean, toggle: () -> Unit,
    checked: Boolean? = null, onSelect: () -> Unit = {}, onReplace: (() -> Unit)? = null,
) {
    val active = playback.assetId == sound.assetId
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BubbleButton(onClick = toggle, enabled = enabled && !playback.preparing, diameter = 48.dp, showBorder = false,
            colors = listOf(WizardAccent), iconAssetId = if (active && playback.playing) "ui-pause" else "ui-play",
            accessibilityLabel = stringResource(if (active && playback.playing) R.string.game_pause_sound else R.string.game_preview_sound, sound.name))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Box(Modifier.fillMaxWidth()) {
                Text(sound.name, Modifier.fillMaxWidth().padding(end = if (onReplace != null) 28.dp else 0.dp),
                    fontFamily = HomeHeadingFontFamily, fontSize = 20.sp, color = WizardInk, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (onReplace != null) Box(Modifier.matchParentSize()) {
                    WizardSmallIcon("ui-replace", stringResource(R.string.replace_sound), enabled, onReplace,
                        Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-12).dp))
                }
            }
            LinearProgressIndicator(progress = { if (active && playback.durationMs > 0) (playback.positionMs.toFloat() / playback.durationMs).coerceIn(0f, 1f) else 0f }, modifier = Modifier.fillMaxWidth().height(2.dp))
            Text(stringResource(R.string.game_sound_seconds, sound.durationMs / 1000f), fontSize = 12.sp, color = WizardInk)
            if (active && playback.failed) Text(stringResource(R.string.game_preview_error), color = MaterialTheme.colorScheme.error)
        }
        if (checked != null) Checkbox(checked, { onSelect() }, enabled = enabled, colors = CheckboxDefaults.colors(checkedColor = WizardAccent),
            modifier = Modifier.testTag("select-sound-${sound.id}").semantics { contentDescription = "${sound.name}" })
    }
}

@Preview(name = "Create game", widthDp = 360, heightDp = 800)
@Preview(name = "Create game large text", widthDp = 640, heightDp = 360, fontScale = 1.5f)
@Composable private fun CreateGamePreview() { PuzzleBubblesTheme { CreateGameContent(CreateGameState(loading = false), emptyList(), emptyList(), SoundPlaybackState(), {}) } }

private val previewPhotos = listOf(LibraryPhoto("cat-image", 0), LibraryPhoto("dog-image", 0))
private val previewSounds = listOf(LibrarySound("preview-sound", 2000, 0, "My recording"))
private fun readyPreview() = CreateGameState(name = "Family", imageIds = previewPhotos.map { it.id }, loading = false)

@Preview(name = "Loading Library", widthDp = 360, heightDp = 800)
@Composable private fun LoadingEditorPreview() { PuzzleBubblesTheme {
    CreateGameContent(CreateGameState(), emptyList(), emptyList(), SoundPlaybackState(), {})
} }
@Preview(name = "Save failed with draft retained", widthDp = 360, heightDp = 800)
@Composable private fun FailedSavePreview() { PuzzleBubblesTheme {
    CreateGameContent(readyPreview().copy(error = WizardError.SAVE,
        questions = listOf(QuestionDraft("preview", "cat-image", "preview-sound"))),
        previewPhotos, previewSounds, SoundPlaybackState(), {})
} }
@Preview(name = "Empty sound selector", widthDp = 360, heightDp = 800)
@Composable private fun EmptySoundSelectorPreview() { PuzzleBubblesTheme {
    CreateGameContent(readyPreview().copy(selector = GameSelector.SOUND), previewPhotos, emptyList(), SoundPlaybackState(), {})
} }

@Preview(name = "Ready to add questions", widthDp = 360, heightDp = 800)
@Composable private fun ReadyEditorPreview() { PuzzleBubblesTheme { CreateGameContent(readyPreview(), previewPhotos, previewSounds, SoundPlaybackState(), {}) } }
@Preview(name = "Edit existing game", widthDp = 360, heightDp = 800)
@Composable private fun EditGamePreview() { PuzzleBubblesTheme {
    CreateGameContent(readyPreview().copy(editing = true, questions = listOf(QuestionDraft("preview", "cat-image", "preview-sound"))),
        previewPhotos, previewSounds, SoundPlaybackState(), {})
} }
@Preview(name = "Photo selection modal", widthDp = 360, heightDp = 800)
@Composable private fun PhotoModalPreview() { PuzzleBubblesTheme { CreateGameContent(readyPreview().copy(selector = GameSelector.PHOTOS, pendingImages = previewPhotos.map { it.id }.toSet()), previewPhotos, previewSounds, SoundPlaybackState(), {}) } }
@Preview(name = "Sound selection modal", widthDp = 360, heightDp = 800)
@Composable private fun SoundModalPreview() { PuzzleBubblesTheme { CreateGameContent(readyPreview().copy(selector = GameSelector.SOUND, pendingChoice = "preview-sound"), previewPhotos, previewSounds, SoundPlaybackState(), {}) } }
@Preview(name = "Completed question", widthDp = 360, heightDp = 800)
@Preview(name = "Completed question landscape large text", widthDp = 640, heightDp = 360, fontScale = 1.5f)
@Composable private fun CompletedQuestionPreview() { PuzzleBubblesTheme {
    CreateGameContent(readyPreview().copy(questions = listOf(QuestionDraft("preview", "cat-image", "preview-sound"))),
        previewPhotos, previewSounds, SoundPlaybackState(), {})
} }
