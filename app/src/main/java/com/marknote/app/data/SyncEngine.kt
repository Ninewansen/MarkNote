package com.marknote.app.data

import android.content.Context
import android.util.Log
import com.marknote.app.R
import java.io.File
import java.io.IOException

/** 一次双向同步的结果 */
data class SyncResult(
    val uploaded: Int,
    val downloaded: Int,
    val skipped: Int,
    val errors: List<String>,
    val finishedAt: Long,
) {
    val hasErrors: Boolean get() = errors.isNotEmpty()
}

/**
 * 双向同步（安全模式）：
 * - 本地有而远端没有/不同 → 上传；
 * - 远端有而本地没有/不同 → 下载；
 * - 不自动删除任何一边的文件，避免误删。
 */
object SyncEngine {

    suspend fun sync(
        context: Context,
        config: WebDavConfig,
        localRoot: File,
        onProgress: (String) -> Unit = {},
    ): SyncResult {
        val client = WebDavClient(config.username, config.password)
        val base = WebDavClient.collectionUrl(config)
        val errors = mutableListOf<String>()

        onProgress(context.getString(R.string.sync_connecting))
        try {
            client.ensureCollection(base)
        } catch (e: Exception) {
            Log.e("MarkNoteSync", "ensureCollection failed: ${e.message}", e)
            throw IOException(
                context.getString(R.string.sync_mkdir_failed, e.message ?: "?"),
                e,
            )
        }

        val remoteByName = try {
            client.list(base)
                .filter { !it.isCollection }
                .filter { it.name.substringAfterLast('.', "").lowercase() in SUPPORTED_TEXT_EXTENSIONS }
                .filter { it.name.isSafeRemoteName() }
                .associateBy { it.name }
        } catch (e: Exception) {
            throw IOException(e.message ?: "PROPFIND 失败", e)
        }

        val localFiles = localRoot.listFiles { f -> f.isFile && f.extension.lowercase() in SUPPORTED_TEXT_EXTENSIONS }
            ?: emptyArray()

        var uploaded = 0
        var downloaded = 0
        var skipped = 0

        // 本地 → 远端：缺失或大小不一致则上传。
        // 用大小做变更检测，避免两端设备时钟不一致导致反复上传。
        for (file in localFiles) {
            val remote = remoteByName[file.name]
            val needUpload = remote == null || remote.size != file.length()
            if (!needUpload) {
                skipped++
                continue
            }
            try {
                onProgress(context.getString(R.string.sync_uploading, file.name))
                client.put(WebDavClient.fileUrl(base, file.name), file)
                uploaded++
            } catch (e: Exception) {
                errors += context.getString(R.string.sync_upload_failed, file.name, e.message ?: "?")
            }
        }

        // 远端 → 本地：缺失或大小不一致则下载
        for ((name, remote) in remoteByName) {
            val local = File(localRoot, name)
            val needDownload = !local.exists() || local.length() != remote.size
            if (!needDownload) {
                skipped++
                continue
            }
            try {
                onProgress(context.getString(R.string.sync_downloading, name))
                client.get(WebDavClient.fileUrl(base, name), local)
                downloaded++
            } catch (e: Exception) {
                errors += context.getString(R.string.sync_download_failed, name, e.message ?: "?")
            }
        }

        return SyncResult(
            uploaded = uploaded,
            downloaded = downloaded,
            skipped = skipped,
            errors = errors,
            finishedAt = System.currentTimeMillis(),
        )
    }

    private fun String.isSafeRemoteName(): Boolean {
        if (isBlank()) return false
        if (contains('/') || contains('\\') || contains("\u0000")) return false
        if (this == "." || this == "..") return false
        return true
    }
}
