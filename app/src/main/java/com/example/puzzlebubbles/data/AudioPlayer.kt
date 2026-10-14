package com.example.puzzlebubbles.data

interface AudioPlayer {
    fun play(assetId: String)
    suspend fun playToEnd(assetId: String)
    fun stop()
    fun release()
}
