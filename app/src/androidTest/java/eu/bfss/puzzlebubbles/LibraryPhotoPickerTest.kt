package eu.bfss.puzzlebubbles

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.Build
import android.net.Uri
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.test.core.app.ApplicationProvider
import eu.bfss.puzzlebubbles.presentation.library.LibraryPhotoPicker
import eu.bfss.puzzlebubbles.presentation.library.usesDocumentFallback
import org.junit.Assert.*
import org.junit.Test

class LibraryPhotoPickerTest {
    /** Checks the outcome that applies to this device; the decision itself is unit-tested. */
    @Test fun pickerUsesDocumentFallbackOnlyForTheBrokenAndroid16Build() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val request = PickVisualMediaRequest(PickVisualMedia.ImageOnly)
        val standard = PickVisualMedia().createIntent(context, request)
        val provider = standard.resolveActivity(context.packageManager)?.packageName
        val version = provider?.let { context.packageManager.getPackageInfo(it, 0).longVersionCode }

        val intent = LibraryPhotoPicker().createIntent(context, request)
        if (usesDocumentFallback(Build.VERSION.SDK_INT, provider, version)) {
            assertEquals(Intent.ACTION_OPEN_DOCUMENT, intent.action)
            assertEquals("image/*", intent.type)
            assertTrue(intent.hasCategory(Intent.CATEGORY_OPENABLE))
            assertFalse(intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, true))
            assertNotNull(intent.resolveActivity(context.packageManager))
        } else {
            assertEquals(standard.action, intent.action)
            assertNotEquals(Intent.ACTION_OPEN_DOCUMENT, intent.action)
        }
    }

    @Test fun successfulDataAndClipResultsReturnTheSelectedImage() {
        val uri = Uri.parse("content://test/selected-image")
        val picker = LibraryPhotoPicker()
        assertEquals(uri, picker.parseResult(Activity.RESULT_OK, Intent().setData(uri)))
        assertEquals(uri, picker.parseResult(Activity.RESULT_OK,
            Intent().apply { clipData = ClipData.newRawUri("Photo", uri) }))
    }

    @Test fun cancellationAndEmptyResultsNeverImportAnything() {
        val picker = LibraryPhotoPicker()
        assertNull(picker.parseResult(Activity.RESULT_CANCELED, null))
        assertNull(picker.parseResult(Activity.RESULT_CANCELED, Intent().setData(Uri.parse("content://test/ignored"))))
        assertNull(picker.parseResult(Activity.RESULT_OK, Intent()))
    }
}
