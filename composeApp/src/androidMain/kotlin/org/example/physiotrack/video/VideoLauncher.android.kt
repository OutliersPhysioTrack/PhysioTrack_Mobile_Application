package org.example.physiotrack.video

import android.content.Intent
import org.example.physiotrack.AndroidContextHolder

/**
 * Android-only: launches a lightweight [VideoPlayerActivity] to play an mp4 from assets/.
 */
actual fun playAssetVideo(assetPath: String) {
    val ctx = AndroidContextHolder.appContext ?: return
    val intent = Intent(ctx, VideoPlayerActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra(VideoPlayerActivity.EXTRA_ASSET_PATH, assetPath)
    }
    ctx.startActivity(intent)
}
