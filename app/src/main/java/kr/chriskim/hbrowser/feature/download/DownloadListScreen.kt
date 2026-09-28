package kr.chriskim.hbrowser.feature.download

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.chriskim.hbrowser.core.database.DownloadItemEntity
import kr.chriskim.hbrowser.core.model.DownloadStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadListScreen(
    vm: DownloadViewModel,
    onBack: () -> Unit
) {
    val context         = LocalContext.current
    val items           by vm.filteredDownloads.collectAsState()
    val selectedFilter  by vm.filter.collectAsState()

    LaunchedEffect(Unit) { vm.syncStatus() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("다운로드") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            // ── 필터 탭 ─────────────────────────────────────────────
            ScrollableTabRow(
                selectedTabIndex = selectedFilter.ordinal,
                edgePadding      = 16.dp
            ) {
                DownloadFilter.values().forEach { filter ->
                    Tab(
                        selected = selectedFilter == filter,
                        onClick  = { vm.setFilter(filter) },
                        text     = {
                            Text(
                                when (filter) {
                                    DownloadFilter.ALL       -> "전체"
                                    DownloadFilter.ACTIVE    -> "진행 중"
                                    DownloadFilter.COMPLETED -> "완료"
                                    DownloadFilter.FAILED    -> "실패"
                                }
                            )
                        }
                    )
                }
            }

            // ── 목록 ────────────────────────────────────────────────
            if (items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Download,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "다운로드 내역이 없습니다.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding    = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        DownloadItemRow(
                            item      = item,
                            progress  = vm.progressOf(item),
                            onPause   = { vm.pauseDownload(item.id) },
                            onResume  = { vm.resumeDownload(item.id) },
                            onCancel  = { vm.cancelDownload(item.id) },
                            onDelete  = { vm.deleteDownload(item.id) },
                            onOpen    = { vm.openFile(context, item) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 개별 다운로드 항목 행
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DownloadItemRow(
    item: DownloadItemEntity,
    progress: Float,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onOpen: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // 파일명 + 상태 아이콘
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusIcon(item.status, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text     = item.fileName,
                style    = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (item.isHls) {
                SuggestionChip(
                    onClick = {},
                    label   = { Text("HLS", fontSize = 10.sp) },
                    modifier = Modifier.height(24.dp)
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // 진행 바 (활성 중일 때)
        if (item.status == DownloadStatus.RUNNING || item.status == DownloadStatus.PAUSED) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(2.dp))
            // 바이트 표시
            val dl   = formatBytes(item.downloadedBytes)
            val total = item.contentLength?.let { formatBytes(it) } ?: "??"
            Text("$dl / $total", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }

        if (item.status == DownloadStatus.QUEUED) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        // 오류 이유 표시
        if (item.status == DownloadStatus.FAILED && !item.reason.isNullOrBlank()) {
            Text(
                "실패: ${item.reason}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(4.dp))

        // 액션 버튼 행
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            when (item.status) {
                DownloadStatus.RUNNING -> {
                    TextButton(onClick = onPause) { Text("일시정지") }
                    TextButton(onClick = onCancel) { Text("취소") }
                }
                DownloadStatus.PAUSED -> {
                    TextButton(onClick = onResume) { Text("재개") }
                    TextButton(onClick = onCancel) { Text("취소") }
                }
                DownloadStatus.QUEUED -> {
                    TextButton(onClick = onCancel) { Text("취소") }
                }
                DownloadStatus.COMPLETED -> {
                    TextButton(onClick = onOpen) { Text("열기") }
                    TextButton(onClick = onDelete) { Text("삭제") }
                }
                DownloadStatus.FAILED, DownloadStatus.CANCELED -> {
                    TextButton(onClick = onDelete) { Text("삭제") }
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun StatusIcon(status: DownloadStatus, modifier: Modifier = Modifier) {
    val (icon, tint) = when (status) {
        DownloadStatus.QUEUED    -> Icons.Filled.AccessTime to MaterialTheme.colorScheme.tertiary
        DownloadStatus.RUNNING   -> Icons.Filled.Download   to MaterialTheme.colorScheme.primary
        DownloadStatus.PAUSED    -> Icons.Filled.Pause      to MaterialTheme.colorScheme.secondary
        DownloadStatus.COMPLETED -> Icons.Filled.CheckCircle to MaterialTheme.colorScheme.primary
        DownloadStatus.FAILED    -> Icons.Filled.Error      to MaterialTheme.colorScheme.error
        DownloadStatus.CANCELED -> Icons.Filled.Cancel     to MaterialTheme.colorScheme.outline
        else                     -> Icons.Filled.HelpOutline to MaterialTheme.colorScheme.outline
    }
    Icon(icon, contentDescription = status.name, modifier = modifier, tint = tint)
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_073_741_824L -> "%.1f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576L     -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1_024L         -> "%.1f KB".format(bytes / 1_024.0)
    else                    -> "$bytes B"
}
