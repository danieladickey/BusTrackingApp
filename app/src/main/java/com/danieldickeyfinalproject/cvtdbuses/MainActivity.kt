package com.danieldickeyfinalproject.cvtdbuses

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.*
import android.util.Log
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.OnLocationCameraTransitionListener
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.markerview.MarkerView
import com.mapbox.mapboxsdk.plugins.markerview.MarkerViewManager

class MainActivity : AppCompatActivity() {
    private var mapView: MapView? = null
    lateinit var mAdView: AdView

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // create map etc
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))
        setContentView(R.layout.activity_main)
        mapView = findViewById(R.id.mapView)
        mapView?.onCreate(savedInstanceState)

        // keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Google ad mob initialization
        MobileAds.initialize(this) {}
        mAdView = findViewById(R.id.bannerAd)
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)

        // allows network stuff to be called in main (for parsing bus location data)
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().permitAll().build())

        // instantiate bus data
        Log.d("TAG", "onCreate: running CVTD bus app...")
        val cvtd = CVTD()
        var busList = cvtd.getBuses()
        var busCount = cvtd.count
        Log.d("TAG", "onCreate: busCount: $busCount")

        mapView?.getMapAsync(OnMapReadyCallback { mapBoxMap ->
            mapBoxMap.setStyle(
                Style.MAPBOX_STREETS
            ) { style ->
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
                }

                val markerViewManager = MarkerViewManager(mapView, mapBoxMap)
                var textViewMarkerList: MutableList<MarkerView> = mutableListOf()
                var imageViewMarkerList: MutableList<MarkerView> = mutableListOf()

                // initiate markers
                updateMarkers(
                    busCount,
                    busList,
                    markerViewManager,
                    textViewMarkerList,
                    imageViewMarkerList
                )

                // repeat code every x seconds
                val x: Long = 10
                val mainHandler = Handler(Looper.getMainLooper())
                mainHandler.post(object : Runnable {
                    override fun run() {
                        // call the code below every x seconds:

                        // get updated GPS data ( lat and lon )
                        busList = cvtd.getBuses()
                        Log.d("TAG", "run: busCount    : $busCount")
                        Log.d("TAG", "run: busList.size: ${busList.size}   busList: $busList")

                        // if the original busCount doesn't match the length of the busList then
                        // update the markers to match the buses
                        // CVTD either ended a bus route or added a bus route as scheduled
                        if (busCount != busList.size && busList.isNotEmpty()) {
                            Log.d("TAG", "run: count != size; recalculating marker lists")
                            busCount = busList.size
                            updateMarkers(
                                busCount,
                                busList,
                                markerViewManager,
                                textViewMarkerList,
                                imageViewMarkerList
                            )
                        }

                        // update makers with new GPS data ( lat and lon)
                        if (busList.isNotEmpty()) {
                            updateLocations(busList, textViewMarkerList, imageViewMarkerList)
                        } else {
                            Log.d("TAG", "run: did not update due to lack of new info due to lack of internet connection")
                        }
                        // call the code above every x seconds:
                        mainHandler.postDelayed(this, x * 1000)
                    }
                })


                // update user's position on map
                val locationComponent = mapBoxMap.locationComponent
                locationComponent.activateLocationComponent(
                    LocationComponentActivationOptions.builder(this, style)
                        .useSpecializedLocationLayer(true)
                        .build()
                )
                locationComponent.isLocationComponentEnabled = true
                locationComponent.setCameraMode(
                    CameraMode.TRACKING,
                    object : OnLocationCameraTransitionListener {
                        override fun onLocationCameraTransitionFinished(cameraMode: Int) {
                            locationComponent.zoomWhileTracking(12.5)
                        }

                        override fun onLocationCameraTransitionCanceled(cameraMode: Int) {}
                    }
                )
            }
        })
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    //override fun onSaveInstanceState(outState: Bundle?) {
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
    }

    // update all buses locations on map
    private fun updateLocations(
        busList: MutableList<MutableList<String>>,
        textViewMarkerList: MutableList<MarkerView>,
        imageViewMarkerList: MutableList<MarkerView>
    ) {
        // for each bus update it's text location and image location
        for (i in 0 until textViewMarkerList.size) {
            textViewMarkerList[i].setLatLng(
                LatLng(
                    busList[i][1].toDouble(),
                    busList[i][2].toDouble()
                )
            )
            imageViewMarkerList[i].setLatLng(
                LatLng(
                    busList[i][1].toDouble(),
                    busList[i][2].toDouble()
                )
            )
        }
    }

    // update marker lists with corresponding bus data
    private fun updateMarkers(
        busCount: Int,
        busList: MutableList<MutableList<String>>,
        markerViewManager: MarkerViewManager,
        textViewMarkerList: MutableList<MarkerView>,
        imageViewMarkerList: MutableList<MarkerView>
    ) {
        // remove old data if there is any
        if (imageViewMarkerList.isNotEmpty()) {
            Log.d("TAG", "updateMarkers: removing old data")
            for (i in textViewMarkerList.indices) {
                markerViewManager.removeMarker(textViewMarkerList[i])
                markerViewManager.removeMarker(imageViewMarkerList[i])
            }
            textViewMarkerList.clear()
            imageViewMarkerList.clear()
        }


        // for each bus create a marker for it
        for (i in 0 until busCount) {
            // create a textView for the route
            val textView = TextView(this)
            // put space in between route name and icon with "    "
            val route = "       " + busList[i][0]
            route.also { textView.text = it }
//                    textView.setTextColor(Color.parseColor("#FFFFFF"))
//                    textView.setShadowLayer(10f, 0f, 0f, R.color.black)
//                    textView.textSize = 20F

            // add the textView of the route to the map at the bus's location
            val textViewMarker = MarkerView(
                LatLng(
                    busList[i][1].toDouble(),
                    busList[i][2].toDouble()
                ), textView
            )
            markerViewManager.addMarker(textViewMarker)
            // save the marker in a list for updating it's location later
            textViewMarkerList.add(textViewMarker)

            // create an imageView for the "bus icon"
            val imageView = ImageView(this)
            imageView.layoutParams = LinearLayout.LayoutParams(50, 50)
            // match the color of the bus to the route
            imageView.setColorFilter(Color.parseColor("#" + busList[i][3]));
            val imgResId = R.drawable.ic_bus_24
            imageView.setImageResource(imgResId)

            // add the imageView of the "bus icon" to the map at the bus's location
            val imageViewMarker = MarkerView(
                LatLng(
                    busList[i][1].toDouble(),
                    busList[i][2].toDouble()
                ), imageView
            )
            markerViewManager.addMarker(imageViewMarker)
            // save the marker in a list for updating it's location later
            imageViewMarkerList.add(imageViewMarker)
        }
    }

}