package kr.chriskim.hbrowser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kr.chriskim.hbrowser.core.ui.theme.HBrowserTheme
import kr.chriskim.hbrowser.feature.bookmark.BookmarkScreen
import kr.chriskim.hbrowser.feature.bookmark.BookmarkViewModel
import kr.chriskim.hbrowser.feature.browser.BrowserScreen
import kr.chriskim.hbrowser.feature.browser.BrowserViewModel
import kr.chriskim.hbrowser.feature.download.DownloadListScreen
import kr.chriskim.hbrowser.feature.download.DownloadViewModel
import kr.chriskim.hbrowser.feature.history.HistoryScreen
import kr.chriskim.hbrowser.feature.history.HistoryViewModel
import kr.chriskim.hbrowser.feature.settings.SettingsScreen
import kr.chriskim.hbrowser.feature.settings.SettingsViewModel

object Routes {
    const val BROWSER   = "browser"
    const val DOWNLOADS = "downloads"
    const val HISTORY   = "history"
    const val BOOKMARKS = "bookmarks"
    const val SETTINGS  = "settings"
}

class MainActivity : ComponentActivity() {

    // ViewModelFactory helpers (AppContainer 기반)
    private fun <T : androidx.lifecycle.ViewModel> vmFactory(creator: () -> T) =
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <V : androidx.lifecycle.ViewModel> create(cls: Class<V>): V = creator() as V
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val c = appContainer   // AppContainer

        setContent {
            HBrowserTheme {
                val navController = rememberNavController()

                // ViewModel 인스턴스 (Activity 범위)
                val browserVm: BrowserViewModel = viewModel(
                    factory = vmFactory { c.createBrowserViewModel() })
                val downloadVm: DownloadViewModel = viewModel(
                    factory = vmFactory { c.createDownloadViewModel() })
                val settingsVm: SettingsViewModel = viewModel(
                    factory = vmFactory { c.createSettingsViewModel() })
                val historyVm: HistoryViewModel = viewModel(
                    factory = vmFactory { c.createHistoryViewModel() })
                val bookmarkVm: BookmarkViewModel = viewModel(
                    factory = vmFactory { c.createBookmarkViewModel() })

                NavHost(
                    navController    = navController,
                    startDestination = Routes.BROWSER
                ) {
                    composable(Routes.BROWSER) {
                        BrowserScreen(
                            vm                    = browserVm,
                            adBlockEngine         = c.adBlockEngine,
                            onNavigateToDownloads = { navController.navigate(Routes.DOWNLOADS) },
                            onNavigateToHistory   = { navController.navigate(Routes.HISTORY) },
                            onNavigateToBookmarks = { navController.navigate(Routes.BOOKMARKS) },
                            onNavigateToSettings  = { navController.navigate(Routes.SETTINGS) }
                        )
                    }

                    composable(Routes.DOWNLOADS) {
                        DownloadListScreen(
                            vm     = downloadVm,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(Routes.HISTORY) {
                        HistoryScreen(
                            vm           = historyVm,
                            onNavigateTo = { url -> browserVm.navigate(url); navController.popBackStack(Routes.BROWSER, false) },
                            onBack       = { navController.popBackStack() }
                        )
                    }

                    composable(Routes.BOOKMARKS) {
                        BookmarkScreen(
                            vm           = bookmarkVm,
                            onNavigateTo = { url -> browserVm.navigate(url); navController.popBackStack(Routes.BROWSER, false) },
                            onBack       = { navController.popBackStack() }
                        )
                    }

                    composable(Routes.SETTINGS) {
                        SettingsScreen(
                            vm     = settingsVm,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
