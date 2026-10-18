package com.example.puzzlebubbles.domain

import kotlinx.coroutines.flow.StateFlow

data class LibraryPhoto(val id: String, val createdAt: Long) {
    val assetId: String get() = "library-photo:$id"
}

interface PhotoRepository {
    val photos: StateFlow<List<LibraryPhoto>>
    suspend fun refresh()
    suspend fun save(draftAssetId: String): LibraryPhoto
    suspend fun delete(id: String)
}
