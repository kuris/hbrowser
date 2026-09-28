package kr.chriskim.hbrowser.feature.download.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Environment
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import kr.chriskim.hbrowser.HBrowserApp
import kr.chriskim.hbrowser.core.model.DownloadStatus
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * 자체 ForegroundService 다운로더
 *
 * - HTTP Range 요청으로 이어받기(resume) 지원
 * - 진행률 실시간 DB 업데이트
 * - Wi-Fi 전용 옵션 지원
 * - HLS .m3u8은 별도 HlsDownloadTask로 처리
 *
 * 보안:
 * - DRM / 보호 콘텐츠 우회 없음
 * - 쿠키는 로컬 요청 재현용으로만 사용
 * - 외부 전송 없음
 */
class DownloadForegroundService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = mutableMapOf<Long, Job>()  // id → Job

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        DownloadNotificationHelper.createChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START   -> handleStart(intent)
            ACTION_CANCEL  -> handleCancel(intent)
            ACTION_PAUSE   -> handlePause(intent)
            ACTION_RESUME  -> handleResume(intent)
        }
        return START_NOT_STICKY
    }

    // ── 시작 ──────────────────────────────────────────────────

    private fun handleStart(intent: Intent) {
        val id        = intent.getLongExtra(EXTRA_ID, -1L).takeIf { it != -1L } ?: return
        val url       = intent.getStringExtra(EXTRA_URL) ?: return
        val fileName  = intent.getStringExtra(EXTRA_FILENAME) ?: "download"
        val userAgent = intent.getStringExtra(EXTRA_USER_AGENT)
        val referer   = intent.getStringExtra(EXTRA_REFERER)
        val cookies   = intent.getStringExtra(EXTRA_COOKIES)
        val wifiOnly  = intent.getBooleanExtra(EXTRA_WIFI_ONLY, false)
        val isHls     = intent.getBooleanExtra(EXTRA_IS_HLS, false)

        // ForegroundService 시작 알림
        startForeground(
            DownloadNotificationHelper.NOTIF_ID_BASE + id.toInt(),
            DownloadNotificationHelper.buildProgress(this, fileName, -1, id)
        )

        val job = scope.launch {
            val dao = (application as HBrowserApp).container.db.downloadDao()
            val nm  = NotificationManagerCompat.from(this@DownloadForegroundService)
            val notifId = DownloadNotificationHelper.NOTIF_ID_BASE + id.toInt()

            try {
                // Wi-Fi 전용 체크
                if (wifiOnly && !isWifiConnected()) {
                    dao.updateProgress(id, DownloadStatus.PAUSED, "Wi-Fi 연결 필요",
                        null, null, 0, null, System.currentTimeMillis())
                    return@launch
                }

                if (isHls) {
                    HlsDownloadTask(this@DownloadForegroundService, id, url, fileName,
                        userAgent, cookies, dao, nm, notifId, wifiOnly).execute()
                } else {
                    HttpDownloadTask(this@DownloadForegroundService, id, url, fileName,
                        userAgent, referer, cookies, dao, nm, notifId).execute()
                }
            } catch (e: CancellationException) {
                dao.updateProgress(id, DownloadStatus.CANCELED, null, null, null, 0, null, System.currentTimeMillis())
            } catch (e: Exception) {
                dao.updateProgress(id, DownloadStatus.FAILED, e.message, null, null, 0, null, System.currentTimeMillis())
                nm.notify(notifId, DownloadNotificationHelper.buildFailed(this@DownloadForegroundService, fileName, e.message))
            } finally {
                activeJobs.remove(id)
                if (activeJobs.isEmpty()) stopSelf()
            }
        }
        activeJobs[id] = job
    }

    private fun handleCancel(intent: Intent) {
        val id = intent.getLongExtra(EXTRA_ID, -1L)
        activeJobs[id]?.cancel()
    }

    private fun handlePause(intent: Intent) {
        val id = intent.getLongExtra(EXTRA_ID, -1L)
        activeJobs[id]?.cancel()  // 현재는 취소로 처리, resume 시 이어받기
    }

    private fun handleResume(intent: Intent) {
        handleStart(intent)  // 같은 ID로 재시작 → resume
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    // ── Wi-Fi 체크 ────────────────────────────────────────────

    private fun isWifiConnected(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val nw = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(nw) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    // ── Companion ─────────────────────────────────────────────

    companion object {
        const val ACTION_START   = "kr.chriskim.hbrowser.DOWNLOAD_START"
        const val ACTION_CANCEL  = "kr.chriskim.hbrowser.DOWNLOAD_CANCEL"
        const val ACTION_PAUSE   = "kr.chriskim.hbrowser.DOWNLOAD_PAUSE"
        const val ACTION_RESUME  = "kr.chriskim.hbrowser.DOWNLOAD_RESUME"

        const val EXTRA_ID         = "extra_id"
        const val EXTRA_URL        = "extra_url"
        const val EXTRA_FILENAME   = "extra_filename"
        const val EXTRA_USER_AGENT = "extra_ua"
        const val EXTRA_REFERER    = "extra_referer"
        const val EXTRA_COOKIES    = "extra_cookies"
        const val EXTRA_WIFI_ONLY  = "extra_wifi_only"
        const val EXTRA_IS_HLS     = "extra_is_hls"

        fun buildStartIntent(ctx: Context, id: Long, url: String, fileName: String,
                             userAgent: String?, referer: String?, cookies: String?,
                             wifiOnly: Boolean, isHls: Boolean): Intent =
            Intent(ctx, DownloadForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_ID, id)
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_FILENAME, fileName)
                putExtra(EXTRA_USER_AGENT, userAgent)
                putExtra(EXTRA_REFERER, referer)
                putExtra(EXTRA_COOKIES, cookies)
                putExtra(EXTRA_WIFI_ONLY, wifiOnly)
                putExtra(EXTRA_IS_HLS, isHls)
            }
    }
}
