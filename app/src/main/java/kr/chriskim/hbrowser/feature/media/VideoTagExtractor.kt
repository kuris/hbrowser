package kr.chriskim.hbrowser.feature.media

import android.webkit.WebView
import kr.chriskim.hbrowser.core.model.MediaCandidate
import kr.chriskim.hbrowser.core.model.MediaSource

/**
 * JavaScript evaluateJavascript로 현재 페이지 video 태그 URL 추출
 * 보안: video/source src URL만 수집, 개인정보/쿠키/폼 수집 없음
 */
class VideoTagExtractor(private val store: MediaCandidateStore) {
    fun extract(wv: WebView, pageUrl: String) {
        val js = """
(function(){
  var r=[];
  try{
    var vs=document.querySelectorAll('video');
    for(var i=0;i<vs.length;i++){
      var v=vs[i];
      if(v.src&&v.src.length>0)r.push(v.src);
      if(v.currentSrc&&v.currentSrc.length>0&&v.currentSrc!==v.src)r.push(v.currentSrc);
      var ss=v.querySelectorAll('source');
      for(var j=0;j<ss.length;j++){if(ss[j].src&&ss[j].src.length>0)r.push(ss[j].src);}
    }
  }catch(e){}
  return JSON.stringify(r);
})();""".trimIndent()

        wv.evaluateJavascript(js) { result ->
            if (result == null || result == "null" || result == "\"[]\"") return@evaluateJavascript
            parseJsonArray(result.removePrefix("\"").removeSuffix("\"")
                .replace("\\\"","\"").replace("\\\\","\\"))
                .filter { it.startsWith("http") || it.startsWith("/") }
                .forEach { url ->
                    store.addCandidate(MediaCandidate.fromUrl(url, pageUrl, MediaSource.VIDEO_TAG))
                }
        }
    }

    private fun parseJsonArray(json: String): List<String> = runCatching {
        val t = json.trim()
        if (!t.startsWith("[")) return emptyList()
        val inner = t.removePrefix("[").removeSuffix("]")
        if (inner.isBlank()) return emptyList()
        inner.split("\",\"").map { it.trim().trim('"') }.filter { it.isNotBlank() }
    }.getOrElse { emptyList() }
}
