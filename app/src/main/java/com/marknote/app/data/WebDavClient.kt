package com.marknote.app.data

import java.io.File
import java.io.IOException
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

/** WebDAV 连接配置 */
data class WebDavConfig(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val remotePath: String = "MarkNote",
    val autoSync: Boolean = false,
)

/** 远端文件条目 */
data class RemoteFile(
    val name: String,
    val size: Long,
    val lastModified: Long,
    val isCollection: Boolean,
)

/**
 * 基于 OkHttp 的轻量 WebDAV 客户端（参考 sardine-android 的 API 思路）：
 * PROPFIND 列目录、MKCOL 建目录、PUT/GET 上传下载，Basic 认证。
 */
class WebDavClient(
    private val username: String,
    private val password: String,
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val authHeader = Credentials.basic(username, password)

    /** 确保远端集合存在（不存在则创建；已存在/被重定向视为成功） */
    suspend fun ensureCollection(url: HttpUrl) = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .method("MKCOL", null)
            .header("Authorization", authHeader)
            .build()
        client.newCall(request).execute().use { response ->
            when (response.code) {
                201, 405, 301, 302 -> Unit
                else -> throw IOException("HTTP ${response.code}")
            }
        }
    }

    /** 列出集合下的文件（Depth: 1） */
    suspend fun list(url: HttpUrl): List<RemoteFile> = withContext(Dispatchers.IO) {
        val body = PROPFIND_BODY.toRequestBody("application/xml".toMediaType())
        val request = Request.Builder()
            .url(url)
            .method("PROPFIND", body)
            .header("Authorization", authHeader)
            .header("Depth", "1")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val xml = response.body?.string() ?: throw IOException("空响应")
            parseMultistatus(xml, url.toString())
        }
    }

    /** 上传本地文件到远端 */
    suspend fun put(url: HttpUrl, file: File) = withContext(Dispatchers.IO) {
        val body = file.asRequestBody("text/plain; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(url)
            .put(body)
            .header("Authorization", authHeader)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
        }
    }

    /** 下载远端文件到本地（先写临时文件，成功后再替换） */
    suspend fun get(url: HttpUrl, dest: File) = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Authorization", authHeader)
            .build()
        val temp = File(dest.parentFile, "${dest.name}.part")
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val source = response.body ?: throw IOException("空响应")
            source.byteStream().use { input ->
                temp.outputStream().use { output -> input.copyTo(output) }
            }
        }
        if (!temp.renameTo(dest)) {
            dest.delete()
            if (!temp.renameTo(dest)) throw IOException("写入本地文件失败")
        }
    }

    private fun parseMultistatus(xml: String, baseUrl: String): List<RemoteFile> {
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(xml.reader())

        val result = mutableListOf<RemoteFile>()
        var href: String? = null
        var size = 0L
        var modified = 0L
        var collection = false

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    // 注意：substringAfter 的第二个参数是“找不到分隔符时返回的值”，
                    // 传 "" 会把无前缀标签（href/response 等）变成空串导致匹配失败，
                    // 这里在没有冒号时保留原名。
                    val name = parser.name.substringAfter(':', parser.name).lowercase()
                    when (name) {
                        "href" -> href = parser.nextText()
                        "getcontentlength" -> size = parser.nextText().toLongOrNull() ?: 0L
                        "getlastmodified" -> modified = parseHttpDate(parser.nextText()) ?: 0L
                        "collection" -> collection = true
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name.substringAfter(':', parser.name)
                            .equals("response", ignoreCase = true)
                    ) {
                        val current = href
                        if (current != null) {
                            val name = decodeName(current, baseUrl)
                            if (name != null) {
                                result += RemoteFile(name, size, modified, collection)
                            }
                        }
                        href = null
                        size = 0L
                        modified = 0L
                        collection = false
                    }
                }
            }
            parser.next()
        }
        return result
    }

    /** href → 文件名；集合自身（与 baseUrl 相同）返回 null */
    private fun decodeName(href: String, baseUrl: String): String? {
        val trimmed = href.trimEnd('/')
        if (trimmed == baseUrl.trimEnd('/')) return null
        val raw = trimmed.substringAfterLast('/')
        val name = try {
            URLDecoder.decode(raw, "UTF-8")
        } catch (_: Exception) {
            raw
        }
        return name.takeIf { it.isNotBlank() }
    }

    companion object {
        private val PROPFIND_BODY = """
            <?xml version="1.0" encoding="utf-8"?>
            <d:propfind xmlns:d="DAV:">
              <d:prop>
                <d:displayname/>
                <d:getcontentlength/>
                <d:getlastmodified/>
                <d:resourcetype/>
              </d:prop>
            </d:propfind>
        """.trimIndent()

        private val httpDateFormats = listOf(
            SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US),
        )

        fun parseHttpDate(value: String): Long? {
            for (format in httpDateFormats) {
                try {
                    return format.parse(value)?.time
                } catch (_: Exception) {
                    // 尝试下一种格式
                }
            }
            return null
        }

        /** 根据配置拼接远端集合 URL */
        fun collectionUrl(config: WebDavConfig): HttpUrl {
            val server = config.serverUrl.trim().trimEnd('/')
            val path = config.remotePath.trim().trim('/')
            val raw = if (path.isEmpty()) "$server/" else "$server/$path/"
            return raw.toHttpUrl()
        }

        /** 集合下某个文件的 URL（文件名自动做 URL 编码） */
        fun fileUrl(base: HttpUrl, name: String): HttpUrl =
            base.newBuilder().addPathSegment(name).build()
    }
}
