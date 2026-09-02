package com.example.data.util

object YouTubeHelper {
    /**
     * Extrai o ID de um vídeo do YouTube a partir de qualquer formato de link:
     * - https://www.youtube.com/watch?v=dQw4w9WgXcQ
     * - https://youtu.be/dQw4w9WgXcQ
     * - https://www.youtube.com/shorts/dQw4w9WgXcQ
     * - https://www.youtube.com/embed/dQw4w9WgXcQ
     */
    fun extractVideoId(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val trimmed = url.trim()

        val patterns = listOf(
            Regex("(?:youtube\\.com/watch\\?v=|youtube\\.com/watch\\?.*&v=)([a-zA-Z0-9_-]{11})"),
            Regex("youtu\\.be/([a-zA-Z0-9_-]{11})"),
            Regex("youtube\\.com/embed/([a-zA-Z0-9_-]{11})"),
            Regex("youtube\\.com/shorts/([a-zA-Z0-9_-]{11})"),
            Regex("youtube\\.com/v/([a-zA-Z0-9_-]{11})")
        )

        for (pattern in patterns) {
            val match = pattern.find(trimmed)
            if (match != null && match.groupValues.size > 1) {
                return match.groupValues[1]
            }
        }
        return null
    }

    fun isYouTubeUrl(url: String?): Boolean {
        return extractVideoId(url) != null
    }

    fun buildEmbedHtml(videoId: String): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; background: #000; overflow: hidden; }
                    html, body { width: 100%; height: 100%; background: #000; }
                    .video-wrapper { position: relative; width: 100vw; height: 100vh; display: flex; align-items: center; justify-content: center; background: #000; }
                    iframe { width: 100vw; height: 100vh; border: 0; object-fit: cover; }
                </style>
            </head>
            <body>
                <div class="video-wrapper">
                    <iframe 
                        id="ytplayer"
                        src="https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&mute=0&controls=0&modestbranding=1&rel=0&loop=1&playlist=$videoId&playsinline=1&enablejsapi=1&fs=0&iv_load_policy=3" 
                        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" 
                        allowfullscreen>
                    </iframe>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
}
