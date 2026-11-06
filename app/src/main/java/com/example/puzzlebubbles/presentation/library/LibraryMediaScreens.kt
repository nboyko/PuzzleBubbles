package com.example.puzzlebubbles.presentation.library

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.puzzlebubbles.R
import com.example.puzzlebubbles.domain.LibrarySound
import com.example.puzzlebubbles.domain.SoundDraft
import com.example.puzzlebubbles.domain.SoundPlaybackState
import com.example.puzzlebubbles.presentation.components.AssetImage
import com.example.puzzlebubbles.presentation.components.BubbleButton
import com.example.puzzlebubbles.presentation.home.HomeNavigationBar
import com.example.puzzlebubbles.ui.theme.HomeHeadingFontFamily
import com.example.puzzlebubbles.ui.theme.PuzzleBubblesTheme

private val SoundInk = Color(0xFF18345B)
private val SoundAccent = Color(0xFF9251B7)
private val SoundBackground = Brush.verticalGradient(listOf(Color(0xFFEAF8FF), Color(0xFFF6F0FF)))

@Composable
fun SoundsScreen(model: SoundsViewModel, onBack: () -> Unit, onHome: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activity = context.activity()
    val owner = LocalLifecycleOwner.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        model.permissionResult(granted, !granted && activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.RECORD_AUDIO))
    }
    DisposableEffect(owner, model) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> model.foreground()
                Lifecycle.Event.ON_STOP -> if (activity?.isChangingConfigurations != true) model.background()
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) model.foreground()
        onDispose {
            owner.lifecycle.removeObserver(observer)
            if (activity?.isChangingConfigurations != true) model.background()
        }
    }
    SoundsContent(model.uiState, model.playback, onBack, onHome,
        onAdd = model::open, onClose = model::close,
        onRecord = {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            if (model.record(granted)) try { permission.launch(Manifest.permission.RECORD_AUDIO) }
            catch (_: Exception) { model.permissionResult(false, false) }
        }, onStop = model::stopRecording, onSave = model::save, onToggle = model::toggle,
        onNameChange = model::nameChanged,
        onDeleteRequest = model::requestDelete, onDeleteCancel = model::cancelDelete,
        onDelete = model::delete, onRetry = model::retry,
        onSettings = {
            try { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) }
            catch (_: Exception) { /* The permission explanation and Record remain available. */ }
        }, modifier = modifier)
}

private tailrec fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}

@Composable
private fun SoundsContent(
    state: SoundsUiState, playback: SoundPlaybackState = SoundPlaybackState(),
    onBack: () -> Unit = {}, onHome: () -> Unit = {}, onAdd: () -> Unit = {}, onClose: () -> Unit = {},
    onRecord: () -> Unit = {}, onStop: () -> Unit = {}, onSave: () -> Unit = {},
    onNameChange: (String) -> Unit = {},
    onToggle: (String) -> Unit = {}, onDeleteRequest: (String) -> Unit = {},
    onDeleteCancel: () -> Unit = {}, onDelete: () -> Unit = {}, onRetry: () -> Unit = {},
    onSettings: () -> Unit = {}, modifier: Modifier = Modifier,
) {
    val covered = state.modal != null || state.deleteId != null
    val canAdd = !covered && !state.blocked && state.error !in listOf(SoundError.LOAD, SoundError.CLEANUP)
    BackHandler(enabled = covered || state.committing || state.cleaning) {
        if (!state.committing) {
            if (state.deleteId != null) onDeleteCancel() else onClose()
        }
    }
    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(SoundBackground)
            .then(if (covered) Modifier.clearAndSetSemantics {} else Modifier)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                BubbleButton(onClick = { if (!covered && !state.blocked) onBack() }, iconAssetId = "ui-back",
                    accessibilityLabel = stringResource(R.string.back), colors = listOf(SoundInk), showBorder = false)
                Text(stringResource(R.string.sounds), Modifier.weight(1f).semantics { heading() },
                    color = SoundInk, fontFamily = HomeHeadingFontFamily, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                BubbleButton(onClick = onAdd, iconAssetId = "ui-plus", accessibilityLabel = stringResource(R.string.add_sound),
                    colors = listOf(SoundAccent), enabled = canAdd)
            }
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                when {
                    state.loading -> CircularProgressIndicator()
                    state.error == SoundError.LOAD -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        SoundErrorText(state.error)
                        BubbleButton(onClick = onRetry, text = stringResource(R.string.retry))
                    }
                    state.sounds.isEmpty() -> Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        AssetImage("ui-sound", Modifier.size(112.dp).weight(1f, fill = false), colorFilter = ColorFilter.tint(SoundAccent))
                        BubbleButton(onClick = onAdd, text = stringResource(R.string.add_record),
                            colors = listOf(SoundAccent), enabled = canAdd)
                    }
                    else -> LazyColumn(Modifier.fillMaxSize().testTag("sounds-list"), contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        itemsIndexed(state.sounds, key = { _, sound -> sound.id }) { index, sound ->
                            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(24.dp)) {
                                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f).padding(8.dp)) {
                                        Text(sound.name, color = SoundInk, fontFamily = HomeHeadingFontFamily,
                                            fontSize = 22.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        SoundPlaybackControl(sound.assetId, sound.durationMs, index + 1, playback,
                                            !covered && !state.blocked, onToggle, Modifier.fillMaxWidth())
                                    }
                                    BubbleButton(onClick = { onDeleteRequest(sound.id) }, iconAssetId = "ui-delete",
                                        accessibilityLabel = stringResource(R.string.delete_recording, index + 1),
                                        showBorder = false, colors = listOf(SoundAccent), enabled = !covered && !state.blocked)
                                }
                            }
                        }
                    }
                }
            }
            if (state.modal == null && state.deleteId == null && state.error != SoundError.LOAD && state.error != null) {
                SoundErrorText(state.error)
                BubbleButton(onClick = onRetry, text = stringResource(R.string.retry), enabled = !state.blocked)
            }
            if (state.modal == null && playback.failed) Text(stringResource(R.string.sound_play_error), Modifier.padding(12.dp), color = SoundInk)
            HomeNavigationBar(true, { if (!covered && !state.blocked) onHome() }, { if (!covered && !state.blocked) onBack() })
        }
        state.modal?.let { modal ->
            SoundModal(stringResource(R.string.record_sound), state.committing, onClose) {
                val title = when (modal.step) {
                    RecordingStep.RECORDING -> R.string.recording_active
                    RecordingStep.STOPPING -> R.string.recording_finishing
                    RecordingStep.STARTING -> R.string.recording_starting
                    RecordingStep.PERMISSION -> R.string.recording_permission_pending
                    RecordingStep.PREVIEW, RecordingStep.SAVING -> R.string.recording_preview
                    else -> R.string.record_sound
                }
                Text(stringResource(title), fontFamily = HomeHeadingFontFamily, fontSize = 28.sp,
                    color = SoundInk, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(20.dp))
                when (modal.step) {
                    RecordingStep.RECORDING -> {
                        Text(stringResource(R.string.sound_time, soundTime(modal.elapsedMs), "0:05"),
                            fontFamily = HomeHeadingFontFamily, fontSize = 36.sp, color = SoundAccent,
                            modifier = Modifier.align(Alignment.CenterHorizontally).testTag("recording-timer"))
                        BubbleButton(onClick = onStop, text = stringResource(R.string.stop_recording), iconAssetId = "ui-stop",
                            colors = listOf(SoundAccent), modifier = Modifier.fillMaxWidth().padding(top = 24.dp))
                    }
                    RecordingStep.PREVIEW, RecordingStep.SAVING -> {
                        modal.draft?.let { draft ->
                            RecordingNameField(draft.assetId, modal.name, !state.blocked, onNameChange)
                            Spacer(Modifier.height(16.dp))
                            SoundPlaybackControl(draft.assetId, draft.durationMs, 1, playback, !state.blocked, onToggle, Modifier.fillMaxWidth())
                        }
                        if (playback.failed) Text(stringResource(R.string.sound_play_error), color = SoundInk)
                        SoundErrorText(modal.error)
                        if (modal.step == RecordingStep.SAVING) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally).padding(12.dp))
                        Row(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            BubbleButton(onClick = onClose, text = stringResource(R.string.cancel), enabled = !state.committing,
                                colors = listOf(SoundAccent), modifier = Modifier.weight(1f))
                            BubbleButton(onClick = onSave, text = stringResource(R.string.save_sound), enabled = !state.blocked,
                                colors = listOf(SoundAccent), modifier = Modifier.weight(1f))
                        }
                    }
                    RecordingStep.IDLE -> {
                        AssetImage("ui-microphone", Modifier.size(88.dp).align(Alignment.CenterHorizontally), colorFilter = ColorFilter.tint(SoundAccent))
                        Text("0:00", Modifier.align(Alignment.CenterHorizontally), fontFamily = HomeHeadingFontFamily, fontSize = 36.sp, color = SoundAccent)
                        SoundErrorText(modal.error)
                        if (modal.settingsRequired) BubbleButton(onClick = onSettings, text = stringResource(R.string.open_app_settings),
                            colors = listOf(SoundAccent), modifier = Modifier.fillMaxWidth())
                        BubbleButton(onClick = onRecord, text = stringResource(R.string.record), iconAssetId = "ui-microphone",
                            enabled = !state.blocked && !state.permissionPending, colors = listOf(SoundAccent),
                            modifier = Modifier.fillMaxWidth().padding(top = 24.dp))
                    }
                    else -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally).padding(24.dp))
                }
            }
        }
        if (state.deleteId != null) SoundModal(stringResource(R.string.delete_sound_confirmation), state.deleting, onDeleteCancel) {
            Text(stringResource(R.string.delete_sound_confirmation), fontFamily = HomeHeadingFontFamily, fontSize = 28.sp, color = SoundInk)
            SoundErrorText(state.error)
            if (state.deleting) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally).padding(12.dp))
            Row(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BubbleButton(onClick = onDeleteCancel, text = stringResource(R.string.cancel), enabled = !state.deleting,
                    colors = listOf(SoundAccent), modifier = Modifier.weight(1f))
                BubbleButton(onClick = onDelete, text = stringResource(R.string.delete_sound), enabled = !state.deleting,
                    colors = listOf(SoundAccent), modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SoundModal(title: String, busy: Boolean, onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().background(SoundInk.copy(alpha = .45f)).imePadding().testTag("sound-backdrop")
        .pointerInput(busy, onClose) { detectTapGestures { if (!busy) onClose() } }, contentAlignment = Alignment.Center) {
        Surface(Modifier.safeDrawingPadding().padding(12.dp).widthIn(max = 560.dp).fillMaxWidth()
            .heightIn(max = maxHeight - 32.dp).testTag("sound-modal").semantics { paneTitle = title }
            .pointerInput(Unit) { detectTapGestures {} }, shape = RoundedCornerShape(28.dp), color = Color.Transparent, shadowElevation = 12.dp) {
            Column(Modifier.background(SoundBackground).verticalScroll(rememberScrollState()).padding(20.dp)) {
                BubbleButton(onClick = onClose, iconAssetId = "ui-close", accessibilityLabel = stringResource(R.string.close),
                    colors = listOf(SoundInk), enabled = !busy, showBorder = false, modifier = Modifier.align(Alignment.End))
                content()
            }
        }
    }
}

@Composable
private fun RecordingNameField(assetId: String, name: String, enabled: Boolean, onChange: (String) -> Unit) {
    var value by rememberSaveable(assetId, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(name, TextRange(0, name.length)))
    }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val inspection = LocalInspectionMode.current
    LaunchedEffect(assetId) { if (enabled && !inspection) focus.requestFocus() }
    OutlinedTextField(value = value, onValueChange = {
        value = it
        onChange(it.text)
    }, label = { Text(stringResource(R.string.recording_name)) }, singleLine = true, enabled = enabled,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SoundAccent, cursorColor = SoundAccent),
        modifier = Modifier.fillMaxWidth().focusRequester(focus).testTag("recording-name"))
}

@Composable
private fun SoundPlaybackControl(
    assetId: String, durationMs: Long, number: Int, state: SoundPlaybackState, enabled: Boolean,
    onToggle: (String) -> Unit, modifier: Modifier = Modifier,
) {
    val active = state.assetId == assetId
    val playing = active && state.playing
    val preparing = active && state.preparing
    val position = if (active) state.positionMs.coerceIn(0, durationMs) else 0
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BubbleButton(onClick = { onToggle(assetId) }, iconAssetId = if (playing) "ui-pause" else "ui-play",
            accessibilityLabel = stringResource(if (playing) R.string.pause_recording else R.string.play_recording, number),
            enabled = enabled && !preparing, colors = listOf(SoundAccent), showBorder = false)
        Column(Modifier.weight(1f)) {
            LinearProgressIndicator(progress = { if (durationMs > 0) position.toFloat() / durationMs else 0f },
                modifier = Modifier.fillMaxWidth().testTag("sound-progress:$assetId"), color = SoundAccent)
            Text(stringResource(R.string.sound_time, soundTime(position), soundTime(durationMs)), color = SoundInk,
                fontFamily = HomeHeadingFontFamily, fontSize = 16.sp, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

internal fun soundTime(milliseconds: Long): String {
    val seconds = milliseconds.coerceAtLeast(0) / 1000
    return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}

@Composable
private fun SoundErrorText(error: SoundError?) {
    val message = when (error) {
        SoundError.LOAD -> R.string.sound_load_error
        SoundError.PERMISSION -> R.string.sound_permission_error
        SoundError.RECORD -> R.string.sound_record_error
        SoundError.SAVE -> R.string.sound_save_error
        SoundError.DELETE -> R.string.sound_delete_error
        SoundError.IN_USE -> R.string.sound_in_use
        SoundError.CLEANUP -> R.string.sound_cleanup_error
        null -> return
    }
    Text(stringResource(message), Modifier.fillMaxWidth().padding(vertical = 12.dp), color = SoundInk, textAlign = TextAlign.Center)
}

@Preview(name = "Sounds - empty", widthDp = 360, heightDp = 800)
@Preview(name = "Sounds - landscape", widthDp = 800, heightDp = 360)
@Composable
private fun SoundsScreenPreview() { PuzzleBubblesTheme { SoundsContent(SoundsUiState(loading = false)) } }

@Preview(name = "Sounds - saved and playing", widthDp = 360, heightDp = 800)
@Composable
private fun SoundsListPreview() {
    PuzzleBubblesTheme { SoundsContent(SoundsUiState(loading = false,
        sounds = listOf(LibrarySound("example-a", 4000, 2, "Track 2"), LibrarySound("example-b", 3000, 1, "Birdsong"))),
        SoundPlaybackState("library-sound:example-a", 1200, 4000, playing = true)) }
}

@Preview(name = "Recording modal", widthDp = 360, heightDp = 800)
@Preview(name = "Recording - large text", widthDp = 320, heightDp = 640, fontScale = 1.5f)
@Composable
private fun RecordingPreview() { PuzzleBubblesTheme { SoundsContent(SoundsUiState(loading = false, modal = RecordingModal())) } }

@Preview(name = "Recording - active", widthDp = 360, heightDp = 800)
@Composable
private fun ActiveRecordingPreview() { PuzzleBubblesTheme { SoundsContent(SoundsUiState(loading = false,
    modal = RecordingModal(RecordingStep.RECORDING, elapsedMs = 3000))) } }

@Preview(name = "Recording - preview", widthDp = 360, heightDp = 800)
@Preview(name = "Recording preview - landscape", widthDp = 800, heightDp = 360)
@Composable
private fun RecordedSoundPreview() { PuzzleBubblesTheme { SoundsContent(SoundsUiState(loading = false,
    modal = RecordingModal(RecordingStep.PREVIEW, SoundDraft("sound-draft:example", 3000), name = "Track 1"))) } }

@Preview(name = "Recording - permission denied", widthDp = 360, heightDp = 800)
@Composable
private fun DeniedRecordingPreview() { PuzzleBubblesTheme { SoundsContent(SoundsUiState(loading = false,
    modal = RecordingModal(error = SoundError.PERMISSION, settingsRequired = true))) } }

@Preview(name = "Recording - saving", widthDp = 360, heightDp = 800)
@Composable
private fun SavingSoundPreview() { PuzzleBubblesTheme { SoundsContent(SoundsUiState(loading = false,
    modal = RecordingModal(RecordingStep.SAVING, SoundDraft("sound-draft:example", 3000), name = "Track 1"))) } }

@Preview(name = "Delete sound confirmation", widthDp = 360, heightDp = 800)
@Composable
private fun DeleteSoundPreview() { PuzzleBubblesTheme { SoundsContent(SoundsUiState(loading = false, deleteId = "example")) } }

@Preview(name = "Sounds - loading", widthDp = 360, heightDp = 800)
@Composable
private fun SoundsLoadingPreview() { PuzzleBubblesTheme { SoundsContent(SoundsUiState()) } }

@Preview(name = "Sounds - load error", widthDp = 360, heightDp = 800)
@Composable
private fun SoundsLoadErrorPreview() { PuzzleBubblesTheme { SoundsContent(SoundsUiState(loading = false, error = SoundError.LOAD)) } }

@Preview(name = "Recording - save error", widthDp = 360, heightDp = 800)
@Composable
private fun SoundSaveErrorPreview() { PuzzleBubblesTheme { SoundsContent(SoundsUiState(loading = false,
    modal = RecordingModal(RecordingStep.PREVIEW, SoundDraft("sound-draft:example", 3000), error = SoundError.SAVE, name = "Birdsong"))) } }
