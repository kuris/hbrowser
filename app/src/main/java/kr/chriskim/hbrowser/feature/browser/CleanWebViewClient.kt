package kr.chriskim.hbrowser.feature.browser

import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import kr.chriskim.hbrowser.feature.adblock.engine.AdBlockEngine
import kr.chriskim.hbrowser.feature.media.MediaDetector
import kr.chriskim.hbrowser.feature.media.VideoTagExtractor

class CleanWebViewClient(
    private val vm: BrowserViewModel,
    private val adBlockEngine: AdBlockEngine,
    private val detector: MediaDetector,
    private val extractor: VideoTagExtractor
) : WebViewClient() {

    override fun shouldInterceptRequest(view: WebView?, req: WebResourceRequest?): WebResourceResponse? {
        val url     = req?.url?.toString() ?: return null
        val pageUrl = vm.currentUrl.value

        // 광고차단
        if (vm.isAdBlock.value && adBlockEngine.shouldBlock(url, pageUrl)) {
            vm.onAdBlocked()
            return emptyResponse()
        }
        // 미디어 감지
        detector.checkAndAdd(url, pageUrl)
        return null
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        url?.let { vm.onPageStarted(it) }
        view?.let { vm.updateNav(it) }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        val u = url ?: vm.currentUrl.value
        vm.onPageFinished(u, view?.title)
        view?.let { vm.updateNav(it); extractor.extract(it, u) }
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false

    private fun emptyResponse() = WebResourceResponse("text/plain","utf-8",204,"No Content",
        mapOf("Access-Control-Allow-Origin" to "*"),"".byteInputStream())
}
