package com.example.puzzlebubbles.presentation.library

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia

/** Keeps AndroidX selection and result handling, with a fallback for a broken picker build. */
class LibraryPhotoPicker : ActivityResultContract<PickVisualMediaRequest, Uri?>() {
    private val picker = PickVisualMedia()

    override fun createIntent(context: Context, input: PickVisualMediaRequest): Intent {
        val intent = picker.createIntent(context, input)
        val provider = intent.resolveActivity(context.packageManager)?.packageName
        // This Android 16 picker returns RESULT_CANCELED with no URI from Preview -> Select.
        // Do not reinterpret cancellation as success or request access to the entire gallery.
        if (Build.VERSION.SDK_INT == 36 && provider == "com.google.android.photopicker") {
            val version = try {
                context.packageManager.getPackageInfo(provider, 0).longVersionCode
            } catch (_: PackageManager.NameNotFoundException) { null }
            if (version == 36L) {
                val fallback = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "image/*"
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
                }
                if (fallback.resolveActivity(context.packageManager) != null) return fallback
            }
        }
        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? = picker.parseResult(resultCode, intent)
}
