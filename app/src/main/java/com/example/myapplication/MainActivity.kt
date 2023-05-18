package com.example.myapplication


import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream


class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "image_quality"
    }
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerview = findViewById<RecyclerView>(R.id.recyclerview)
        recyclerview.layoutManager = LinearLayoutManager(this)

        val data: ArrayList<ImageData> = ArrayList<ImageData>()
        val imagesNames : ArrayList<String> = ArrayList<String>()
        imagesNames.add("cards.jpg")
        imagesNames.add("lambo.png")
        imagesNames.add("modelsaidbad1.jpeg")
        imagesNames.add("modelsaidgood1.jpeg")
        imagesNames.add("modelsaidgood4.jpeg")
        imagesNames.add("modelsaidgood5.jpeg")

        for (imagesName in imagesNames){
            try {
                val ims = assets.open(imagesName)
                val d = createFileFromInputStream(ims, imagesName)
                data.add(ImageData(d!!, imagesName))
            } catch (ex: IOException) {
                return
            }
        }
        val adapter = ImageQualityAdapter(data, this)
        recyclerview.adapter = adapter
    }

    private fun createFileFromInputStream(inputStream: InputStream, fileName : String): File? {
        try {
            val f: File = File(getCacheDir(),fileName)
            val outputStream: OutputStream = FileOutputStream(f)
            val buffer = ByteArray(1024)
            var length = 0
            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }
            outputStream.close()
            inputStream.close()
            return f
        } catch (e: IOException) {
            //Logging exception
        }
        return null
    }

}