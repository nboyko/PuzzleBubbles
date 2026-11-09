package com.example.puzzlebubbles

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.puzzlebubbles.data.photos.PhotoImageStore
import com.example.puzzlebubbles.data.photos.PhotoProcessor
import com.example.puzzlebubbles.domain.PhotoCrop
import com.example.puzzlebubbles.domain.LibraryPhoto
import com.example.puzzlebubbles.domain.PhotoRepository
import com.example.puzzlebubbles.presentation.components.LocalPhotoImages
import com.example.puzzlebubbles.presentation.library.PhotosScreen
import com.example.puzzlebubbles.presentation.library.PhotosViewModel
import com.example.puzzlebubbles.presentation.library.PhotoSource
import com.example.puzzlebubbles.ui.theme.PuzzleBubblesTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.UUID

class PhotosScreenTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private lateinit var root: File
    private lateinit var images: PhotoImageStore
    private lateinit var repository: CountingRepository
    private lateinit var model: PhotosViewModel
    private var selected: Uri? = Uri.parse("content://test/one")
    private var pickerInput: Any? = null
    private val picker = object : ActivityResultRegistry() {
        override fun <I, O> onLaunch(code: Int, contract: ActivityResultContract<I, O>, input: I, options: ActivityOptionsCompat?) {
            pickerInput = input
            dispatchResult(code, if (selected == null) Activity.RESULT_CANCELED else Activity.RESULT_OK, Intent().setData(selected))
        }
    }
    private val owner = object : ActivityResultRegistryOwner { override val activityResultRegistry get() = picker }
    private inner class CountingRepository : PhotoRepository {
        override val photos = MutableStateFlow<List<LibraryPhoto>>(emptyList())
        var saves = 0
        var deletes = 0
        var replacements = 0
        var failReplacement = false
        var saveGate: CompletableDeferred<Unit>? = null
        var replaceGate: CompletableDeferred<Unit>? = null
        var deleteGate: CompletableDeferred<Unit>? = null
        override suspend fun refresh() = Unit
        override suspend fun save(draftAssetId: String, crop: PhotoCrop?): LibraryPhoto {
            saves++
            saveGate?.await()
            val photo = LibraryPhoto(UUID.randomUUID().toString(), 1)
            withContext(Dispatchers.IO) {
                val output = PhotoProcessor.render(images.draft(draftAssetId), crop)
                try { images.write(photo.id, output); images.promote(photo.id) } finally { output.recycle() }
            }
            photos.value = photos.value + photo
            images.discard(draftAssetId)
            return photo
        }
        override suspend fun replace(id: String, crop: PhotoCrop) {
            replacements++
            replaceGate?.await()
            if (failReplacement) throw java.io.IOException("test replacement failure")
            withContext(Dispatchers.IO) {
                val source = images.image("library-photo:$id", false)!!
                val output = PhotoProcessor.render(source, crop)
                try { images.write(id, output); images.replace(id) } finally { source.recycle(); output.recycle() }
            }
            photos.value = photos.value.map { if (it.id == id) it.copy(revision = it.revision + 1) else it }
        }
        override suspend fun delete(id: String) {
            deletes++
            deleteGate?.await()
            withContext(Dispatchers.IO) { images.remove(images.file(id)) }
            photos.value = photos.value.filterNot { it.id == id }
        }
    }
    @Before fun setup() {
        compose.activityRule.scenario.onActivity { activity ->
            root = File(activity.cacheDir, "photo-ui-${UUID.randomUUID()}")
            images = PhotoImageStore(root)
            repository = CountingRepository()
            model = ViewModelProvider(activity, object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val vm = PhotosViewModel(repository, prepare = {
                        val bitmap = Bitmap.createBitmap(1024, 1024, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(Color.RED)
                        android.graphics.Canvas(bitmap).drawRect(512f, 0f, 1024f, 1024f,
                            android.graphics.Paint().apply { color = Color.GREEN })
                        PhotoSource(images.putDraft(UUID.randomUUID().toString(), bitmap), bitmap.width, bitmap.height)
                    }, discard = images::discard)
                    return modelClass.cast(vm)!!
                }
            })["photo-test", PhotosViewModel::class.java]
            show(activity)
        }
        compose.waitUntil { !model.loading }
    }
    private fun show(activity: MainActivity) {
        activity.setContent {
            PuzzleBubblesTheme {
                CompositionLocalProvider(LocalPhotoImages provides images, LocalActivityResultRegistryOwner provides owner) {
                    PhotosScreen(model, onBack = {}, onHome = {}, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
    @After fun cleanup() {
        compose.activityRule.scenario.onActivity { it.viewModelStore.clear() }
        root.deleteRecursively() // Unique test-owned files only.
    }
    private fun pick() {
        if (model.photos.isEmpty()) compose.onNodeWithText("Add Photo").performClick()
        else compose.onNodeWithContentDescription("Add Photo").performClick()
        compose.onNodeWithTag("photo-modal").assertIsDisplayed()
    }
    private fun saveNew() {
        pick()
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil { model.modal == null && model.photos.size == 1 }
        compose.waitUntil { compose.onAllNodesWithContentDescription("Photo 1").fetchSemanticsNodes().isNotEmpty() }
    }
    @Test fun pickerIsSingleImageCancellationLeavesLibraryUnchanged() {
        selected = null
        compose.onNodeWithContentDescription("Add Photo").performClick()
        compose.waitForIdle()
        assertSame(PickVisualMedia.ImageOnly, (pickerInput as PickVisualMediaRequest).mediaType)
        compose.onNodeWithTag("photo-modal").assertDoesNotExist()
        compose.onNodeWithText("Add Photo").assertIsDisplayed()
        assertEquals(0, repository.saves)
    }
    @Test fun unsavedCancelAndSystemBackDiscardWithoutSaving() {
        pick()
        compose.onNodeWithText("Edit Photo").assertDoesNotExist()
        compose.onNodeWithTag("photo-modal").performTouchInput { click(androidx.compose.ui.geometry.Offset(4f, 4f)) }
        compose.onNodeWithTag("photo-modal").assertIsDisplayed()
        compose.onNodeWithTag("photo-backdrop").performTouchInput { click(androidx.compose.ui.geometry.Offset(1f, 1f)) }
        compose.onNodeWithTag("photo-modal").assertDoesNotExist()
        pick()
        compose.onNodeWithText("Delete").assertDoesNotExist()
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.onNodeWithTag("photo-modal").assertDoesNotExist()
        pick()
        compose.onNodeWithText("Cancel").assertDoesNotExist()
        compose.onNodeWithContentDescription("Close").performClick()
        compose.onNodeWithTag("photo-modal").assertDoesNotExist()
        assertEquals(0, repository.saves)
        assertTrue(repository.photos.value.isEmpty())
    }
    @Test fun rotationPreservesPreviewAndSaveProcessesRapidTapsOnlyOnce() {
        pick()
        val draft = model.modal!!.assetId
        compose.runOnIdle { model.transform(2f, 10f, -5f, 0f, 0f, 200f) }
        val crop = model.modal!!.crop
        compose.activityRule.scenario.recreate()
        compose.activityRule.scenario.onActivity { show(it) }
        compose.onNodeWithTag("photo-modal").assertIsDisplayed()
        assertEquals(draft, model.modal!!.assetId)
        assertEquals(crop, model.modal!!.crop)
        repository.saveGate = CompletableDeferred()
        compose.runOnIdle { model.save(); model.save(); model.close(); model.delete() }
        compose.waitUntil { repository.saves == 1 }
        compose.activityRule.scenario.recreate()
        compose.activityRule.scenario.onActivity { show(it) }
        compose.onNodeWithText("Save").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Close").assertIsNotEnabled()
        compose.onNodeWithTag("photo-backdrop").performTouchInput { click(androidx.compose.ui.geometry.Offset(1f, 1f)) }
        compose.onNodeWithTag("photo-modal").assertIsDisplayed()
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.onNodeWithTag("photo-modal").assertIsDisplayed()
        compose.runOnIdle { repository.saveGate!!.complete(Unit) }
        compose.waitUntil { model.modal == null }
        assertEquals(1, repository.saves)
        assertEquals(1, model.photos.size)
    }
    @Test fun savedSaveAndCloseDoNotRewriteOrDuplicatePhoto() {
        saveNew()
        val photo = model.photos.single()
        val bytes = images.file(photo.id).readBytes()
        for (closeWithSave in listOf(true, false)) {
            compose.onNodeWithContentDescription("Photo 1").performClick()
            compose.onNodeWithText("Delete").assertIsDisplayed()
            compose.onNodeWithText("Cancel").assertDoesNotExist()
            if (closeWithSave) compose.onNodeWithText("Save").performClick()
            else compose.onNodeWithContentDescription("Close").performClick()
            compose.onNodeWithTag("photo-modal").assertDoesNotExist()
        }
        assertArrayEquals(bytes, images.file(photo.id).readBytes())
        assertEquals(listOf(photo), model.photos)
        assertEquals(1, repository.saves)
    }
    @Test fun repeatedDeleteCannotOverlapAndLastPhotoReturnsToEmptyState() {
        saveNew()
        compose.onNodeWithContentDescription("Photo 1").performClick()
        repository.deleteGate = CompletableDeferred()
        compose.runOnIdle { model.delete(); model.delete(); model.save(); model.close() }
        compose.waitUntil { repository.deletes == 1 }
        compose.onNodeWithText("Delete").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Close").assertIsNotEnabled()
        compose.runOnIdle { repository.deleteGate!!.complete(Unit) }
        compose.waitUntil { model.modal == null }
        compose.onNodeWithText("Add Photo").assertIsDisplayed()
        assertEquals(1, repository.deletes)
    }
    @Test fun savedEditingPreservesIdentityAndRefreshesThumbnailAfterOneSave() {
        saveNew()
        val photo = model.photos.single()
        val before = images.file(photo.id).readBytes()
        compose.onNodeWithContentDescription("Photo 1").performClick()
        compose.waitForIdle()
        val screenshot = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        try {
            java.io.File(compose.activity.externalCacheDir, "circular-crop-guide.png").outputStream().use {
                screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        } finally { screenshot.recycle() }
        compose.onNodeWithTag("photo-crop-viewport").performTouchInput {
            down(0, center - androidx.compose.ui.geometry.Offset(30f, 0f))
            down(1, center + androidx.compose.ui.geometry.Offset(30f, 0f))
            moveTo(0, center - androidx.compose.ui.geometry.Offset(90f, 0f), delayMillis = 100)
            moveTo(1, center + androidx.compose.ui.geometry.Offset(90f, 0f), delayMillis = 100)
            up(0); up(1)
        }
        compose.runOnIdle { assertTrue(model.modal!!.crop.zoom > 1f) }
        // Select the right half deterministically after verifying the real pinch detector.
        compose.runOnIdle {
            model.transform(.01f, 0f, 0f, 0f, 0f, 200f)
            model.transform(2f, -100f, 0f, 0f, 0f, 200f)
        }
        val crop = model.modal!!.crop
        repository.replaceGate = CompletableDeferred()
        compose.runOnIdle { model.save(); model.save(); model.delete(); model.close() }
        compose.waitUntil { repository.replacements == 1 }
        compose.activityRule.scenario.recreate()
        compose.activityRule.scenario.onActivity { show(it) }
        assertEquals(crop, model.modal!!.crop)
        compose.onNodeWithText("Save").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Close").assertIsNotEnabled()
        compose.runOnIdle { repository.replaceGate!!.complete(Unit) }
        compose.waitUntil { model.modal == null && model.photos.single().revision == 1L }
        compose.waitUntil { compose.onAllNodesWithContentDescription("Photo 1").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(photo.id, model.photos.single().id)
        assertEquals(photo.createdAt, model.photos.single().createdAt)
        assertFalse(before.contentEquals(images.file(photo.id).readBytes()))
        assertEquals(1, repository.replacements)
        compose.waitUntil(timeoutMillis = 5_000) {
            val pixels = compose.onNodeWithContentDescription("Photo 1").captureToImage().toPixelMap()
            pixels[pixels.width / 4, pixels.height / 2].green > .8f
        }
        compose.onNodeWithContentDescription("Photo 1").performClick() // Re-edit the new stored image.
        assertTrue(model.modal!!.crop.unchanged)
        compose.onNodeWithContentDescription("Close").performClick()
    }
    @Test fun savedCropCanBeCancelledThroughBackWithoutChangingFile() {
        saveNew()
        val photo = model.photos.single()
        val before = images.file(photo.id).readBytes()
        compose.onNodeWithContentDescription("Photo 1").performClick()
        compose.runOnIdle { model.transform(2f, -50f, 0f, 0f, 0f, 200f) }
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.onNodeWithTag("photo-modal").assertDoesNotExist()
        assertArrayEquals(before, images.file(photo.id).readBytes())
        assertEquals(0, repository.replacements)
    }
    @Test fun failedReplacementKeepsCropAndAllowsExplicitRetry() {
        saveNew()
        val photo = model.photos.single()
        val before = images.file(photo.id).readBytes()
        compose.onNodeWithContentDescription("Photo 1").performClick()
        compose.runOnIdle { model.transform(2f, -100f, 0f, 0f, 0f, 200f) }
        val crop = model.modal!!.crop
        repository.failReplacement = true
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil { !model.busy }
        assertEquals(crop, model.modal!!.crop)
        assertArrayEquals(before, images.file(photo.id).readBytes())
        compose.onNodeWithText("Couldn't save this photo. Check available storage and try again.").assertIsDisplayed()
        repository.failReplacement = false
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil { model.modal == null }
        assertEquals(2, repository.replacements)
        assertEquals(photo.id, model.photos.single().id)
    }
}
