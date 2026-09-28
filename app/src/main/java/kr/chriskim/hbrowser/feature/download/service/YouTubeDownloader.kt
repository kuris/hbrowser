package kr.chriskim.hbrowser.feature.download.service

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.URL

/**
 * YouTube/TVWiki51 다운로드용 yt-dlp 통합
 *
 * yt-dlp가 설치되어 있으면 YouTube/TVWiki51 URL에서 직접 다운로드 URL을 추출
 * 설치되어 없으면 최초 실행 시 자동 다운로드
 */
class YouTubeDownloader(private val context: Context) {

    companion object {
        private const val YT_DLP_COMMAND = "yt-dlp"
        private const val GITHUB_RELEASE_URL = "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp"
        private const val APP_YT_DLP_DIR = "yt-dlp"

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

    private val ytDlpFile: File by lazy {
        val appDir = context.getDir(APP_YT_DLP_DIR, Context.MODE_PRIVATE)
        val file = File(appDir, "yt-dlp")
        if (!file.exists()) {
            downloadYtDlp()
        }
        file
    }

    private val pythonCommand: String? by lazy {
        val commands = listOf("python3", "python")
        for (cmd in commands) {
            if (findInPath(cmd) != null) return@lazy cmd
        }
        null
    }

    /** yt-dlp가 설치되어 있는지 확인 */
    fun isAvailable(): Boolean {
        // PATH에서 yt-dlp 확인
        if (findInPath(YT_DLP_COMMAND) != null) return true

        // 앱 내부에 yt-dlp가 있고 python이 있으면 사용
        if (ytDlpFile.exists() && pythonCommand != null) return true

        return false
    }

    /** yt-dlp 다운로드 URL 추출 */
    suspend fun getDownloadUrl(url: String): Result<DownloadInfo> = withContext(Dispatchers.IO) {
        try {
            if (!isAvailable()) {
                return@withContext Result.failure(
                    Exception("yt-dlp가 설치되어 있지 않습니다. 최초 실행 시 자동 다운로드됩니다.")
                )
            }

            val ytDlpPath = resolveYtDlpPath()
            val process = ProcessBuilder(
                ytDlpPath,
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

    /** yt-dlp 직접 다운로드 URL만 추출 */
    suspend fun getDirectUrl(url: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!isAvailable()) {
                return@withContext Result.failure(
                    Exception("yt-dlp가 설치되어 있지 않습니다.")
                )
            }

            val ytDlpPath = resolveYtDlpPath()
            val process = ProcessBuilder(
                ytDlpPath,
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

    private fun resolveYtDlpPath(): String {
        // PATH에서 yt-dlp 확인
        val pathYtDlp = findInPath(YT_DLP_COMMAND)
        if (pathYtDlp != null) return pathYtDlp

        // 앱 내부 yt-dlp + python 사용
        if (ytDlpFile.exists() && pythonCommand != null) {
            return "$pythonCommand ${ytDlpFile.absolutePath}"
        }

        throw Exception("yt-dlp를 찾을 수 없습니다.")
    }

    private fun findInPath(command: String): String? {
        val pathEnv = System.getenv("PATH") ?: return null
        val pathDirs = pathEnv.split(File.pathSeparator)
        for (dir in pathDirs) {
            val file = File(dir, command)
            if (file.exists() && file.canExecute()) return file.absolutePath
        }
        return null
    }

    private fun downloadYtDlp() {
        val appDir = context.getDir(APP_YT_DLP_DIR, Context.MODE_PRIVATE)
        val ytDlpFile = File(appDir, "yt-dlp")

        try {
            val url = URL(GITHUB_RELEASE_URL)
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 30000
            connection.readTimeout = 60000

            connection.inputStream.use { input ->
                ytDlpFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            connection.disconnect()

            ytDlpFile.setExecutable(true)
        } catch (e: Exception) {
            throw Exception("yt-dlp 다운로드 실패: ${e.message}")
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
