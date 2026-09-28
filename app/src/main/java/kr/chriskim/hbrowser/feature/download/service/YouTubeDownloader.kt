package kr.chriskim.hbrowser.feature.download.service

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * YouTube/TVWiki51 다운로드용 yt-dlp 통합
 *
 * yt-dlp가 설치되어 있으면 YouTube/TVWiki51 URL에서 직접 다운로드 URL을 추출
 * 설치되어 없으면 에러 반환 (사용자에게 yt-dlp 설치를 안내)
 */
class YouTubeDownloader(private val context: Context) {

    companion object {
        private const val YT_DLP_COMMAND = "yt-dlp"

        private val YOUTUBE_URL_PATTERN = Regex(
            "https?://(www\\.)?(youtube\\.com|youtu\\.be)/.*"
        )
        private val TVWIKI51_URL_PATTERN = Regex(
            "https?://(www\\.)?tvwiki51\\.net/.*"
        )

        fun isYouTubeUrl(url: String): Boolean {
            return YOUTUBE_URL_PATTERN.matches(url)
        }

        fun isTvWiki51Url(url: String): Boolean {
            return TVWIKI51_URL_PATTERN.matches(url)
        }

        fun isSupportedUrl(url: String): Boolean {
            return isYouTubeUrl(url) || isTvWiki51Url(url)
        }
    }

    /** yt-dlp가 설치되어 있는지 확인 */
    fun isAvailable(): Boolean {
        return try {
            val process = ProcessBuilder("which", YT_DLP_COMMAND)
                .redirectErrorStream(true)
                .start()
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    /** YouTube/TVWiki51 URL에서 다운로드 URL 추출 */
    suspend fun getDownloadUrl(url: String): Result<DownloadInfo> = withContext(Dispatchers.IO) {
        try {
            if (!isAvailable()) {
                return@withContext Result.failure(
                    Exception("yt-dlp가 설치되어 있지 않습니다. yt-dlp를 설치한 후 다시 시도하세요.")
                )
            }

            // yt-dlp로 JSON 정보 추출
            val process = ProcessBuilder(
                YT_DLP_COMMAND,
                "--dump-json",
                "--no-playlist",
                "--no-warnings",
                url
            ).redirectErrorStream(true).start()

            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            if (exitCode != 0) {
                return@withContext Result.failure(
                    Exception("yt-dlp 실패: $output")
                )
            }

            val json = JSONObject(output)
            val info = DownloadInfo(
                title = json.optString("title", "Video"),
                url = json.optString("url", ""),
                formats = extractFormats(json),
                thumbnail = json.optString("thumbnail", ""),
                duration = json.optInt("duration", 0),
                ext = json.optString("ext", "mp4")
            )

            Result.success(info)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** YouTube/TVWiki51 URL에서 직접 다운로드 URL만 추출 */
    suspend fun getDirectUrl(url: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!isAvailable()) {
                return@withContext Result.failure(
                    Exception("yt-dlp가 설치되어 있지 않습니다.")
                )
            }

            val process = ProcessBuilder(
                YT_DLP_COMMAND,
                "--get-url",
                "--no-playlist",
                "--no-warnings",
                url
            ).redirectErrorStream(true).start()

            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            if (exitCode != 0) {
                return@withContext Result.failure(
                    Exception("yt-dlp 실패: $output")
                )
            }

            Result.success(output.trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractFormats(json: JSONObject): List<FormatInfo> {
        val formats = mutableListOf<FormatInfo>()
        val formatsArray = json.optJSONArray("formats") ?: return formats

        for (i in 0 until formatsArray.length()) {
            val format = formatsArray.getJSONObject(i)
            formats.add(
                FormatInfo(
                    quality = format.optString("quality", "unknown"),
                    ext = format.optString("ext", "mp4"),
                    mimeType = format.optString("mime_type", ""),
                    url = format.optString("url", ""),
                    height = format.optInt("height", 0),
                    width = format.optInt("width", 0),
                    fileSize = format.optLong("filesize", 0)
                )
            )
        }

        // 품질순 정렬 (높은 품질 우선)
        return formats.sortedByDescending { it.quality }
    }

    data class DownloadInfo(
        val title: String,
        val url: String,
        val formats: List<FormatInfo>,
        val thumbnail: String,
        val duration: Int,
        val ext: String
    )

    data class FormatInfo(
        val quality: String,
        val ext: String,
        val mimeType: String,
        val url: String,
        val height: Int,
        val width: Int,
        val fileSize: Long
    )
}
