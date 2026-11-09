package com.example.puzzlebubbles

import com.example.puzzlebubbles.presentation.library.GOOGLE_PHOTO_PICKER
import com.example.puzzlebubbles.presentation.library.usesDocumentFallback
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoPickerFallbackTest {
    @Test fun brokenAndroid16PickerBuildUsesFallback() =
        assertTrue(usesDocumentFallback(36, GOOGLE_PHOTO_PICKER, 36L))

    @Test fun otherPickerVersionsKeepStandardPicker() {
        assertFalse(usesDocumentFallback(36, GOOGLE_PHOTO_PICKER, 37L))
        assertFalse(usesDocumentFallback(36, GOOGLE_PHOTO_PICKER, null))
    }

    @Test fun otherAndroidVersionsKeepStandardPicker() {
        assertFalse(usesDocumentFallback(35, GOOGLE_PHOTO_PICKER, 36L))
        assertFalse(usesDocumentFallback(37, GOOGLE_PHOTO_PICKER, 36L))
    }

    @Test fun otherProvidersKeepStandardPicker() {
        assertFalse(usesDocumentFallback(36, "com.android.providers.media.module", 36L))
        assertFalse(usesDocumentFallback(36, null, null))
    }
}
