package kr.chriskim.hbrowser.core.model

import android.app.DownloadManager

enum class DownloadStatus {
    QUEUED, RUNNING, PAUSED, SUCCESSFUL, FAILED, CANCELED, COMPLETED;

    companion object {
        fun fromDmStatus(s: Int) = when (s) {
            DownloadManager.STATUS_PENDING   -> QUEUED
            DownloadManager.STATUS_RUNNING   -> RUNNING
            DownloadManager.STATUS_PAUSED    -> PAUSED
            DownloadManager.STATUS_SUCCESSFUL -> SUCCESSFUL
            DownloadManager.STATUS_FAILED    -> FAILED
            else -> FAILED
        }
    }
}
