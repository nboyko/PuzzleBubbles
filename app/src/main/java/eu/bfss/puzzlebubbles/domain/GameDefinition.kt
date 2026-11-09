package eu.bfss.puzzlebubbles.domain

data class GameDefinition(
    val id: String,
    val title: String,
    val type: GameType,
    val coverImageAssetId: String,
)

enum class GameType {
    WHATS_THAT,
    MATCHING,
}
