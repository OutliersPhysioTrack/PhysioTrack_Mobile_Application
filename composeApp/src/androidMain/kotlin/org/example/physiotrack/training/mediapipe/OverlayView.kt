package org.example.physiotrack.training.mediapipe

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import org.example.physiotrack.R
import kotlin.math.max
import kotlin.math.min

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var results: PoseLandmarkerResult? = null

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.YELLOW
        strokeWidth = LANDMARK_STROKE_WIDTH
        style = Paint.Style.FILL
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(requireNotNull(context), R.color.mp_color_primary)
        strokeWidth = LANDMARK_STROKE_WIDTH
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 42f
        style = Paint.Style.FILL
    }

    private val textBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x88000000.toInt()
        style = Paint.Style.FILL
    }

    private var scaleFactor: Float = 1f
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1
    private var xOffset: Float = 0f
    private var yOffset: Float = 0f
    private var isMirrored: Boolean = false

    // angle label (normalized coords)
    private var angleText: String? = null
    private var angleNx: Float = 0f
    private var angleNy: Float = 0f

    fun clear() {
        results = null
        angleText = null
        invalidate()
    }

    fun setAngleLabel(nx: Float, ny: Float, text: String?) {
        angleNx = nx
        angleNy = ny
        angleText = text
        invalidate()
    }

    private fun mapX(nx: Float): Float {
        val scaledW = imageWidth * scaleFactor
        val xInImage = if (isMirrored) (1f - nx) * scaledW else nx * scaledW
        return xOffset + xInImage
    }

    private fun mapY(ny: Float): Float {
        val scaledH = imageHeight * scaleFactor
        return yOffset + (ny * scaledH)
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        val poseLandmarkerResult = results ?: return
        val pose = poseLandmarkerResult.landmarks().firstOrNull() ?: return

        for (lm in pose) {
            canvas.drawPoint(mapX(lm.x()), mapY(lm.y()), pointPaint)
        }

        PoseLandmarker.POSE_LANDMARKS.forEach { c ->
            val conn = c ?: return@forEach
            val s = pose[conn.start()]
            val e = pose[conn.end()]
            canvas.drawLine(
                mapX(s.x()), mapY(s.y()),
                mapX(e.x()), mapY(e.y()),
                linePaint
            )
        }

        // draw angle at elbow
        val text = angleText ?: return
        val x = mapX(angleNx)
        val y = mapY(angleNy)

        val pad = 10f
        val w = textPaint.measureText(text)
        val h = textPaint.textSize

        canvas.drawRoundRect(
            x - pad,
            y - h - pad,
            x + w + pad,
            y + pad,
            14f,
            14f,
            textBgPaint
        )
        canvas.drawText(text, x, y, textPaint)
    }

    fun setResults(
        poseLandmarkerResults: PoseLandmarkerResult,
        imageHeight: Int,
        imageWidth: Int,
        runningMode: RunningMode = RunningMode.IMAGE,
        isMirrored: Boolean = false
    ) {
        results = poseLandmarkerResults
        this.imageHeight = imageHeight
        this.imageWidth = imageWidth
        this.isMirrored = isMirrored

        if (width == 0 || height == 0) {
            invalidate()
            return
        }

        val scaleX = width * 1f / imageWidth
        val scaleY = height * 1f / imageHeight

        scaleFactor = when (runningMode) {
            RunningMode.LIVE_STREAM -> max(scaleX, scaleY) // FILL_CENTER / center-crop
            RunningMode.IMAGE, RunningMode.VIDEO -> min(scaleX, scaleY)
        }

        val scaledW = imageWidth * scaleFactor
        val scaledH = imageHeight * scaleFactor
        xOffset = (width - scaledW) / 2f
        yOffset = (height - scaledH) / 2f

        invalidate()
    }

    companion object {
        private const val LANDMARK_STROKE_WIDTH = 12F
    }
}
