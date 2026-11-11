package com.rescate.app.ui.impact

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.PathInterpolator
import androidx.core.content.ContextCompat
import com.rescate.app.R
import com.rescate.app.data.model.ImpactPeriod
import java.util.Locale





class BarChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var data: List<ImpactPeriod> = emptyList()
    private var progress = 0f
    private val d = resources.displayMetrics.density

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ContextCompat.getColor(context, R.color.green_500) }
    private val maxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ContextCompat.getColor(context, R.color.green_800) }
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ContextCompat.getColor(context, R.color.mint_50) }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.ink_muted)
        textSize = 11 * resources.displayMetrics.scaledDensity
        textAlign = Paint.Align.CENTER
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.ink)
        textSize = 11 * resources.displayMetrics.scaledDensity
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val rect = RectF()
    private var animator: ValueAnimator? = null

    fun setData(values: List<ImpactPeriod>) {
        data = values
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 900
            interpolator = PathInterpolator(0.2f, 0f, 0f, 1f)
            addUpdateListener { progress = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (data.isEmpty()) return
        val labelSpace = 22 * d
        val valueSpace = 18 * d
        val chartH = height - labelSpace - valueSpace
        val slot = width / data.size.toFloat()
        val barW = (slot * 0.5f).coerceAtMost(36 * d)
        val max = (data.maxOf { it.kg }).coerceAtLeast(0.1)
        val maxIndex = data.indices.maxByOrNull { data[it].kg } ?: -1
        val radius = 8 * d

        data.forEachIndexed { i, item ->
            val cx = slot * i + slot / 2
             
            val local = ((progress * (1 + data.size * 0.12f)) - i * 0.12f).coerceIn(0f, 1f)
            val h = (item.kg / max * chartH).toFloat() * local
            rect.set(cx - barW / 2, valueSpace, cx + barW / 2, valueSpace + chartH)
            canvas.drawRoundRect(rect, radius, radius, trackPaint)
            rect.set(cx - barW / 2, valueSpace + chartH - h, cx + barW / 2, valueSpace + chartH)
            if (h > 0) canvas.drawRoundRect(rect, radius, radius, if (i == maxIndex) maxPaint else barPaint)
            if (local > 0.9f) {
                valuePaint.alpha = ((local - 0.9f) * 10 * 255).toInt()
                canvas.drawText(String.format(Locale.US, "%.1f", item.kg), cx, valueSpace + chartH - h - 6 * d, valuePaint)
            }
            canvas.drawText(item.label, cx, height - 6 * d, labelPaint)
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }
}
