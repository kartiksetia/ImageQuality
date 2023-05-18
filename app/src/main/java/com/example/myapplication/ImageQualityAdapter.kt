package com.example.myapplication
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.ExifInterface
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.ml.TfliteModel
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Arrays


class ImageQualityAdapter(private val mList: List<ImageData>, private val context : Context) : RecyclerView.Adapter<ImageQualityAdapter.ViewHolder>() {

    // create new views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // inflates the card_view_design view
        // that is used to hold list item
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_view_design, parent, false)

        return ViewHolder(view)
    }

    // binds the list items to a view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val ItemsViewModel = mList[position]

        val newImageData = getDrawableAfterProcessing(ItemsViewModel)

        holder.imageView.setImageBitmap(newImageData.finalImageBitmap)

        holder.textView.text = newImageData.mImageTitle

    }

    private fun getDrawableAfterProcessing(newData : ImageData): ImageData {

        val bitmap : Bitmap = BitmapFactory.decodeFile(newData.mImageId.getAbsolutePath())

        var orientation : Int = getCorrectlyOrientedImage(newData.mImageId.absolutePath)
        val bmRotated = rotateBitmap(bitmap, orientation)

        val scaledBitmap = scaleBitmap(bmRotated!!,256,512)
        //pickedImageView.setImageBitmap(scaledBitmap)
        val result : String = performInference(preProcessImage(scaledBitmap!!))
        newData.finalImageBitmap = bmRotated
        newData.mImageTitle = result

        return newData
    }

    // return the number of the items in the list
    override fun getItemCount(): Int {
        return mList.size
    }

    // Holds the views for adding it to image and text
    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageview)
        val textView: TextView = itemView.findViewById(R.id.textView)
    }



    private fun performInference(input: ByteBuffer): String{
        Log.d("image_quality","<<<<<<, performInference")

        val model = TfliteModel.newInstance(context)
        val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 512, 256, 3),  DataType.FLOAT32)
        inputFeature0.loadBuffer(input)
        val outputs = model.process(inputFeature0)
        val outputFeature0 = outputs.outputFeature0AsTensorBuffer
        return Arrays.toString(outputFeature0.floatArray)
    }



    fun scaleBitmap(bitmap: Bitmap, wantedWidth: Int, wantedHeight: Int): Bitmap? {
        val output = Bitmap.createBitmap(wantedWidth, wantedHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val m = Matrix()
        m.setScale(wantedWidth.toFloat() / bitmap.width, wantedHeight.toFloat() / bitmap.height)
        canvas.drawBitmap(bitmap, m, Paint())
        return output
    }

    private fun preProcessImage(bitmap: Bitmap): ByteBuffer {
        Log.d("image_quality","<<<<<<, preProcessImage")
        val imgData = ByteBuffer.allocateDirect(4 * 256 * 512 * 3)
        imgData.order(ByteOrder.nativeOrder())
        val means  = listOf<Float>(118.03860628335003F, 112.65675931587339F, 108.60966603551644F)// []
        val stds  = listOf<Float>(70.75601041091608F, 71.51498874856256F, 73.11152587776891F)
        val intValues = IntArray(256 * 512)


        imgData.rewind()
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        // Convert the image to floating point.
        var pixel = 0
        for (i in 0 until 512) {
            for (j in 0 until 256) {
                val `val` = intValues[pixel++]
                imgData.putFloat(((`val`.shr(16) and 0xFF) - means[0])/stds[0])
                imgData.putFloat(((`val`.shr(8) and 0xFF)- means[0])/ stds[0])
                imgData.putFloat(((`val` and 0xFF) - means[0])/stds[0])
            }
        }
        return imgData
    }

    fun getCorrectlyOrientedImage(path: String): Int {
        var exif: ExifInterface? = null

        try {
             exif = ExifInterface(path)


        } catch (e: IOException) {
            e.printStackTrace()
        }
        val orientation = exif!!.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        )
        return orientation
    }

    private fun getRealPathFromURI(contentURI: Uri, activity: Context): String? {
        val cursor = activity.contentResolver
            .query(contentURI, null, null, null, null)
        return if (cursor == null) { // Source is Dropbox or other similar local file
            // path
            contentURI.path
        } else {
            cursor.moveToFirst()
            val idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
            cursor.getString(idx)
        }
    }


    fun drawableToBitmap(drawable: Drawable): Bitmap? {
        var bitmap: Bitmap? = null
        if (drawable is BitmapDrawable) {
            val bitmapDrawable = drawable
            if (bitmapDrawable.bitmap != null) {
                return bitmapDrawable.bitmap
            }
        }
        bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
            Bitmap.createBitmap(
                1,
                1,
                Bitmap.Config.ARGB_8888
            ) // Single color bitmap will be created of 1x1 pixel
        } else {
            Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
        }
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap? {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_NORMAL -> return bitmap
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setRotate(180f)
                matrix.postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            else -> return bitmap
        }
        return try {
            val bmRotated =
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            bitmap.recycle()
            bmRotated
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
            null
        }
    }
}
