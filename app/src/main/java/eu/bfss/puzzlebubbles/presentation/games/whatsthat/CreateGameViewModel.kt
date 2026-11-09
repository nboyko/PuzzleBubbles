package eu.bfss.puzzlebubbles.presentation.games.whatsthat

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import eu.bfss.puzzlebubbles.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.UUID

enum class GameSelector { PHOTOS, IMAGE, SOUND }
enum class WizardError { LOAD, SAVE, USED_PHOTO, MIN_PHOTOS }
data class QuestionDraft(val id: String = UUID.randomUUID().toString(), val imageId: String? = null, val soundId: String? = null)
data class CreateGameState(
    val name: String = "", val imageIds: List<String> = emptyList(),
    val questions: List<QuestionDraft> = emptyList(), val selector: GameSelector? = null,
    val pendingImages: Set<String> = emptySet(), val pendingChoice: String? = null,
    val minimumReached: Boolean = false, val editingQuestion: String? = null,
    val loading: Boolean = true, val saving: Boolean = false, val error: WizardError? = null,
    val discard: Boolean = false, val exit: Boolean = false, val changed: Boolean = false,
    val editing: Boolean = false,
) {
    val canAddQuestions get() = imageIds.size >= 2
    val valid get() = name.trim().isNotEmpty() && canAddQuestions && questions.size in 1..10 &&
        questions.all { it.imageId in imageIds && it.soundId != null }
}

sealed interface WizardAction {
    data class Name(val value: String) : WizardAction
    data class Photo(val id: String) : WizardAction
    data class EditImage(val question: String) : WizardAction
    data class EditSound(val question: String) : WizardAction
    data class Sound(val id: String) : WizardAction
    data class RemoveQuestion(val id: String) : WizardAction
    data class Preview(val assetId: String) : WizardAction
    data object Photos : WizardAction
    data object Done : WizardAction
    data object AddQuestion : WizardAction
    data object Back : WizardAction
    data object Cancel : WizardAction
    data object Keep : WizardAction
    data object Discard : WizardAction
    data object Save : WizardAction
    data object Retry : WizardAction
}

class CreateGameViewModel(
    private val games: GameRepository, private val photoRepository: PhotoRepository,
    private val soundRepository: SoundRepository, private val player: SoundPlayback,
    private val editGameId: String? = null,
) : ViewModel() {
    var state by mutableStateOf(CreateGameState(editing = editGameId != null)); private set
    val photos = photoRepository.photos
    val sounds = soundRepository.sounds
    val playback = player.playback
    private val gameId = editGameId ?: "whats-that-${UUID.randomUUID()}"
    private data class Values(val name: String, val photos: Set<String>, val questions: List<Pair<String?, String?>>)
    private fun values() = Values(state.name.trim(), state.imageIds.toSet(), state.questions.map { it.imageId to it.soundId })
    private var original = values()
    private fun updateChanged() { state = state.copy(changed = values() != original) }
    init { load() }

    private fun load() {
        state = state.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                photoRepository.refresh(); soundRepository.refresh()
                if (editGameId != null) {
                    val game = games.loadCustomGame(editGameId)
                    if (!state.exit) {
                        state = state.copy(name = game.name, imageIds = game.imageIds,
                            questions = game.questions.map { QuestionDraft(imageId = it.imageId, soundId = it.soundId) })
                        original = values()
                        updateChanged()
                    }
                }
            }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { state = state.copy(error = WizardError.LOAD) }
            finally { state = state.copy(loading = false) }
        }
    }
    fun stopPreview() = player.stop()
    fun act(action: WizardAction) {
        if (state.saving || state.exit) return
        if (state.discard) {
            when (action) {
                WizardAction.Keep, WizardAction.Back -> state = state.copy(discard = false)
                WizardAction.Discard -> state = state.copy(exit = true, discard = false)
                else -> Unit
            }
            return
        }
        if (action == WizardAction.Back || action == WizardAction.Cancel) {
            player.stop()
            if (state.selector != null) closeSelector()
            else state = state.copy(discard = state.changed, exit = !state.changed)
            return
        }
        if (state.loading || state.error == WizardError.LOAD) {
            if (action == WizardAction.Retry && !state.loading) load()
            return
        }
        if (state.selector != null) {
            select(action)
            updateChanged()
            return
        }
        when (action) {
            is WizardAction.Name -> state = state.copy(name = action.value.take(100), changed = true)
            WizardAction.Photos -> {
                player.stop()
                state = state.copy(selector = GameSelector.PHOTOS, pendingImages = state.imageIds.toSet(),
                    minimumReached = state.imageIds.size >= 2, error = null)
            }
            is WizardAction.EditImage -> openQuestionSelector(action.question, GameSelector.IMAGE)
            is WizardAction.EditSound -> openQuestionSelector(action.question, GameSelector.SOUND)
            WizardAction.AddQuestion -> if (state.canAddQuestions && state.questions.size < 10)
                state = state.copy(questions = state.questions + QuestionDraft(), changed = true)
            is WizardAction.RemoveQuestion -> if (state.questions.any { it.id == action.id }) {
                player.stop()
                state = state.copy(questions = state.questions.filterNot { it.id == action.id }, changed = true)
            }
            is WizardAction.Preview -> if (state.questions.any { q -> sounds.value.any { it.id == q.soundId && it.assetId == action.assetId } })
                player.toggle(action.assetId)
            WizardAction.Save -> save()
            else -> Unit
        }
        updateChanged()
    }
    private fun openQuestionSelector(id: String, selector: GameSelector) {
        val question = state.questions.find { it.id == id } ?: return
        player.stop()
        state = state.copy(selector = selector, editingQuestion = id, error = null,
            pendingChoice = if (selector == GameSelector.IMAGE) question.imageId else question.soundId)
    }
    private fun select(action: WizardAction) {
        when (action) {
            is WizardAction.Photo -> when (state.selector) {
                GameSelector.PHOTOS -> if (photos.value.any { it.id == action.id }) {
                    val selected = action.id in state.pendingImages
                    state = when {
                        selected && state.questions.any { it.imageId == action.id } -> state.copy(error = WizardError.USED_PHOTO)
                        selected && state.minimumReached && state.pendingImages.size <= 2 -> state.copy(error = WizardError.MIN_PHOTOS)
                        else -> {
                            val pending = if (selected) state.pendingImages - action.id else state.pendingImages + action.id
                            state.copy(pendingImages = pending, minimumReached = state.minimumReached || pending.size >= 2, error = null)
                        }
                    }
                }
                GameSelector.IMAGE -> if (action.id in state.imageIds) state = state.copy(pendingChoice = action.id)
                else -> Unit
            }
            is WizardAction.Sound -> if (state.selector == GameSelector.SOUND && sounds.value.any { it.id == action.id })
                state = state.copy(pendingChoice = action.id)
            is WizardAction.Preview -> if (state.selector == GameSelector.SOUND && sounds.value.any { it.assetId == action.assetId })
                player.toggle(action.assetId)
            WizardAction.Done -> commitSelection()
            else -> Unit
        }
    }
    private fun commitSelection() {
        when (state.selector) {
            GameSelector.PHOTOS -> {
                val ids = photos.value.filter { it.id in state.pendingImages }.map { it.id }
                if (!ids.containsAll(state.questions.mapNotNull { it.imageId })) return
                state = state.copy(imageIds = ids, changed = state.changed || ids != state.imageIds)
            }
            GameSelector.IMAGE, GameSelector.SOUND -> {
                val id = state.pendingChoice ?: return
                val image = state.selector == GameSelector.IMAGE
                if (image && id !in state.imageIds || !image && sounds.value.none { it.id == id }) return
                state = state.copy(questions = state.questions.map {
                    if (it.id == state.editingQuestion) {
                        if (image) it.copy(imageId = id) else it.copy(soundId = id)
                    } else it
                }, changed = true)
            }
            null -> return
        }
        player.stop()
        closeSelector()
    }
    private fun closeSelector() {
        state = state.copy(selector = null, pendingImages = emptySet(), pendingChoice = null,
            minimumReached = false, editingQuestion = null, error = null)
    }
    private fun save() {
        if (!state.valid) return
        player.stop()
        val game = CustomGame(gameId, state.name.trim(), state.imageIds,
            state.questions.map { CustomQuestion(checkNotNull(it.imageId), checkNotNull(it.soundId)) })
        state = state.copy(saving = true, error = null)
        viewModelScope.launch {
            try {
                if (state.editing) games.updateCustomGame(game) else games.saveCustomGame(game)
                state = state.copy(exit = true)
            }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { state = state.copy(error = WizardError.SAVE) }
            finally { state = state.copy(saving = false) }
        }
    }
    override fun onCleared() { player.release() }
    class Factory(private val games: GameRepository, private val photos: PhotoRepository,
        private val sounds: SoundRepository, private val editGameId: String? = null,
        private val player: () -> SoundPlayback) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            modelClass.cast(CreateGameViewModel(games, photos, sounds, player(), editGameId))!!
    }
}
