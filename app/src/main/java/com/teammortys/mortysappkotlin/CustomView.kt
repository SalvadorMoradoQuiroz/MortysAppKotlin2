package com.teammortys.mortysappkotlin

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.widget.ImageView


/**
 * Created by BunyamiN on 25-5-2015.
 */
class CustomView(context: Context?, attrs: AttributeSet?) :
    androidx.appcompat.widget.AppCompatImageView(context!!, attrs) {
    private val circleColor = Color.WHITE
    private val tag = "CustomView"
    var w = 0
    var h = 0

    private var x = 75
    private var y = 75
    private val indent = 10
    private val circleradius = 30
    public override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        if (widthMode == MeasureSpec.EXACTLY) {
            w = widthSize
        } else if (widthMode == MeasureSpec.AT_MOST) {
            w = 150
        }
        if (heightMode == MeasureSpec.EXACTLY) {
            h = heightSize
        } else if (heightMode == MeasureSpec.AT_MOST) {
            h = 150
        }
        setMeasuredDimension(width, height)
    }

    public override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val paint = Paint()
        paint.color = circleColor
        paint.style = Paint.Style.FILL
        canvas.drawCircle(x.toFloat(), y.toFloat(), circleradius.toFloat(), paint)
    }

    fun right() {
        x += indent
        invalidate()
    }

    fun left() {
        x -= indent
        invalidate()
    }

    fun down() {
        y += indent
        invalidate()
    }

    fun up() {
        y -= indent
        invalidate()
    }
}