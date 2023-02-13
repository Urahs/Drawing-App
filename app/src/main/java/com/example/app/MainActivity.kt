package com.example.app

import android.app.Dialog
import android.media.Image
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.get

//https://www.geeksforgeeks.org/how-to-create-a-color-picker-tool-in-android-using-color-wheel-and-slider/
class MainActivity : AppCompatActivity() {

    private var drawingView: DrawingView? = null
    private var imageButtonCurrentPaint: ImageButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawingView = findViewById(R.id.drawingView)
        drawingView?.setSizeForBrush(10f)

        val linearLayoutPaintColors = findViewById<LinearLayout>(R.id.paint_colors)
        imageButtonCurrentPaint = linearLayoutPaintColors[0] as ImageButton
        imageButtonCurrentPaint!!.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.palette_selected))

        findViewById<ImageButton>(R.id.brush_button).setOnClickListener{
            showBrushSizeChooserDialog()
        }
    }

    private fun showBrushSizeChooserDialog(){
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush size: ")
        brushDialog.findViewById<ImageButton>(R.id.small_brush).setOnClickListener{
            drawingView!!.setSizeForBrush(5f)
            brushDialog.dismiss()
        }
        brushDialog.findViewById<ImageButton>(R.id.medium_brush).setOnClickListener{
            drawingView!!.setSizeForBrush(10f)
            brushDialog.dismiss()
        }
        brushDialog.findViewById<ImageButton>(R.id.large_brush).setOnClickListener{
            drawingView!!.setSizeForBrush(20f)
            brushDialog.dismiss()
        }

        brushDialog.show()
    }

    fun paintClicked(view: View){
        if(view !== imageButtonCurrentPaint){
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            drawingView?.setColor(colorTag)
            imageButton!!.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.palette_selected))
            imageButtonCurrentPaint!!.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.palette_normal))
            imageButtonCurrentPaint = view
        }
    }
}