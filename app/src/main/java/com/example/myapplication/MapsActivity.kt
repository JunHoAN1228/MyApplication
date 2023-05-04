package com.example.myapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.myapplication.databinding.ActivityMapsBinding
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        lifecycleScope.launch {
            val datapoints = processData(convertDataPoint())

            val latLngList = datapoints.map { LatLng(it.latitude, it.longitude) }


            if (latLngList.isNotEmpty()) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom((latLngList[0]),15f))
            }

            latLngList.forEach { latLng ->
                val marker = MarkerOptions().position(latLng)
                marker.icon(BitmapFromVector(getApplicationContext(), R.drawable.baseline_lens_24))
                mMap.addMarker(marker)
            }
        }
    }

    private fun convertDataPoint(): MutableList<DataPoint> {
        val dataPoints = mutableListOf<DataPoint>()

        val assetManager = assets
        val fileList = assetManager.list("")?.filter { it.endsWith(".plt") } ?: emptyList()

        fileList.forEachIndexed { index, fileName ->
            val inputStream = assetManager.open(fileName)
            val reader = BufferedReader(InputStreamReader(inputStream))

            reader.useLines { lines ->
                lines.forEach { line ->
                    if (!line.startsWith("latitude")) { // 헤더 라인 무시
                        val values = line.split(",")
                        val latitude = values[0].toDouble()
                        val longitude = values[1].toDouble()
                        val altitude = values[2].toDouble()
                        val day = index + 1 // 파일마다 day 값이 1씩 증가

                        dataPoints.add(DataPoint(latitude, longitude, altitude, day))
                    }
                }
            }
        }

        // dataPoints 리스트에 모든 데이터 포인트가 저장되어 있습니다.
        return dataPoints
    }
}