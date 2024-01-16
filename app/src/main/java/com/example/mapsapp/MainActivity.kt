package com.example.mapsapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
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
            // Jeżeli masz uprawnienia do lokalizacji, ustaw aktualną lokalizację użytkownika na mapie
            setMyLocation()
        } else {
            // Jeżeli nie masz uprawnień, poproś użytkownika o nie
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun setMyLocation() {
        // Uzyskaj aktualną lokalizację użytkownika
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val currentLatLng = LatLng(location.latitude, location.longitude)

                // Ustaw marker na aktualnej lokalizacji użytkownika
                mGoogleMap?.addMarker(MarkerOptions().position(currentLatLng).title("My Location"))

                // Przesuń kamerę na aktualną lokalizację użytkownika
                mGoogleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))

                // Dodaj 10 losowych markerów w odległości 1 km od aktualnej lokalizacji użytkownika
                repeat(10) {
                    val randomLocation =
                        generateRandomLocationNearby(currentLatLng, 1000.0) // Odległość w metrach
                    mGoogleMap?.addMarker(
                        MarkerOptions().position(randomLocation).title("Point $it")
                    )
                }
            }
        }
    }

    // Funkcja do generowania losowych współrzędnych w odległości od danej lokalizacji
    private fun generateRandomLocationNearby(center: LatLng, radius: Double): LatLng {
        val random = Random.Default
        val lat = center.latitude + (random.nextDouble() * 2 - 1) * (radius / 20111.0)
        val lng = center.longitude + (random.nextDouble() * 2 - 1) * (radius / (20111.0 * cos(
            Math.toRadians(center.latitude)
        )))
        return LatLng(lat, lng)
    }
}
