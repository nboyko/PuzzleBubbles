package com.example.puzzlebubbles.presentation.library

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia

internal const val GOOGLE_PHOTO_PICKER = "com.google.android.photopicker"

/**
 * True only for the Android 16 Google Photo Picker build 36, which returns
 * RESULT_CANCELED with no URI from Preview -> Select. Cancellation is never
 * reinterpreted as success, and no access to the entire gallery is requested.
 */
internal fun usesDocumentFallback(sdk: Int, provider: String?, providerVersion: Long?): Boolean =
    sdk == 36 && provider == GOOGLE_PHOTO_PICKER && providerVersion == 36L

/** Keeps AndroidX selection and result handling, with a fallback for a broken picker build. */
class LibraryPhotoPicker : ActivityResultContract<PickVisualMediaRequest, Uri?>() {
    private val picker = PickVisualMedia()

    override fun createIntent(context: Context, input: PickVisualMediaRequest): Intent {
        val intent = picker.createIntent(context, input)
        val provider = intent.resolveActivity(context.packageManager)?.packageName
        val version = provider?.let {
            try { context.packageManager.getPackageInfo(it, 0).longVersionCode }
            catch (_: PackageManager.NameNotFoundException) { null }
        }
        if (usesDocumentFallback(Build.VERSION.SDK_INT, provider, version)) {
            val fallback = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
            }
            if (fallback.resolveActivity(context.packageManager) != null) return fallback
        }
        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? = picker.parseResult(resultCode, intent)
}
