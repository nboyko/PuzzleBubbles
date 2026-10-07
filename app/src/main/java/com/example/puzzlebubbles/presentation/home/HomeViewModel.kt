package com.example.puzzlebubbles.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.puzzlebubbles.domain.GameRepository

class HomeViewModel(repository: GameRepository) : ViewModel() {
    val games = repository.getGameCatalog().toList()

    class Factory(private val repository: GameRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(HomeViewModel::class.java))
            return modelClass.cast(HomeViewModel(repository))!!
        }
    }
}
