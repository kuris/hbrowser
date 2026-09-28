package kr.chriskim.hbrowser.feature.browser

import android.webkit.CookieManager
import android.webkit.WebView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kr.chriskim.hbrowser.core.model.DownloadEngine
import kr.chriskim.hbrowser.core.model.MediaCandidate
import kr.chriskim.hbrowser.core.model.MediaSource
import kr.chriskim.hbrowser.core.model.MediaType
import kr.chriskim.hbrowser.core.util.FileNameUtils
import kr.chriskim.hbrowser.core.util.UrlUtils
import kr.chriskim.hbrowser.feature.adblock.AdBlockRepository
import kr.chriskim.hbrowser.feature.adblock.engine.AdBlockEngine
import kr.chriskim.hbrowser.feature.bookmark.BookmarkRepository
import kr.chriskim.hbrowser.feature.download.DownloadRepository
import kr.chriskim.hbrowser.feature.download.DownloadRequest
import kr.chriskim.hbrowser.feature.history.HistoryRepository
import kr.chriskim.hbrowser.feature.media.MediaCandidateStore
import kr.chriskim.hbrowser.feature.settings.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BrowserViewModel(
    val adBlockEngine: AdBlockEngine,
    val mediaCandidateStore: MediaCandidateStore,
    private val downloadRepo: DownloadRepository,
    private val settingsRepo: SettingsRepository,
    private val historyRepo: HistoryRepository,
    private val bookmarkRepo: BookmarkRepository,
    private val adBlockRepo: AdBlockRepository
) : ViewModel() {

    // ── 브라우저 상태 ─────────────────────────────────────────
    private val _url      = MutableStateFlow(UrlUtils.HOME_URL)
    val currentUrl: StateFlow<String> = _url.asStateFlow()

    private val _title    = MutableStateFlow("")
    val pageTitle: StateFlow<String> = _title.asStateFlow()

    // ── 네비게이션 요청 (외부에서 WebView.loadUrl 트리거) ──────────
    private val _navigationRequest = MutableStateFlow<String?>(null)
    val navigationRequest: StateFlow<String?> = _navigationRequest.asStateFlow()

    fun navigate(url: String) { _navigationRequest.value = processInput(url) }
    fun clearNavigationRequest() { _navigationRequest.value = null }
    fun onTitleReceived(title: String) { _title.value = title }

    private val _progress = MutableStateFlow(100)
    val loadingProgress: StateFlow<Int> = _progress.asStateFlow()

    private val _canBack  = MutableStateFlow(false)
    val canGoBack: StateFlow<Boolean> = _canBack.asStateFlow()

    private val _canFwd   = MutableStateFlow(false)
    val canGoForward: StateFlow<Boolean> = _canFwd.asStateFlow()

    // ── 광고차단 ──────────────────────────────────────────────
    private val _blocked  = MutableStateFlow(0)
    val blockedCount: StateFlow<Int> = _blocked.asStateFlow()

    val isAdBlock: StateFlow<Boolean> = settingsRepo.isAdBlock
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    // ── 북마크 ────────────────────────────────────────────────
    private val _isBookmarked = MutableStateFlow(false)
    val isBookmarked: StateFlow<Boolean> = _isBookmarked.asStateFlow()

    // ── 미디어 감지 ───────────────────────────────────────────
    val mediaCandidates: StateFlow<List<MediaCandidate>> = mediaCandidateStore.candidates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── 다운로드 ──────────────────────────────────────────────
    private val _downloadDialog = MutableStateFlow<PendingDownload?>(null)
    val showDownloadDialog: StateFlow<PendingDownload?> = _downloadDialog.asStateFlow()

    private val _mediaSheet = MutableStateFlow(false)
    val showMediaSheet: StateFlow<Boolean> = _mediaSheet.asStateFlow()

    val confirmDownload: StateFlow<Boolean> = settingsRepo.isConfirmDownload
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val wifiOnly: StateFlow<Boolean> = settingsRepo.isWifiOnly
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val useFgDownloader: StateFlow<Boolean> = settingsRepo.useFgDownloader
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    // ── URL 처리 ──────────────────────────────────────────────
    fun processInput(s: String): String = UrlUtils.processInput(s)

    fun onPageStarted(url: String) {
        _url.value = url
        _progress.value = 0
        _blocked.value = 0
        mediaCandidateStore.clearForNewPage(url)
        checkBookmark(url)
        checkAdBlockException(url)
    }

    fun onPageFinished(url: String, title: String?) {
        _url.value = url; _title.value = title ?: url; _progress.value = 100
        viewModelScope.launch {
            if (url.startsWith("http")) historyRepo.add(url, title ?: url)
        }
        checkBookmark(url)
    }

    fun onProgressChanged(p: Int) { _progress.value = p }

    fun updateNav(wv: WebView) { _canBack.value = wv.canGoBack(); _canFwd.value = wv.canGoForward() }

    fun onAdBlocked() { _blocked.value += 1 }

    // ── 북마크 ────────────────────────────────────────────────
    private fun checkBookmark(url: String) {
        viewModelScope.launch { _isBookmarked.value = bookmarkRepo.isBookmarked(url) }
    }

    fun toggleBookmark() {
        viewModelScope.launch {
            val bookmarked = bookmarkRepo.toggle(_url.value, _title.value)
            _isBookmarked.value = bookmarked
        }
    }

    // ── 광고차단 예외 ─────────────────────────────────────────
    private val _isAdBlockException = MutableStateFlow(false)
    val isAdBlockException: StateFlow<Boolean> = _isAdBlockException.asStateFlow()

    private fun checkAdBlockException(url: String) {
        val host = UrlUtils.extractHost(url) ?: return
        viewModelScope.launch { _isAdBlockException.value = adBlockRepo.isException(host) }
    }

    fun toggleAdBlockException() {
        val host = UrlUtils.extractHost(_url.value) ?: return
        viewModelScope.launch {
            if (_isAdBlockException.value) { adBlockRepo.removeException(host); _isAdBlockException.value = false }
            else { adBlockRepo.addException(host); _isAdBlockException.value = true }
        }
    }

    // ── 다운로드 ──────────────────────────────────────────────
    fun onDownloadRequested(url: String, ua: String?, disp: String?, mime: String?, len: Long, pageUrl: String) {
        val name    = FileNameUtils.guess(url, disp, mime)
        val cookies = CookieManager.getInstance().getCookie(url)
        mediaCandidateStore.addCandidate(
            MediaCandidate.fromUrl(url, pageUrl, MediaSource.DOWNLOAD_LISTENER, mime))
        val pending = PendingDownload(url, name, mime, if (len > 0) len else null, pageUrl, ua, cookies)
        if (confirmDownload.value) _downloadDialog.value = pending
        else doStartDownload(pending)
    }

    fun confirmDownload()  { val p = _downloadDialog.value ?: return; _downloadDialog.value = null; doStartDownload(p) }
    fun cancelDownload()   { _downloadDialog.value = null }

    private fun doStartDownload(p: PendingDownload) {
        val engine = if (useFgDownloader.value) DownloadEngine.FOREGROUND else DownloadEngine.SYSTEM
        val isHls  = p.url.lowercase().contains(".m3u8") || p.mimeType?.contains("mpegurl") == true
        viewModelScope.launch {
            downloadRepo.startDownload(DownloadRequest(
                url = p.url, fileName = p.fileName, mimeType = p.mimeType,
                pageUrl = p.pageUrl, userAgent = p.userAgent,
                referer = p.pageUrl, cookies = p.cookies,
                contentLength = p.contentLength, isHls = isHls,
                wifiOnly = wifiOnly.value, engine = engine
            ))
        }
    }

    fun downloadMediaCandidate(c: MediaCandidate, ua: String?) {
        val cookies = CookieManager.getInstance().getCookie(c.url)
        val engine  = if (useFgDownloader.value) DownloadEngine.FOREGROUND else DownloadEngine.SYSTEM
        val isHls   = c.type == MediaType.HLS
        viewModelScope.launch {
            downloadRepo.startDownload(DownloadRequest(
                url = c.url, fileName = c.fileName, mimeType = c.mimeType,
                pageUrl = c.pageUrl, userAgent = ua,
                referer = c.pageUrl, cookies = cookies,
                contentLength = null, isHls = isHls,
                wifiOnly = wifiOnly.value, engine = engine
            ))
        }
    }

    fun toggleMediaSheet()  { _mediaSheet.value = !_mediaSheet.value }
    fun hideMediaSheet()    { _mediaSheet.value = false }
}

data class PendingDownload(
    val url: String, val fileName: String, val mimeType: String?,
    val contentLength: Long?, val pageUrl: String,
    val userAgent: String?, val cookies: String?
)
