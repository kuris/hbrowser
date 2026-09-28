package kr.chriskim.hbrowser.feature.browser

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import kr.chriskim.hbrowser.core.model.MediaCandidate
import kr.chriskim.hbrowser.core.model.MediaType
import kr.chriskim.hbrowser.feature.adblock.engine.AdBlockEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    vm: BrowserViewModel,
    adBlockEngine: AdBlockEngine,
    onNavigateToDownloads: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToBookmarks: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    val currentUrl     by vm.currentUrl.collectAsState()
    val pageTitle      by vm.pageTitle.collectAsState()
    val progress       by vm.loadingProgress.collectAsState()
    val canBack        by vm.canGoBack.collectAsState()
    val canFwd         by vm.canGoForward.collectAsState()
    val blockedCount   by vm.blockedCount.collectAsState()
    val isAdBlock      by vm.isAdBlock.collectAsState()
    val isBookmarked   by vm.isBookmarked.collectAsState()
    val isException    by vm.isAdBlockException.collectAsState()
    val mediaCandidates by vm.mediaCandidates.collectAsState()
    val downloadDialog by vm.showDownloadDialog.collectAsState()
    val showMediaSheet by vm.showMediaSheet.collectAsState()

    val focusManager   = LocalFocusManager.current
    val density        = LocalDensity.current

    var addressText    by remember { mutableStateOf("") }
    var isAddressBarFocused by remember { mutableStateOf(false) }

    // URL 변경 시 주소창 동기화
    LaunchedEffect(currentUrl) {
        if (!isAddressBarFocused) {
            addressText = kr.chriskim.hbrowser.core.util.UrlUtils.displayUrl(currentUrl)
        }
    }

    // WebView 참조
    var webView by remember { mutableStateOf<android.webkit.WebView?>(null) }

    Scaffold(
        bottomBar = {
            // ── 하단 네비게이션 바 (뒤로/앞으로/홈) ──────────────────
            NavigationBar(tonalElevation = 4.dp) {
                NavigationBarItem(
                    icon     = { Icon(Icons.Filled.ArrowBack, null) },
                    label    = { Text("뒤로") },
                    selected = false,
                    enabled  = canBack,
                    onClick  = { webView?.goBack() }
                )
                NavigationBarItem(
                    icon     = { Icon(Icons.Filled.Home, null) },
                    label    = { Text("홈") },
                    selected = false,
                    onClick  = { vm.navigate(kr.chriskim.hbrowser.core.util.UrlUtils.HOME_URL) }
                )
                NavigationBarItem(
                    icon     = { Icon(Icons.Filled.ArrowForward, null) },
                    label    = { Text("앞으로") },
                    selected = false,
                    enabled  = canFwd,
                    onClick  = { webView?.goForward() }
                )
            }
        }
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            // ── 주소창 행 + 로딩 바 ───────────────────────────────────
            Surface(
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 8.dp)
                ) {
                    AdBlockBadge(
                        enabled      = isAdBlock && !isException,
                        blockedCount = blockedCount,
                        isException  = isException
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        OutlinedTextField(
                            value         = addressText,
                            onValueChange = { addressText = it },
                            modifier      = Modifier.fillMaxWidth(),
                            singleLine    = true,
                            textStyle     = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp).copy(textAlign = TextAlign.Start),
                            placeholder   = { Text("주소 또는 검색어 입력", fontSize = 13.sp) },
                            leadingIcon   = if (progress in 1..99) ({
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            }) else null,
                            trailingIcon  = if (addressText.isNotEmpty()) ({
                                IconButton(onClick = {
                                    addressText = ""
                                }) {
                                    Icon(Icons.Filled.Close, contentDescription = "지우기", modifier = Modifier.size(16.dp))
                                }
                            }) else null,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction    = ImeAction.Go
                            ),
                            keyboardActions = KeyboardActions(onGo = {
                                isAddressBarFocused = false
                                vm.navigate(addressText)
                            }),
                            shape           = RoundedCornerShape(20.dp),
                            colors          = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = { vm.toggleBookmark() }) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = "북마크",
                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    BadgedBox(
                        badge = {
                            if (mediaCandidates.isNotEmpty())
                                Badge { Text(mediaCandidates.size.toString()) }
                        }
                    ) {
                        IconButton(onClick = { vm.toggleMediaSheet() }) {
                            Icon(Icons.Filled.Download, contentDescription = "미디어 다운로드")
                        }
                    }
                    BrowserOverflowMenu(
                        isAdBlock         = isAdBlock,
                        isException       = isException,
                        onToggleException = { vm.toggleAdBlockException() },
                        onRefresh         = { webView?.reload() },
                        onNavigateToDownloads  = onNavigateToDownloads,
                        onNavigateToHistory    = onNavigateToHistory,
                        onNavigateToBookmarks  = onNavigateToBookmarks,
                        onNavigateToSettings   = onNavigateToSettings,
                    )
                }
                AnimatedVisibility(visible = progress in 1..99) {
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ── WebView ───────────────────────────────────────────────
            Box(Modifier.weight(1f)) {
                BrowserWebView(
                    vm             = vm,
                    adBlockEngine  = adBlockEngine,
                    modifier       = Modifier.fillMaxSize(),
                    onWebViewCreated = { wv -> webView = wv }
                )
            }
        }
    }

    // ── 다운로드 확인 다이얼로그 ──────────────────────────────────────
    downloadDialog?.let { pending ->
        DownloadConfirmDialog(
            pending   = pending,
            onConfirm = { vm.confirmDownload() },
            onDismiss = { vm.cancelDownload() }
        )
    }

    // ── 미디어 바텀시트 ───────────────────────────────────────────────
    if (showMediaSheet) {
        MediaBottomSheet(
            candidates = mediaCandidates,
            onDownload = { candidate -> vm.downloadMediaCandidate(candidate, null) },
            onDismiss  = { vm.hideMediaSheet() }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 주소창 Composable
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AddressBar(
    text: String,
    onTextChange: (String) -> Unit,
    isLoading: Boolean,
    onGo: () -> Unit,
    onFocused: () -> Unit,
    onDismissFocus: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value         = text,
        onValueChange = onTextChange,
        modifier      = modifier
            .height(48.dp)
            .focusRequester(focusRequester),
        singleLine    = true,
        textStyle     = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp).copy(textAlign = TextAlign.Center),
        placeholder   = { Text("주소 또는 검색어 입력", fontSize = 13.sp, textAlign = TextAlign.Center) },
        leadingIcon   = if (isLoading) ({
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        }) else ({
            Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
        }),
        trailingIcon  = if (text.isNotEmpty()) ({
            IconButton(onClick = { onTextChange(""); onDismissFocus() }) {
                Icon(Icons.Filled.Close, contentDescription = "지우기", modifier = Modifier.size(16.dp))
            }
        }) else null,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Uri,
            imeAction    = ImeAction.Go
        ),
        keyboardActions = KeyboardActions(onGo = { onGo() }),
        shape           = RoundedCornerShape(24.dp),
        colors          = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        )
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// 광고차단 배지 아이콘
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AdBlockBadge(
    enabled: Boolean,
    blockedCount: Int,
    isException: Boolean
) {
    BadgedBox(
        badge = {
            if (enabled && blockedCount > 0) {
                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                    Text(if (blockedCount > 99) "99+" else blockedCount.toString(), fontSize = 9.sp)
                }
            }
        }
    ) {
        Icon(
            imageVector = Icons.Filled.Shield,
            contentDescription = "광고차단",
            tint = when {
                isException -> MaterialTheme.colorScheme.outline
                enabled     -> MaterialTheme.colorScheme.primary
                else        -> MaterialTheme.colorScheme.outline
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 오버플로우 메뉴
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun BrowserOverflowMenu(
    isAdBlock: Boolean,
    isException: Boolean,
    onToggleException: () -> Unit,
    onRefresh: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToBookmarks: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Filled.MoreVert, contentDescription = "메뉴")
    }
    DropdownMenu(
        expanded        = expanded,
        onDismissRequest = { expanded = false }
    ) {
        DropdownMenuItem(
            text    = { Text("새로고침") },
            onClick = { expanded = false; onRefresh() },
            leadingIcon = { Icon(Icons.Filled.Refresh, null) }
        )
        if (isAdBlock) {
            DropdownMenuItem(
                text = {
                    Text(if (isException) "이 사이트 광고차단 사용" else "이 사이트 광고차단 예외")
                },
                onClick = { expanded = false; onToggleException() },
                leadingIcon = { Icon(Icons.Filled.Block, null) }
            )
        }
        HorizontalDivider()
        DropdownMenuItem(
            text    = { Text("다운로드") },
            onClick = { expanded = false; onNavigateToDownloads() },
            leadingIcon = { Icon(Icons.Filled.Download, null) }
        )
        DropdownMenuItem(
            text    = { Text("방문 기록") },
            onClick = { expanded = false; onNavigateToHistory() },
            leadingIcon = { Icon(Icons.Filled.History, null) }
        )
        DropdownMenuItem(
            text    = { Text("북마크") },
            onClick = { expanded = false; onNavigateToBookmarks() },
            leadingIcon = { Icon(Icons.Filled.BookmarkBorder, null) }
        )
        HorizontalDivider()
        DropdownMenuItem(
            text    = { Text("설정") },
            onClick = { expanded = false; onNavigateToSettings() },
            leadingIcon = { Icon(Icons.Filled.Settings, null) }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 다운로드 확인 다이얼로그
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DownloadConfirmDialog(
    pending: PendingDownload,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title   = { Text("다운로드") },
        text    = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("파일: ${pending.fileName}", style = MaterialTheme.typography.bodyMedium)
                pending.mimeType?.let {
                    Text("형식: $it", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
                pending.contentLength?.let {
                    val mb = "%.1f MB".format(it / 1_048_576.0)
                    Text("크기: $mb", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("다운로드") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// 미디어 바텀시트
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaBottomSheet(
    candidates: List<MediaCandidate>,
    onDownload: (MediaCandidate) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState
    ) {
        Column(Modifier.padding(bottom = 32.dp)) {
            Text(
                text  = "감지된 미디어 (${candidates.size}개)",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            if (candidates.isEmpty()) {
                Text(
                    "감지된 미디어가 없습니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            } else {
                LazyColumn {
                    items(candidates, key = { it.url }) { candidate ->
                        MediaCandidateRow(
                            candidate  = candidate,
                            onDownload = { onDownload(candidate) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaCandidateRow(
    candidate: MediaCandidate,
    onDownload: () -> Unit
) {
    val icon = when (candidate.type) {
        MediaType.VIDEO -> Icons.Filled.Videocam
        MediaType.AUDIO -> Icons.Filled.MusicNote
        MediaType.HLS   -> Icons.Filled.LiveTv
        MediaType.IMAGE -> Icons.Filled.Image
        MediaType.OTHER -> Icons.Filled.InsertDriveFile
        else -> Icons.Filled.HelpOutline
    }
    val typeLabel = when (candidate.type) {
        MediaType.VIDEO -> "동영상"
        MediaType.AUDIO -> "오디오"
        MediaType.HLS   -> "HLS 스트림"
        MediaType.IMAGE -> "이미지"
        MediaType.OTHER -> "파일"
        else -> "알수없음"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = typeLabel,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text     = candidate.fileName,
                style    = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text  = typeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        FilledTonalIconButton(onClick = onDownload) {
            Icon(Icons.Filled.Download, contentDescription = "다운로드")
        }
    }
}
