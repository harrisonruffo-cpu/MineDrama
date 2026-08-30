package com.example.data.util

import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import java.util.regex.Pattern

object VideoUrlResolver {

    private const val TAG = "VideoUrlResolver"

    val FALLBACK_STREAMS = listOf(
        "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
        "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
        "https://storage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
        "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
        "https://storage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
        "https://storage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
        "https://storage.googleapis.com/gtv-videos-bucket/sample/WhatCarCanYouGetForAGrand.mp4"
    )

    /**
     * Cleans, sanitizes, and converts common cloud storage/sharing links
     * (Google Drive, Dropbox, OneDrive, Pixeldrain, etc.) into direct video streaming URLs.
     */
    fun resolveDirectVideoUrl(inputUrl: String?): String {
        if (inputUrl.isNullOrBlank()) {
            return FALLBACK_STREAMS[0]
        }

        var url = inputUrl.trim()
            .replace("\"", "")
            .replace("'", "")
            .replace("\n", "")
            .replace("\r", "")

        // Handle protocol
        if (url.startsWith("//")) {
            url = "https:$url"
        } else if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("file://") && !url.startsWith("content://") && !url.startsWith("/")) {
            url = "https://$url"
        }

        // Local file or content uri
        if (url.startsWith("/") || url.startsWith("file://") || url.startsWith("content://")) {
            return url
        }

        try {
            // 1. Google Drive Link Conversion
            // Examples:
            // https://drive.google.com/file/d/1A2B3C4D5E/view?usp=sharing
            // https://drive.google.com/open?id=1A2B3C4D5E
            // https://drive.google.com/uc?id=1A2B3C4D5E
            // https://docs.google.com/file/d/1A2B3C4D5E/edit
            if (url.contains("drive.google.com") || url.contains("docs.google.com")) {
                val fileId = extractGoogleDriveFileId(url)
                if (!fileId.isNullOrBlank()) {
                    Log.d(TAG, "Converted Google Drive link with File ID: $fileId")
                    return "https://drive.google.com/uc?export=download&id=$fileId"
                }
            }

            // 2. Dropbox Link Conversion
            // Example: https://www.dropbox.com/s/xyz123/video.mp4?dl=0
            // Example: https://www.dropbox.com/scl/fi/xyz123/video.mp4?rlkey=abc&dl=0
            if (url.contains("dropbox.com")) {
                var directDropbox = url
                if (directDropbox.contains("dl=0")) {
                    directDropbox = directDropbox.replace("dl=0", "raw=1")
                } else if (!directDropbox.contains("raw=1") && !directDropbox.contains("dl=1")) {
                    directDropbox = if (directDropbox.contains("?")) "$directDropbox&raw=1" else "$directDropbox?raw=1"
                }
                directDropbox = directDropbox.replace("www.dropbox.com", "dl.dropboxusercontent.com")
                Log.d(TAG, "Converted Dropbox link to: $directDropbox")
                return directDropbox
            }

            // 3. OneDrive Link Conversion
            if (url.contains("1drv.ms") || url.contains("onedrive.live.com")) {
                if (url.contains("redir?")) {
                    url = url.replace("redir?", "download?")
                }
                return url
            }

            // 4. Pixeldrain Link Conversion
            // Example: https://pixeldrain.com/u/XYZ -> https://pixeldrain.com/api/file/XYZ
            if (url.contains("pixeldrain.com/u/")) {
                val id = url.substringAfter("pixeldrain.com/u/").substringBefore("?").substringBefore("/")
                if (id.isNotBlank()) {
                    return "https://pixeldrain.com/api/file/$id"
                }
            }

            // 5. Internet Archive Link Conversion
            // Example: https://archive.org/details/ITEM -> https://archive.org/download/ITEM/ITEM.mp4
            if (url.contains("archive.org/details/")) {
                val id = url.substringAfter("archive.org/details/").substringBefore("?").substringBefore("/")
                if (id.isNotBlank()) {
                    return "https://archive.org/download/$id/$id.mp4"
                }
            }

            // 6. Google Cloud Storage / Firebase Storage fix
            if (url.contains("commondatastorage.googleapis.com")) {
                url = url.replace("commondatastorage.googleapis.com", "storage.googleapis.com")
            }

        } catch (e: Exception) {
            Log.w(TAG, "Error resolving direct video url for $url", e)
        }

        return url
    }

    private fun extractGoogleDriveFileId(url: String): String? {
        // Pattern 1: /file/d/FILE_ID
        val pattern1 = Pattern.compile("/file/d/([a-zA-Z0-9_-]+)")
        val matcher1 = pattern1.matcher(url)
        if (matcher1.find()) {
            return matcher1.group(1)
        }

        // Pattern 2: id=FILE_ID
        val pattern2 = Pattern.compile("[?&]id=([a-zA-Z0-9_-]+)")
        val matcher2 = pattern2.matcher(url)
        if (matcher2.find()) {
            return matcher2.group(1)
        }

        // Pattern 3: /d/FILE_ID
        val pattern3 = Pattern.compile("/d/([a-zA-Z0-9_-]+)")
        val matcher3 = pattern3.matcher(url)
        if (matcher3.find()) {
            return matcher3.group(1)
        }

        return null
    }

    /**
     * Builds a MediaItem with proper MIME type for ExoPlayer playback
     */
    @OptIn(UnstableApi::class)
    fun buildMediaItem(resolvedUrl: String): MediaItem {
        val uri = if (resolvedUrl.startsWith("/") || resolvedUrl.startsWith("file://")) {
            if (resolvedUrl.startsWith("/")) Uri.fromFile(java.io.File(resolvedUrl)) else Uri.parse(resolvedUrl)
        } else {
            Uri.parse(resolvedUrl)
        }

        val builder = MediaItem.Builder().setUri(uri)
        val lower = resolvedUrl.lowercase()

        when {
            lower.contains(".m3u8") -> builder.setMimeType(MimeTypes.APPLICATION_M3U8)
            lower.contains(".mpd") -> builder.setMimeType(MimeTypes.APPLICATION_MPD)
            lower.contains(".mp4") || lower.contains(".m4v") -> builder.setMimeType(MimeTypes.VIDEO_MP4)
            lower.contains(".webm") -> builder.setMimeType(MimeTypes.VIDEO_WEBM)
            lower.contains(".mkv") -> builder.setMimeType(MimeTypes.VIDEO_MATROSKA)
        }

        return builder.build()
    }

    /**
     * Returns a fallback video URL given an episode index
     */
    fun getFallbackUrl(episodeNumber: Int = 1): String {
        val index = (episodeNumber - 1).coerceAtLeast(0) % FALLBACK_STREAMS.size
        return FALLBACK_STREAMS[index]
    }
}
