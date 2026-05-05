package com.example.legoclassifierapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

fun createImageUri(context: Context): Uri {
    val imageFile = File(context.cacheDir, "lego_image_${System.currentTimeMillis()}.jpg")
    val authority = "${context.packageName}.fileprovider"
    return FileProvider.getUriForFile(context, authority, imageFile)
}

fun uriToBitmap(uri: Uri, context: Context): Bitmap {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        ImageDecoder.decodeBitmap(
            ImageDecoder.createSource(context.contentResolver, uri)
        ) { decoder, _, _ -> decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE }
    } else {
        @Suppress("DEPRECATION")
        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
    }
}

fun classifyImage(
    imgUri: Uri,
    width: Int = 128,
    height: Int = 128,
    interpreter: Interpreter?,
    context: Context,
    onResult: (label: String, displayResult: String) -> Unit = { _, _ -> }
) {
    if (interpreter == null) return

    val labels = try {
        context.assets.open("custom_labels.txt").bufferedReader().useLines { it.toList() }
    } catch (e: IOException) {
        emptyList<String>()
    }

    if (labels.isEmpty()) {
        Log.e("CLASSIFIER", "Labels file not found or empty")
        return
    }

    var bitmap = uriToBitmap(imgUri, context)
    bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
    
    val input = ByteBuffer.allocateDirect(width * height * 3 * 4).order(ByteOrder.nativeOrder())
    for (y in 0 until height) {
        for (x in 0 until width) {
            val px = bitmap.getPixel(x, y)
            input.putFloat(((Color.red(px)) - 127.5f) / 127.5f)
            input.putFloat(((Color.green(px)) - 127.5f) / 127.5f)
            input.putFloat(((Color.blue(px)) - 127.5f) / 127.5f)
        }
    }

    val modelOutput = ByteBuffer.allocateDirect(labels.size * 4).order(ByteOrder.nativeOrder())
    interpreter.run(input, modelOutput)

    modelOutput.rewind()
    val probabilities = modelOutput.asFloatBuffer()
    
    var maxIdx = 0
    var maxProb = -1f
    
    for (i in labels.indices) {
        val probability = probabilities.get(i)
        if (probability > maxProb) {
            maxProb = probability
            maxIdx = i
        }
    }
    
    val label = labels[maxIdx]
    val displayResult = "$label (${String.format("%.1f", maxProb * 100)}%)"
    Log.d("CLASSIFIER", "Result: $displayResult")
    onResult(label, displayResult)
}

fun getLabelIndex(label: String, context: Context): Int? {
    val labels = context.assets.open("custom_labels.txt")
        .bufferedReader()
        .useLines { it.toList() }
    val index = labels.indexOfFirst { it.trim() == label.trim() }
    return if (index == -1) null else index + 1 // +1 if your DB IDs are 1-based
}