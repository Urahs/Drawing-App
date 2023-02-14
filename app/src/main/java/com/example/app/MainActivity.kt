package com.example.app

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.Image
import android.media.MediaScannerConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

//https://www.geeksforgeeks.org/how-to-create-a-color-picker-tool-in-android-using-color-wheel-and-slider/
class MainActivity : AppCompatActivity() {

    private var drawingView: DrawingView? = null
    private var imageButtonCurrentPaint: ImageButton? = null
    private var customProgressDialog: Dialog? = null

    val openGalleryLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
        if(result.resultCode == RESULT_OK && result.data != null){
            findViewById<ImageView>(R.id.backgroundIV).setImageURI(result.data!!.data)
        }
    }

    val requestPermission: ActivityResultLauncher<Array<String>> = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){ permissions ->
        permissions.forEach{ permission ->
            val permissionName = permission.key
            val isGranted = permission.value

            if (isGranted){
                Toast.makeText(this, "Permission is granted", Toast.LENGTH_SHORT).show()

                val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                openGalleryLauncher.launch(pickIntent)
            }
            else
                Toast.makeText(this, "Permission is denied", Toast.LENGTH_SHORT).show()
        }
    }

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

        findViewById<ImageButton>(R.id.gallery_button).setOnClickListener{
            requestStoragePermission()
        }

        findViewById<ImageButton>(R.id.undo_button).setOnClickListener{
            drawingView!!.onClickUndo()
        }

        findViewById<ImageButton>(R.id.save_button).setOnClickListener{
            if(isReadStorageAllowed()){
                showProgressDialog()
                lifecycleScope.launch {
                    val flDravingView: FrameLayout = findViewById(R.id.drawing_view_container)
                    saveBitmapFile(getBitmapFromView(flDravingView))
                }
            }
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

    private fun isReadStorageAllowed(): Boolean {
        val result = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)

        return  result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        if (shouldShowRequestPermissionRationale(android.Manifest.permission.READ_EXTERNAL_STORAGE)){
            showRationaleDialog("Drawing App", "This app needs to access your external storage")
        }
        else {
            requestPermission.launch(arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ))
        }
    }

    private fun getBitmapFromView(view: View): Bitmap {
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if(bgDrawable != null)
            bgDrawable.draw(canvas)
        else
            canvas.drawColor(Color.WHITE)

        view.draw(canvas)
        return returnedBitmap
    }

    private fun showRationaleDialog(title: String, message: String){
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title).setMessage(message)
        builder.create().show()
    }

    private suspend fun saveBitmapFile(bitmap: Bitmap?): String{
        var result = ""
        withContext(Dispatchers.IO){
            if(bitmap != null){
                try {
                    val bytes = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)

                    val f = File(externalCacheDir?.absoluteFile.toString() + File.separator + "DrawingApp" + System.currentTimeMillis() / 1000 + ".png")
                    Log.d("Test", externalCacheDir?.absoluteFile.toString() + File.separator + "DrawingApp" + System.currentTimeMillis() / 1000 + ".png")
                    val fo = FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()

                    result = f.absolutePath

                    runOnUiThread {
                        cancelProgressDialog()

                        if(result.isNotEmpty()){
                            Toast.makeText(applicationContext, "File saved successfully: $result", Toast.LENGTH_SHORT).show()
                            shareImage(result)
                        }
                        else{
                            Toast.makeText(applicationContext, "File cant saved successfully!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                catch (e: Exception){
                    e.printStackTrace()
                }
            }
        }
        return result
    }

    private fun showProgressDialog(){
        customProgressDialog = Dialog(this)
        customProgressDialog?.setContentView(R.layout.dailog_custom_progress)
        customProgressDialog?.show()
    }

    private fun cancelProgressDialog(){
        if(customProgressDialog != null){
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }

    private fun shareImage(result: String){
        MediaScannerConnection.scanFile(this, arrayOf(result), null){ path, uri ->
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.type = "image/png"
            startActivity(Intent.createChooser(shareIntent, "Share"))
        }
    }
}











