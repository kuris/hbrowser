package kr.chriskim.hbrowser.core.database

import androidx.room.TypeConverter
import kr.chriskim.hbrowser.core.model.DownloadEngine
import kr.chriskim.hbrowser.core.model.DownloadStatus

class Converters {
    @TypeConverter fun statusToStr(v: DownloadStatus): String = v.name
    @TypeConverter fun strToStatus(v: String): DownloadStatus = DownloadStatus.valueOf(v)
    @TypeConverter fun engineToStr(v: DownloadEngine): String = v.name
    @TypeConverter fun strToEngine(v: String): DownloadEngine = DownloadEngine.valueOf(v)
}
