package kr.chriskim.hbrowser

import android.app.Application
import android.content.Context
import kr.chriskim.hbrowser.core.database.AppDatabase
import kr.chriskim.hbrowser.feature.adblock.AdBlockRepository
import kr.chriskim.hbrowser.feature.adblock.engine.AdBlockEngine
import kr.chriskim.hbrowser.feature.adblock.engine.EasyListAdBlockEngine
import kr.chriskim.hbrowser.feature.bookmark.BookmarkRepository
import kr.chriskim.hbrowser.feature.bookmark.BookmarkViewModel
import kr.chriskim.hbrowser.feature.browser.BrowserViewModel
import kr.chriskim.hbrowser.feature.download.DownloadRepository
import kr.chriskim.hbrowser.feature.download.DownloadRepositoryImpl
import kr.chriskim.hbrowser.feature.download.DownloadViewModel
import kr.chriskim.hbrowser.feature.download.manager.SystemDownloadManager
import kr.chriskim.hbrowser.feature.history.HistoryRepository
import kr.chriskim.hbrowser.feature.history.HistoryViewModel
import kr.chriskim.hbrowser.feature.media.MediaCandidateStore
import kr.chriskim.hbrowser.feature.settings.SettingsRepository
import kr.chriskim.hbrowser.feature.settings.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * ServiceLocator / AppContainer 패턴 (Hilt 없이)
 */
class AppContainer(private val app: Application) {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ── DB ────────────────────────────────────────────────────
    val db: AppDatabase by lazy { AppDatabase.getInstance(app) }

    // ── Settings ──────────────────────────────────────────────
    val settingsRepo: SettingsRepository by lazy { SettingsRepository(app) }

    // ── AdBlock ───────────────────────────────────────────────
    val adBlockEngine: AdBlockEngine by lazy {
        EasyListAdBlockEngine(app).also { engine ->
            appScope.launch { engine.initialize() }
        }
    }
    val adBlockRepo: AdBlockRepository by lazy {
        AdBlockRepository(db.adBlockRuleDao(), adBlockEngine).also { repo ->
            appScope.launch { repo.loadExceptionsToEngine() }
        }
    }

    // ── Download ──────────────────────────────────────────────
    val sysDm: SystemDownloadManager by lazy { SystemDownloadManager(app) }
    val downloadRepo: DownloadRepository by lazy { DownloadRepositoryImpl(app, db, sysDm) }

    // ── History ───────────────────────────────────────────────
    val historyRepo: HistoryRepository by lazy { HistoryRepository(db) }

    // ── Bookmark ──────────────────────────────────────────────
    val bookmarkRepo: BookmarkRepository by lazy { BookmarkRepository(db) }

    // ── Media ─────────────────────────────────────────────────
    val mediaCandidateStore: MediaCandidateStore by lazy { MediaCandidateStore() }

    // ── ViewModels ────────────────────────────────────────────
    fun createBrowserViewModel() = BrowserViewModel(
        adBlockEngine      = adBlockEngine,
        mediaCandidateStore = mediaCandidateStore,
        downloadRepo       = downloadRepo,
        settingsRepo       = settingsRepo,
        historyRepo        = historyRepo,
        bookmarkRepo       = bookmarkRepo,
        adBlockRepo        = adBlockRepo
    )

    fun createDownloadViewModel()  = DownloadViewModel(downloadRepo)
    fun createSettingsViewModel()  = SettingsViewModel(settingsRepo)
    fun createHistoryViewModel()   = HistoryViewModel(historyRepo)
    fun createBookmarkViewModel()  = BookmarkViewModel(bookmarkRepo)
}

class HBrowserApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

// 편의 확장 함수
val Context.appContainer: AppContainer
    get() = (applicationContext as HBrowserApp).container
