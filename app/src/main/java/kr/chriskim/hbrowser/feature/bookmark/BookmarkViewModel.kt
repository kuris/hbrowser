package kr.chriskim.hbrowser.feature.bookmark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kr.chriskim.hbrowser.core.database.BookmarkEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BookmarkViewModel(
    private val bookmarkRepo: BookmarkRepository
) : ViewModel() {

    val bookmarks: StateFlow<List<BookmarkEntity>> = bookmarkRepo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun remove(id: Long) = viewModelScope.launch { bookmarkRepo.remove(id) }
}
