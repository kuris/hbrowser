package kr.chriskim.hbrowser.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kr.chriskim.hbrowser.core.database.HistoryEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val historyRepo: HistoryRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val histories: StateFlow<List<HistoryEntity>> = _query
        .debounce(200)
        .flatMapLatest { q ->
            if (q.isBlank()) historyRepo.observeAll()
            else flow { emit(historyRepo.search(q)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(q: String) { _query.value = q }

    fun delete(id: Long) = viewModelScope.launch { historyRepo.delete(id) }

    fun clearAll() = viewModelScope.launch { historyRepo.clearAll() }
}
