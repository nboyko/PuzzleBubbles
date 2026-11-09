package eu.bfss.puzzlebubbles.domain

data class WhatsThatGame(
    val id: String, val title: String,
    val imagePool: List<WhatsThatChoice>,
    val questions: List<WhatsThatQuestion>,
)

data class WhatsThatQuestion(val id: String, val imageId: String, val audioAssetId: String)

data class CustomQuestion(val imageId: String, val soundId: String)
data class CustomGame(val id: String, val name: String, val imageIds: List<String>, val questions: List<CustomQuestion>) {
    fun validate() {
        require(name.trim().isNotEmpty())
        require(id.startsWith("whats-that-") && id.length > 11)
        require(imageIds.distinct().size == imageIds.size && imageIds.size >= 2)
        require(questions.size in 1..10)
        require(questions.all { it.imageId in imageIds && it.soundId.isNotBlank() })
    }
    fun playable(): WhatsThatGame {
        validate()
        return WhatsThatGame(id, name,
            imageIds.map { WhatsThatChoice(it, "library-photo:$it") },
            questions.mapIndexed { index, q -> WhatsThatQuestion("question-$index", q.imageId, "library-sound:${q.soundId}") })
    }
}

class MediaInUseException : IllegalStateException("Media is used by a custom game")

data class WhatsThatGameSummary(val id: String, val name: String, val isBuiltIn: Boolean = false)

data class WhatsThatChoice(val id: String, val imageAssetId: String)

data class WhatsThatRound(
    val id: String,
    val targetItemId: String,
    val audioAssetId: String,
    val choices: List<WhatsThatChoice>,
    val correctChoiceId: String,
)

data class WhatsThatSession(val rounds: List<WhatsThatRound>)
