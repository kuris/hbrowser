package kr.chriskim.hbrowser.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepo: SettingsRepository
) : ViewModel() {

    val isAdBlock: StateFlow<Boolean> = settingsRepo.isAdBlock
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val isPopupBlocked: StateFlow<Boolean> = settingsRepo.isPopupBlocked
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val isConfirmDownload: StateFlow<Boolean> = settingsRepo.isConfirmDownload
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val isWifiOnly: StateFlow<Boolean> = settingsRepo.isWifiOnly
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val useFgDownloader: StateFlow<Boolean> = settingsRepo.useFgDownloader
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun setAdBlock(v: Boolean)         = viewModelScope.launch { settingsRepo.setAdBlock(v) }
    fun setPopupBlocked(v: Boolean)    = viewModelScope.launch { settingsRepo.setPopupBlocked(v) }
    fun setConfirmDownload(v: Boolean) = viewModelScope.launch { settingsRepo.setConfirmDownload(v) }
    fun setWifiOnly(v: Boolean)        = viewModelScope.launch { settingsRepo.setWifiOnly(v) }
    fun setUseFgDownloader(v: Boolean) = viewModelScope.launch { settingsRepo.setUseFgDownloader(v) }
}
