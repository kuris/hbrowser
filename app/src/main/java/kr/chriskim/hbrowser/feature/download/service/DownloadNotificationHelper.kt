package kr.chriskim.hbrowser.feature.download.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import kr.chriskim.hbrowser.MainActivity

/**
 * 다운로드 ForegroundService 알림 헬퍼
 */
object DownloadNotificationHelper {

    const val CHANNEL_ID = "hbrowser_download"
    const val NOTIF_ID_BASE = 10000

    fun createChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID, "HBrowser 다운로드", NotificationManager.IMPORTANCE_LOW
            ).apply { description = "파일 다운로드 진행 상황" }
            (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(ch)
        }
    }

    fun buildProgress(ctx: Context, fileName: String, progress: Int, id: Long): android.app.Notification {
        val intent = Intent(ctx, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP }
        val pi = PendingIntent.getActivity(ctx, id.toInt(), intent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(fileName)
            .setContentText(if (progress < 0) "다운로드 중…" else "$progress%")
            .setProgress(100, progress.coerceAtLeast(0), progress < 0)
            .setOngoing(true)
            .setContentIntent(pi)
            .build()
    }

    fun buildComplete(ctx: Context, fileName: String, id: Long): android.app.Notification {
        val intent = Intent(ctx, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP }
        val pi = PendingIntent.getActivity(ctx, id.toInt(), intent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(fileName)
            .setContentText("다운로드 완료")
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
    }

    fun buildFailed(ctx: Context, fileName: String, reason: String?): android.app.Notification {
        return NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(fileName)
            .setContentText("다운로드 실패: ${reason ?: "알 수 없는 오류"}")
            .setAutoCancel(true)
            .build()
    }
}
