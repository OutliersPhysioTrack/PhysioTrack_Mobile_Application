package org.example.physiotrack.media

import android.content.Context
import android.graphics.Canvas
import android.graphics.Movie
import android.os.SystemClock
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

@Composable
actual fun AssetGif(
    assetPath: String,
    modifier: Modifier,
    contentDescription: String?,
) {
    val context = LocalContext.current
    val view = remember { GifMovieView(context) }

    LaunchedEffect(assetPath) { view.setGifFromAssets(assetPath) }

    DisposableEffect(Unit) { onDispose { view.stop() } }

    AndroidView(
        modifier = modifier,
        factory = { view },
        update = { /* no-op */ },
    )
}

private class GifMovieView(context: Context) : View(context) {
    private var movie: Movie? = null
    private var startMs: Long = 0L
    private var running = true

    fun setGifFromAssets(path: String) {
        movie = try {
            context.assets.open(path).use { input ->
                val bytes = input.readBytes()
                Movie.decodeByteArray(bytes, 0, bytes.size)
            }
        } catch (_: Throwable) {
            null
        }
        startMs = 0L
        invalidate()
    }

    fun stop() { running = false }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!running) return
        val m = movie ?: return

        val now = SystemClock.uptimeMillis()
        if (startMs == 0L) startMs = now

        val duration = (if (m.duration() > 0) m.duration() else 1000)
        val relTime = ((now - startMs) % duration).toInt()
        m.setTime(relTime)

        val mw = m.width().toFloat().coerceAtLeast(1f)
        val mh = m.height().toFloat().coerceAtLeast(1f)
        val s = minOf(width / mw, height / mh)

        val dx = (width - mw * s) / 2f
        val dy = (height - mh * s) / 2f

        canvas.save()
        canvas.translate(dx, dy)
        canvas.scale(s, s)
        m.draw(canvas, 0f, 0f)
        canvas.restore()

        invalidate()
    }
}
