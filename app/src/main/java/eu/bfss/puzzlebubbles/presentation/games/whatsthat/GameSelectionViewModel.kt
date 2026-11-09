package eu.bfss.puzzlebubbles.presentation.games.whatsthat

import androidx.compose.runtime.*
import androidx.lifecycle.*
import eu.bfss.puzzlebubbles.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class GameSelectionViewModel(private val repository: GameRepository) : ViewModel() {
    var games by mutableStateOf(repository.getWhatsThatGames()); private set
    var loading by mutableStateOf(true); private set
    var loadError by mutableStateOf(false); private set
    var deleting by mutableStateOf(false); private set
    var deleteId by mutableStateOf<String?>(null); private set
    var deleteError by mutableStateOf(false); private set
    init { reload() }
    fun reload() {
        if (!loading && !loadError) return
        loading = true; loadError = false
        viewModelScope.launch {
            try { repository.observeWhatsThatGames().collect { games = it; loading = false } }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { loadError = true; loading = false }
        }
    }
    fun requestDelete(id: String) { if (!deleting && deleteId == null && games.any { it.id == id && !it.isBuiltIn }) { deleteId = id; deleteError = false } }
    fun cancelDelete() { if (!deleting) { deleteId = null; deleteError = false } }
    fun delete() {
        val id = deleteId ?: return
        if (deleting) return
        deleting = true; deleteError = false
        viewModelScope.launch {
            try { repository.deleteCustomGame(id); games = games.filterNot { it.id == id }; deleteId = null }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { deleteError = true }
            finally { deleting = false }
        }
    }
    class Factory(private val repository: GameRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T = modelClass.cast(GameSelectionViewModel(repository))!!
    }
}
