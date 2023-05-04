package com.example.myapplication

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException


suspend fun readPltFile(context: Context, fileName: String): List<LatLng> = withContext(Dispatchers.IO) {
    val latLngList = mutableListOf<LatLng>()

    context.assets.open(fileName).bufferedReader().useLines { lines ->
        lines.forEach { line ->
            val values = line.split(",")
            if (values.size >= 2) {
                val latitude = values[0].toDoubleOrNull()
                val longitude = values[1].toDoubleOrNull()
                if (latitude != null && longitude != null) {
                    latLngList.add(LatLng(latitude, longitude))
                }
            }
        }
    }

    return@withContext latLngList
}

suspend fun listPltFiles(context: Context): List<String> = withContext(Dispatchers.IO) {
    val pltFiles = mutableListOf<String>()
    try {
        val files = context.assets.list("")
        if (files != null) {
            for (file in files) {
                if (file.endsWith(".plt")) {
                    pltFiles.add(file)
                }
            }
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return@withContext pltFiles
}


fun BitmapFromVector(context:Context, vectorResId:Int): BitmapDescriptor? {
    //drawable generator
    var vectorDrawable: Drawable
    vectorDrawable= ContextCompat.getDrawable(context,vectorResId)!!
    vectorDrawable.setBounds(0,0,vectorDrawable.intrinsicWidth,vectorDrawable.intrinsicHeight)
    //bitmap genarator
    var bitmap: Bitmap
    bitmap= Bitmap.createBitmap(vectorDrawable.intrinsicWidth,vectorDrawable.intrinsicHeight,
        Bitmap.Config.ARGB_8888)
    //canvas genaret
    var canvas: Canvas
    //pass bitmap in canvas constructor
    canvas= Canvas(bitmap)
    //pass canvas in drawable
    vectorDrawable.draw(canvas)
    //return BitmapDescriptorFactory
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}