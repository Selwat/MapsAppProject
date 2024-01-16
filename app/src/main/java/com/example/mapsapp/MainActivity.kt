package com.example.mapsapp

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlin.math.cos
import kotlin.random.Random

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private var mGoogleMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mGoogleMap = googleMap

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            setMyLocation()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun setMyLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val currentLatLng = LatLng(location.latitude, location.longitude)

                mGoogleMap?.addMarker(MarkerOptions().position(currentLatLng).title("My Location"))
                mGoogleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))

                val nearbyPoints = mutableListOf<LatLng>()

                repeat(10) {
                    val randomLocation =
                        generateRandomLocationNearby(currentLatLng, 500.0)
                    mGoogleMap?.addMarker(
                        MarkerOptions().position(randomLocation).title("Point $it")
                    )

                    // Sprawdź odległość od wylosowanego punktu
                    val distance = calculateDistance(currentLatLng, randomLocation)
                    if (distance < 600) {
                        nearbyPoints.add(randomLocation)
                    }
                }

                showConfirmationDialog(nearbyPoints.size)
            }
        }
    }

    private fun generateRandomLocationNearby(center: LatLng, radius: Double): LatLng {
        val random = Random.Default
        val lat = center.latitude + (random.nextDouble() * 2 - 1) * (radius / 50000.0)
        val lng = center.longitude + (random.nextDouble() * 2 - 1) * (radius / (50000.0 * cos(
            Math.toRadians(center.latitude)
        )))
        return LatLng(lat, lng)
    }

    private fun calculateDistance(location1: LatLng, location2: LatLng): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            location1.latitude, location1.longitude,
            location2.latitude, location2.longitude, results
        )
        return results[0]
    }

    private fun showConfirmationDialog(nearbyPointsCount: Int) {
        val title = "Liczba potworów w promieniu 100m:"
        val message = "$nearbyPointsCount"

        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle(title)
        alertDialogBuilder.setMessage(message)
        alertDialogBuilder.setPositiveButton("OK") { dialogInterface: DialogInterface, i: Int ->
            // Tutaj możesz umieścić kod, który zostanie wykonany po naciśnięciu przycisku "OK"
        }

        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }
}