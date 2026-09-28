package kr.chriskim.hbrowser.feature.download

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kr.chriskim.hbrowser.core.database.DownloadItemEntity
import kr.chriskim.hbrowser.core.model.DownloadStatus
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class DownloadViewModel(
    private val downloadRepo: DownloadRepository
) : ViewModel() {

    val downloads: StateFlow<List<DownloadItemEntity>> = downloadRepo
        .observeDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // 필터 상태
    private val _filter = MutableStateFlow(DownloadFilter.ALL)
    val filter: StateFlow<DownloadFilter> = _filter.asStateFlow()

    val filteredDownloads: StateFlow<List<DownloadItemEntity>> = combine(
        downloads, filter
    ) { list, f ->
        when (f) {
            DownloadFilter.ALL        -> list
            DownloadFilter.ACTIVE     -> list.filter { it.status in activeStatuses }
            DownloadFilter.COMPLETED  -> list.filter { it.status == DownloadStatus.COMPLETED }
            DownloadFilter.FAILED     -> list.filter { it.status in failedStatuses }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setFilter(f: DownloadFilter) { _filter.value = f }

    fun syncStatus() = viewModelScope.launch { downloadRepo.syncStatus() }

    fun cancelDownload(id: Long) = viewModelScope.launch {
        downloadRepo.cancelDownload(id)
    }

    fun pauseDownload(id: Long) = viewModelScope.launch {
        downloadRepo.pauseDownload(id)
    }

    fun resumeDownload(id: Long) = viewModelScope.launch {
        downloadRepo.resumeDownload(id)
    }

    fun deleteDownload(id: Long) = viewModelScope.launch {
        downloadRepo.deleteDownload(id)
    }

    fun openFile(context: Context, item: DownloadItemEntity) {
        val path = item.localPath ?: return
        val file = File(path)
        if (!file.exists()) return
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val mime = item.mimeType ?: "*/*"
            context.startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mime)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 진행률 계산 (0f ~ 1f)
    fun progressOf(item: DownloadItemEntity): Float {
        val total = item.contentLength ?: return 0f
        if (total <= 0) return 0f
        return (item.downloadedBytes.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    }

    companion object {
        private val activeStatuses = setOf(
            DownloadStatus.QUEUED,
            DownloadStatus.RUNNING,
            DownloadStatus.PAUSED
        )
        private val failedStatuses = setOf(
            DownloadStatus.FAILED,
            DownloadStatus.CANCELED
        )
    }
}

enum class DownloadFilter { ALL, ACTIVE, COMPLETED, FAILED }
