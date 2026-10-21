package com.example.puzzlebubbles.domain

import kotlinx.coroutines.flow.StateFlow

data class LibraryPhoto(val id: String, val createdAt: Long, val revision: Long = 0) {
    val assetId: String get() = "library-photo:$id"
}

/** Square region in pixels of the temporary, oriented editing source. */
data class PhotoCrop(val left: Float, val top: Float, val side: Float)

interface PhotoRepository {
    val photos: StateFlow<List<LibraryPhoto>>
    suspend fun refresh()
    suspend fun save(draftAssetId: String, crop: PhotoCrop? = null): LibraryPhoto
    suspend fun replace(id: String, crop: PhotoCrop)
    suspend fun delete(id: String)
}
