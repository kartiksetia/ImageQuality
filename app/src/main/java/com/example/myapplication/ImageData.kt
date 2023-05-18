package com.example.myapplication

import android.graphics.Bitmap
import java.io.File

class ImageData(imageId: File, title: String) {
    var mImageId = imageId
    var mImageTitle: String? = title
    var finalImageBitmap: Bitmap? = null

}