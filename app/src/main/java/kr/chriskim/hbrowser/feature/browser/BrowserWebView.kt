package kr.chriskim.hbrowser.feature.browser

import android.annotation.SuppressLint
import android.content.Context
import android.os.Message
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import kr.chriskim.hbrowser.feature.adblock.engine.AdBlockEngine
import kr.chriskim.hbrowser.feature.media.MediaDetector
import kr.chriskim.hbrowser.feature.media.VideoTagExtractor

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserWebView(
    vm: BrowserViewModel,
    adBlockEngine: AdBlockEngine,
    modifier: Modifier = Modifier,
    onWebViewCreated: (WebView) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    // WebView 인스턴스 생성 (recomposition 때 재생성 방지)
    val webView = remember {
        createWebView(context, vm, adBlockEngine).also { onWebViewCreated(it) }
    }

    // URL 관찰 → loadUrl 호출 (외부 navigate 요청 처리)
    val navRequest by vm.navigationRequest.collectAsState()
    LaunchedEffect(navRequest) {
        val url = navRequest ?: return@LaunchedEffect
        if (url != webView.url) webView.loadUrl(url)
        vm.clearNavigationRequest()
    }

    DisposableEffect(Unit) {
        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }

    AndroidView(
        factory  = { webView },
        modifier = modifier,
        update   = { /* WebView 업데이트는 LaunchedEffect로 처리 */ }
    )
}

@SuppressLint("SetJavaScriptEnabled")
private fun createWebView(
    context: Context,
    vm: BrowserViewModel,
    adBlockEngine: AdBlockEngine
): WebView {
    val detector  = MediaDetector(vm.mediaCandidateStore)
    val extractor = VideoTagExtractor(vm.mediaCandidateStore)

    return WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        // ── WebSettings ───────────────────────────────────────────────────
        settings.apply {
            javaScriptEnabled          = true
            domStorageEnabled          = true
            databaseEnabled            = true
            allowFileAccess            = false
            allowContentAccess         = true
            setSupportMultipleWindows(true)
            javaScriptCanOpenWindowsAutomatically = false
            mediaPlaybackRequiresUserGesture      = false
            cacheMode                  = WebSettings.LOAD_DEFAULT
            mixedContentMode           = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            useWideViewPort            = true
            loadWithOverviewMode       = true
            setSupportZoom(true)
            builtInZoomControls        = true
            displayZoomControls        = false
            userAgentString            = userAgentString.replace("wv", "")
        }

        // ── 쿠키 ─────────────────────────────────────────────────────────
        val cookieMgr = CookieManager.getInstance()
        cookieMgr.setAcceptCookie(true)
        cookieMgr.setAcceptThirdPartyCookies(this, true)

        // ── WebViewClient (광고차단 + 미디어 감지) ────────────────────────
        webViewClient = CleanWebViewClient(vm, adBlockEngine, detector, extractor)

        // ── WebChromeClient (진행률 + 팝업 차단) ──────────────────────────
        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                vm.onProgressChanged(newProgress)
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                title?.let { vm.onTitleReceived(it) }
            }

            // 팝업 차단: createWindow 요청은 null 반환으로 거부
            override fun onCreateWindow(
                view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?
            ): Boolean {
                // 사용자 제스처 없는 팝업은 완전 차단
                if (!isUserGesture) return false
                // 사용자 제스처가 있는 경우 URL만 추출해 현재 WebView에서 로드
                val newWebView = WebView(view!!.context)
                newWebView.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(v: WebView?, request: WebResourceRequest?): Boolean {
                        request?.url?.toString()?.let { view.loadUrl(it) }
                        return true
                    }
                }
                val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
                transport.webView = newWebView
                resultMsg.sendToTarget()
                return true
            }

            override fun onCloseWindow(window: WebView?) = Unit
        }

        // ── DownloadListener ─────────────────────────────────────────────
        setDownloadListener { url, ua, contentDisposition, mimetype, contentLength ->
            vm.onDownloadRequested(
                url   = url,
                ua    = ua,
                disp  = contentDisposition,
                mime  = mimetype,
                len   = contentLength,
                pageUrl = this.url ?: url
            )
        }

        // 초기 페이지 로드
        loadUrl(kr.chriskim.hbrowser.core.util.UrlUtils.HOME_URL)
    }
}
