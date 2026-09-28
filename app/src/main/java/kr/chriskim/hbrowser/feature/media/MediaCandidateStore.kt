package kr.chriskim.hbrowser.feature.media

import kr.chriskim.hbrowser.core.model.MediaCandidate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MediaCandidateStore {
    private val _candidates = MutableStateFlow<List<MediaCandidate>>(emptyList())
    val candidates: StateFlow<List<MediaCandidate>> = _candidates.asStateFlow()
    private var currentPage = ""

    fun addCandidate(c: MediaCandidate) {
        if (c.url.length < 10 || c.url.startsWith("data:") || c.url.startsWith("blob:")) return
        val cur = _candidates.value
        if (cur.any { it.url == c.url }) return
        _candidates.value = cur + c
    }

    fun addAll(list: List<MediaCandidate>) = list.forEach { addCandidate(it) }

    fun clearForNewPage(url: String) {
        if (url != currentPage) { currentPage = url; _candidates.value = emptyList() }
    }

    fun clear() { _candidates.value = emptyList() }
    fun count() = _candidates.value.size
}
