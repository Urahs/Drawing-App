package com.example.app

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View

class DrawingView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private var drawPath : CustomPath? = null
    private var canvasBitmap: Bitmap? = null
    private var drawPaint: Paint = Paint()
    private var canvasPaint: Paint? = null
    private var brushSize: Float = 20f
    private var color = Color.BLACK
    private var canvas: Canvas? = null
    private val paths = ArrayList<CustomPath>()

    init {
        setUpDrawingVariables()
    }

    private fun setUpDrawingVariables() {
        drawPath = CustomPath(color, brushSize)
        drawPaint.color = color
        drawPaint.style = Paint.Style.STROKE
        drawPaint.strokeJoin = Paint.Join.ROUND
        drawPaint.strokeCap = Paint.Cap.ROUND
        canvasPaint = Paint(Paint.DITHER_FLAG)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(canvasBitmap!!)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas!!.drawBitmap(canvasBitmap!!, 0f, 0f, canvasPaint)

        paths.forEach{
            drawPaint.strokeWidth = it.brushTickness
            drawPaint.color = it.color
            canvas.drawPath(it, drawPaint)
        }

        if(!drawPath!!.isEmpty){
            drawPaint.strokeWidth = drawPath!!.brushTickness
            drawPaint.color = drawPath!!.color
            canvas.drawPath(drawPath!!, drawPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {

        val touchX = event?.x
        val touchY = event?.y

        when(event?.action){
            MotionEvent.ACTION_DOWN -> {
                drawPath!!.color = color
                drawPath!!.brushTickness = brushSize

                drawPath!!.reset()
                drawPath!!.moveTo(touchX!!, touchY!!)
            }
            MotionEvent.ACTION_MOVE -> {
                drawPath!!.lineTo(touchX!!, touchY!!)
            }
            MotionEvent.ACTION_UP -> {
                paths.add(drawPath!!)
                drawPath = CustomPath(color, brushSize)
            }
            else -> {
                return false
            }
        }
        invalidate()

        return true
    }

    fun setSizeForBrush(newSize: Float){
        brushSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, newSize, resources.displayMetrics)
        drawPaint!!.strokeWidth = brushSize
    }


    internal inner class CustomPath(
        var color: Int,
        var brushTickness: Float
    ) : Path() {}
}