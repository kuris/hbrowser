package kr.chriskim.hbrowser.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: SettingsViewModel,
    onBack: () -> Unit
) {
    val isAdBlock        by vm.isAdBlock.collectAsState()
    val isPopupBlocked   by vm.isPopupBlocked.collectAsState()
    val isConfirmDownload by vm.isConfirmDownload.collectAsState()
    val isWifiOnly       by vm.isWifiOnly.collectAsState()
    val useFgDownloader  by vm.useFgDownloader.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("설정") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // ── 광고차단 섹션 ─────────────────────────────────────────
            SettingsSectionHeader("광고차단")

            SettingsSwitchRow(
                icon        = Icons.Filled.Shield,
                title       = "광고차단 사용",
                description = "EasyList 기반 광고 및 추적기 차단",
                checked     = isAdBlock,
                onToggle    = vm::setAdBlock
            )

            SettingsSwitchRow(
                icon        = Icons.Filled.Block,
                title       = "팝업 차단",
                description = "사용자 동작 없이 열리는 팝업 창 차단",
                checked     = isPopupBlocked,
                onToggle    = vm::setPopupBlocked
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            // ── 다운로드 섹션 ─────────────────────────────────────────
            SettingsSectionHeader("다운로드")

            SettingsSwitchRow(
                icon        = Icons.Filled.Quiz,
                title       = "다운로드 전 확인",
                description = "다운로드 시작 전 파일 정보 확인 창 표시",
                checked     = isConfirmDownload,
                onToggle    = vm::setConfirmDownload
            )

            SettingsSwitchRow(
                icon        = Icons.Filled.Wifi,
                title       = "Wi-Fi 전용 다운로드",
                description = "모바일 데이터 사용 시 다운로드 대기",
                checked     = isWifiOnly,
                onToggle    = vm::setWifiOnly
            )

            SettingsSwitchRow(
                icon        = Icons.Filled.CloudDownload,
                title       = "포그라운드 서비스 다운로드",
                description = "앱 종료 후에도 다운로드 지속, HTTP Range 이어받기 지원",
                checked     = useFgDownloader,
                onToggle    = vm::setUseFgDownloader
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            // ── 앱 정보 섹션 ──────────────────────────────────────────
            SettingsSectionHeader("앱 정보")

            ListItem(
                leadingContent = { Icon(Icons.Filled.Info, contentDescription = null) },
                headlineContent = { Text("HBrowser") },
                supportingContent = { Text("kr.chriskim.hbrowser • 버전 1.0.0") }
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text     = title,
        style    = MaterialTheme.typography.labelLarge,
        color    = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    ListItem(
        leadingContent = {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        headlineContent = { Text(title) },
        supportingContent = {
            Text(description, style = MaterialTheme.typography.bodySmall)
        },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onToggle)
        },
        modifier = Modifier.padding(horizontal = 0.dp)
    )
}
