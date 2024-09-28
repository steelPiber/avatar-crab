package com.example.avatar_crab.presentation.measure

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class CircularOverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val overlayPaint = Paint().apply {
        color = Color.BLACK
        alpha = 160  // 오버레이 투명도 조절 (0~255)
    }

    private val clearPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 전체 화면에 반투명한 검은색 오버레이 그리기
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)

        // 중앙에 원형으로 구멍 뚫기
        val radius = Math.min(width, height) / 2f
        canvas.drawCircle(width / 2f, height / 2f, radius, clearPaint)
    }
}
