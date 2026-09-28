package kr.chriskim.hbrowser.feature.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kr.chriskim.hbrowser.core.database.HistoryEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    vm: HistoryViewModel,
    onNavigateTo: (String) -> Unit,
    onBack: () -> Unit
) {
    val histories by vm.histories.collectAsState()
    val query     by vm.query.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("방문 기록") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                actions = {
                    if (histories.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = "전체 삭제")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            // ── 검색창 ──────────────────────────────────────────────
            OutlinedTextField(
                value         = query,
                onValueChange = vm::setQuery,
                modifier      = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder   = { Text("기록 검색...") },
                leadingIcon   = { Icon(Icons.Filled.Search, null) },
                trailingIcon  = if (query.isNotEmpty()) ({
                    IconButton(onClick = { vm.setQuery("") }) {
                        Icon(Icons.Filled.Close, null)
                    }
                }) else null,
                singleLine    = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )

            // ── 목록 ─────────────────────────────────────────────────
            if (histories.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.History,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            if (query.isBlank()) "방문 기록이 없습니다." else "검색 결과가 없습니다.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                LazyColumn {
                    items(histories, key = { it.id }) { item ->
                        HistoryRow(
                            item      = item,
                            onNavigate = { onNavigateTo(item.url); onBack() },
                            onDelete   = { vm.delete(item.id) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    // ── 전체 삭제 확인 다이얼로그 ─────────────────────────────────────
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title   = { Text("방문 기록 삭제") },
            text    = { Text("모든 방문 기록을 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = { showClearDialog = false; vm.clearAll() }) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("취소") }
            }
        )
    }
}

@Composable
private fun HistoryRow(
    item: HistoryEntity,
    onNavigate: () -> Unit,
    onDelete: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                item.title.ifBlank { item.url },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                item.url,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        },
        leadingContent = {
            Icon(
                Icons.Filled.Language,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatTime(item.visitedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Spacer(Modifier.height(4.dp))
                IconButton(
                    onClick  = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "삭제",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        },
        modifier = Modifier.clickable { onNavigate() }
    )
}

private fun formatTime(millis: Long): String {
    val now   = System.currentTimeMillis()
    val delta = now - millis
    return when {
        delta < 60_000L         -> "방금 전"
        delta < 3_600_000L      -> "${delta / 60_000}분 전"
        delta < 86_400_000L     -> "${delta / 3_600_000}시간 전"
        delta < 604_800_000L    -> "${delta / 86_400_000}일 전"
        else -> SimpleDateFormat("MM.dd", Locale.getDefault()).format(Date(millis))
    }
}
