package kr.chriskim.hbrowser.feature.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.ds by preferencesDataStore(name = "hbrowser_settings")

class SettingsRepository(private val ctx: Context) {
    companion object {
        val AD_BLOCK          = booleanPreferencesKey("ad_block")
        val BLOCK_POPUPS      = booleanPreferencesKey("block_popups")
        val CONFIRM_DOWNLOAD  = booleanPreferencesKey("confirm_download")
        val WIFI_ONLY         = booleanPreferencesKey("wifi_only")
        val USE_FOREGROUND_DL = booleanPreferencesKey("use_fg_downloader")
    }

    val isAdBlock:         Flow<Boolean> = ctx.ds.data.map { it[AD_BLOCK]          ?: true }
    val isPopupBlocked:    Flow<Boolean> = ctx.ds.data.map { it[BLOCK_POPUPS]      ?: true }
    val isConfirmDownload: Flow<Boolean> = ctx.ds.data.map { it[CONFIRM_DOWNLOAD]  ?: true }
    val isWifiOnly:        Flow<Boolean> = ctx.ds.data.map { it[WIFI_ONLY]         ?: false }
    val useFgDownloader:   Flow<Boolean> = ctx.ds.data.map { it[USE_FOREGROUND_DL] ?: true }

    suspend fun setAdBlock(v: Boolean)         { ctx.ds.edit { it[AD_BLOCK]          = v } }
    suspend fun setPopupBlocked(v: Boolean)    { ctx.ds.edit { it[BLOCK_POPUPS]      = v } }
    suspend fun setConfirmDownload(v: Boolean) { ctx.ds.edit { it[CONFIRM_DOWNLOAD]  = v } }
    suspend fun setWifiOnly(v: Boolean)        { ctx.ds.edit { it[WIFI_ONLY]         = v } }
    suspend fun setUseFgDownloader(v: Boolean) { ctx.ds.edit { it[USE_FOREGROUND_DL] = v } }
}
