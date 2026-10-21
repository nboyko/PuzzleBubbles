package com.example.puzzlebubbles.presentation.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.puzzlebubbles.R
import com.example.puzzlebubbles.presentation.components.AssetImage
import com.example.puzzlebubbles.presentation.components.BubbleButton
import com.example.puzzlebubbles.presentation.components.PhotoAssetImage
import com.example.puzzlebubbles.presentation.components.PhotoCropViewport
import com.example.puzzlebubbles.presentation.home.HomeNavigationBar
import com.example.puzzlebubbles.ui.theme.HomeHeadingFontFamily

private val PhotoInk = Color(0xFF18345B)
private val PhotoBlue = Color(0xFF166EC5)
private val PhotoBackground = Brush.verticalGradient(listOf(Color(0xFFEAF8FF), Color(0xFFF6F0FF)))

@Composable
fun PhotosScreen(model: PhotosViewModel, onBack: () -> Unit, onHome: () -> Unit, modifier: Modifier = Modifier) {
    val picker = rememberLauncherForActivityResult(LibraryPhotoPicker(), model::selected)
    val addPhoto: () -> Unit = {
        if (model.beginPick()) try {
            picker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
        } catch (_: Exception) { model.pickerFailed() }
    }
    val canAddPhoto = !model.busy && !model.picking && model.modal == null && model.error != PhotoError.LOAD
    val grid = rememberLazyGridState()
    LaunchedEffect(model.focusId, model.photos) {
        if (model.focusId != null && model.photos.any { it.id == model.focusId }) {
            grid.scrollToItem(0)
            model.focused()
        }
    }
    Box(modifier) {
        Column(Modifier.fillMaxSize().then(if (model.modal != null) Modifier.clearAndSetSemantics {} else Modifier)
            .background(PhotoBackground).windowInsetsPadding(
            WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        )) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BubbleButton(onClick = { if (model.modal == null) onBack() }, iconAssetId = "ui-back",
                        accessibilityLabel = stringResource(R.string.back), colors = listOf(PhotoInk), showBorder = false)
                    Text(stringResource(R.string.photos), Modifier.weight(1f).semantics { heading() },
                        fontFamily = HomeHeadingFontFamily, fontWeight = FontWeight.Bold, fontSize = 32.sp, color = PhotoInk)
                    BubbleButton(
                        onClick = addPhoto,
                        iconAssetId = "ui-plus", accessibilityLabel = stringResource(R.string.add_photo),
                        colors = listOf(PhotoBlue),
                        enabled = canAddPhoto,
                    )
                }
            }
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                when {
                    model.loading -> CircularProgressIndicator()
                    model.error == PhotoError.LOAD -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        PhotoErrorText(model.error)
                        BubbleButton(onClick = model::reload, text = stringResource(R.string.retry))
                    }
                    model.photos.isEmpty() -> Column(
                        Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        AssetImage("ui-image", Modifier.size(112.dp), colorFilter = ColorFilter.tint(PhotoBlue))
                        BubbleButton(onClick = addPhoto, text = stringResource(R.string.add_photo),
                            colors = listOf(PhotoBlue), enabled = canAddPhoto)
                    }
                    else -> LazyVerticalGrid(
                        columns = GridCells.Adaptive(136.dp), state = grid, modifier = Modifier.fillMaxSize().testTag("photos-grid"),
                        contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        itemsIndexed(model.photos, key = { _, photo -> photo.id }) { index, photo ->
                            Card(onClick = { model.open(photo) }, colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                PhotoAssetImage(photo.assetId, Modifier.fillMaxWidth().aspectRatio(1f).padding(8.dp),
                                    thumbnail = true, description = stringResource(R.string.library_photo, index + 1),
                                    revision = photo.revision)
                            }
                        }
                    }
                }
                if (model.busy && !model.loading && model.modal == null) CircularProgressIndicator()
            }
            if (model.modal == null && model.error == PhotoError.IMPORT) PhotoErrorText(model.error)
            HomeNavigationBar(true, { if (model.modal == null) onHome() }, { if (model.modal == null) onBack() },
                modifier = Modifier.alpha(if (model.modal == null) 1f else 0f))
        }
        model.modal?.let { modal -> PhotoModalContent(modal, model.busy, model.error,
            model::save, model::delete, model::close, model::transform) }
    }
}

@Composable
private fun PhotoModalContent(
    modal: PhotoModal, busy: Boolean, error: PhotoError?, onSave: () -> Unit,
    onDelete: () -> Unit, onClose: () -> Unit,
    onTransform: (Float, Float, Float, Float, Float, Float) -> Unit,
) {
    BackHandler { if (!busy) onClose() }
    val title = stringResource(R.string.edit_photo)
    BoxWithConstraints(Modifier.fillMaxSize().background(PhotoInk.copy(alpha = .45f))
        .testTag("photo-backdrop")
        .pointerInput(busy, onClose) { detectTapGestures { if (!busy) onClose() } },
        contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.safeDrawingPadding().padding(12.dp).widthIn(max = 680.dp)
                .fillMaxWidth().fillMaxHeight(if (maxHeight < 400.dp) 1f else .92f)
                .testTag("photo-modal").semantics { paneTitle = title }
                .pointerInput(Unit) { detectTapGestures {} },
            shape = RoundedCornerShape(28.dp), shadowElevation = 12.dp, color = Color.Transparent,
        ) {
            Column(Modifier.fillMaxSize().background(PhotoBackground).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    BubbleButton(
                        onClick = onClose, iconAssetId = "ui-close", accessibilityLabel = stringResource(R.string.close),
                        showBorder = false, colors = listOf(PhotoInk), enabled = !busy,
                    )
                }
                PhotoCropViewport(modal.assetId, modal.crop.region, enabled = !busy,
                    description = stringResource(R.string.photo_preview), onTransform = onTransform,
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 16.dp))
                if (busy) CircularProgressIndicator(Modifier.padding(8.dp))
                PhotoErrorText(error)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (modal.savedId != null) BubbleButton(
                        onClick = onDelete,
                        text = stringResource(R.string.delete_photo),
                        colors = listOf(Color(0xFF9251B7)), enabled = !busy, modifier = Modifier.weight(1f),
                    )
                    BubbleButton(onClick = onSave, text = stringResource(R.string.save_photo), enabled = !busy,
                        colors = listOf(PhotoBlue), modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PhotoErrorText(error: PhotoError?) {
    val message = when (error) {
        PhotoError.LOAD -> R.string.photo_load_error
        PhotoError.IMPORT -> R.string.photo_import_error
        PhotoError.SAVE -> R.string.photo_save_error
        PhotoError.DELETE -> R.string.photo_delete_error
        null -> return
    }
    Text(stringResource(message), Modifier.padding(12.dp), color = PhotoInk, textAlign = TextAlign.Center)
}
