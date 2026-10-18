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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.puzzlebubbles.data.photos.PhotoImageStore
import com.example.puzzlebubbles.domain.LibraryPhoto
import com.example.puzzlebubbles.domain.PhotoRepository
import com.example.puzzlebubbles.presentation.components.LocalPhotoImages
import com.example.puzzlebubbles.presentation.library.PhotosScreen
import com.example.puzzlebubbles.presentation.library.PhotosViewModel
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
        var saveGate: CompletableDeferred<Unit>? = null
        var deleteGate: CompletableDeferred<Unit>? = null
        override suspend fun refresh() = Unit
        override suspend fun save(draftAssetId: String): LibraryPhoto {
            saves++
            saveGate?.await()
            val photo = LibraryPhoto(UUID.randomUUID().toString(), 1)
            withContext(Dispatchers.IO) { images.write(photo.id, images.draft(draftAssetId)); images.promote(photo.id) }
            photos.value = photos.value + photo
            images.discard(draftAssetId)
            return photo
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
                        images.putDraft(UUID.randomUUID().toString(), Bitmap.createBitmap(1024, 1024,
                            Bitmap.Config.ARGB_8888).apply { eraseColor(Color.CYAN) })
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
        compose.onNodeWithContentDescription("Add Photo").performClick()
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
        compose.onNodeWithText("No photos yet").assertIsDisplayed()
        assertEquals(0, repository.saves)
    }
    @Test fun unsavedCancelAndBlockedSystemBackDoNotSave() {
        pick()
        compose.onNodeWithText("Delete").assertDoesNotExist()
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.onNodeWithTag("photo-modal").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithTag("photo-modal").assertDoesNotExist()
        assertEquals(0, repository.saves)
        assertTrue(repository.photos.value.isEmpty())
    }
    @Test fun rotationPreservesPreviewAndSaveProcessesRapidTapsOnlyOnce() {
        pick()
        val draft = model.modal!!.assetId
        compose.activityRule.scenario.recreate()
        compose.activityRule.scenario.onActivity { show(it) }
        compose.onNodeWithTag("photo-modal").assertIsDisplayed()
        assertEquals(draft, model.modal!!.assetId)
        repository.saveGate = CompletableDeferred()
        compose.runOnIdle { model.save(); model.save(); model.close(); model.delete() }
        compose.waitUntil { repository.saves == 1 }
        compose.activityRule.scenario.recreate()
        compose.activityRule.scenario.onActivity { show(it) }
        compose.onNodeWithText("Save").assertIsNotEnabled()
        compose.onNodeWithText("Cancel").assertIsNotEnabled()
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
        compose.onNodeWithText("No photos yet").assertIsDisplayed()
        assertEquals(1, repository.deletes)
    }
}
