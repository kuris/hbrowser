package kr.chriskim.hbrowser.feature.download.manager

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kr.chriskim.hbrowser.HBrowserApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Android DownloadManager 완료 이벤트 수신기 */
class DownloadCompleteReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
        val dmId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        if (dmId == -1L) return
        val pr = goAsync()
        scope.launch {
            try { (ctx.applicationContext as HBrowserApp).container.downloadRepo.updateStatusByDmId(dmId) }
            finally { pr.finish() }
        }
    }
}
