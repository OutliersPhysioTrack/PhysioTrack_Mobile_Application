package org.example.physiotrack.video

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.widget.MediaController
import android.widget.VideoView
import java.io.File
import java.io.FileOutputStream

class VideoPlayerActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val assetPath = intent.getStringExtra(EXTRA_ASSET_PATH).orEmpty()
        if (assetPath.isBlank()) {
            finish()
            return
        }

        val videoFile = copyAssetToCache(assetPath)
        if (videoFile == null || !videoFile.exists()) {
            finish()
            return
        }

        val videoView = VideoView(this)
        val controller = MediaController(this)
        controller.setAnchorView(videoView)
        videoView.setMediaController(controller)
        videoView.setVideoURI(Uri.fromFile(videoFile))
        setContentView(videoView)
        videoView.setOnPreparedListener { it.isLooping = true; videoView.start() }
    }

    private fun copyAssetToCache(assetPath: String): File? {
        return try {
            val outFile = File(cacheDir, "asset_${assetPath.replace('/', '_')}")
            if (outFile.exists() && outFile.length() > 0) return outFile

            assets.open(assetPath).use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }
            outFile
        } catch (_: Throwable) {
            null
        }
    }

    companion object {
        const val EXTRA_ASSET_PATH = "asset_path"
    }
}
