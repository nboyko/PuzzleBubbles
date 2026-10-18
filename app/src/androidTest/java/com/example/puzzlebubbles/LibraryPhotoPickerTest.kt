package com.example.puzzlebubbles

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.Build
import android.net.Uri
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.test.core.app.ApplicationProvider
import com.example.puzzlebubbles.presentation.library.LibraryPhotoPicker
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

class LibraryPhotoPickerTest {
    @Test fun brokenAndroid16PickerUsesSingleImageDocumentFallback() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val request = PickVisualMediaRequest(PickVisualMedia.ImageOnly)
        val provider = PickVisualMedia().createIntent(context, request).resolveActivity(context.packageManager)?.packageName
        assumeTrue(Build.VERSION.SDK_INT == 36 && provider == "com.google.android.photopicker")
        assumeTrue(context.packageManager.getPackageInfo(provider!!, 0).longVersionCode == 36L)

        val intent = LibraryPhotoPicker().createIntent(context, request)
        assertEquals(Intent.ACTION_OPEN_DOCUMENT, intent.action)
        assertEquals("image/*", intent.type)
        assertTrue(intent.hasCategory(Intent.CATEGORY_OPENABLE))
        assertFalse(intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, true))
        assertNotNull(intent.resolveActivity(context.packageManager))
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
