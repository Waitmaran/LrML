package com.colin.lrml

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.toRectF
import com.google.mlkit.vision.face.Face

class RectOverlay constructor(context: Context?, attributeSet: AttributeSet?) :
    View(context, attributeSet) {

    private var rectBounds: MutableList<RectF> = mutableListOf()
    private lateinit var paint: Paint

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Pass it a list of RectF (rectBounds)
        paint = Paint()
        paint.color = Color.RED
        paint.strokeWidth = 10f
        paint.style = Paint.Style.STROKE
        rectBounds.forEach { canvas.drawRect(it.left, it.top, it.right, it.bottom, paint) }
    }

    fun setFacesBounds(fixedBounds: List<RectF>) {
        rectBounds.clear()
        rectBounds = fixedBounds.toMutableList()
        invalidate()
    }
}