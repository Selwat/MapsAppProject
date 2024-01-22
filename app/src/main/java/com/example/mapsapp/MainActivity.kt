package com.example.mapsapp

import android.Manifest
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.util.*
import kotlin.concurrent.fixedRateTimer
import kotlin.math.cos
import kotlin.random.Random

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private var mGoogleMap: GoogleMap? = null

    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private val markers = mutableListOf<Marker>()
    private var lastMonsterGenerationTime: Long = 0
    private val MONSTER_GENERATION_INTERVAL = 10 * 60 * 1000 // 10 minut w milisekundach
    private val MONSTER_CHECK_INTERVAL = 10 * 1000
    private var permissionRequested = false
    private lateinit var monsterCountTextView: TextView
    private lateinit var monsterCatchCountTextView: TextView
    private val capturedMonsters = mutableSetOf<Marker>()
    private var monstersCaughtCount = 0

    private fun updateMonsterCountTextView(count: Int) {
        monsterCountTextView.text = "Liczba potworów w twojej okolicy: " + count.toString()
    }
    private fun updateMonsterCountCatchTextView() {
        monsterCatchCountTextView.text = "Złapane potwory: " + monstersCaughtCount.toString()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        monsterCatchCountTextView = findViewById(R.id.monsterCatchCountTextView)
        monsterCountTextView = findViewById(R.id.monsterCountTextView)
        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Ustawienia do żądania lokalizacji
        locationRequest = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(10000) // 1 sekund
            .setFastestInterval(10000) // 1 sekund


        fixedRateTimer("CheckMonsters", false, 0, MONSTER_CHECK_INTERVAL.toLong()) {
            runOnUiThread {
                checkMonsters()

            }
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let {
                    val newLatLng = LatLng(it.latitude, it.longitude)

                    // Usuń tylko marker użytkownika z mapy
                    markers.firstOrNull { marker -> marker.title == "My Location" }?.remove()
                    markers.removeAll { marker -> marker.title == "My Location" }

                    // Dodaj nowy marker użytkownika
                    val marker = mGoogleMap?.addMarker(MarkerOptions().position(newLatLng).title("My Location"))
                    marker?.let {
                        markers.add(marker)
                    }

                    // Aktualizuj mapę z nową lokalizacją
                    updateMapLocation(newLatLng)
                }
            }
        }

        checkMonsters()
        // Rozpocznij aktualizacje lokalizacji
        startLocationUpdates()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mGoogleMap = googleMap

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startLocationUpdates()
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    mGoogleMap?.addMarker(MarkerOptions().position(currentLatLng).title("My Location"))
                    mGoogleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))

                    // Sprawdź, czy minęło wystarczająco czasu od ostatniego generowania potworów
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastMonsterGenerationTime >= MONSTER_GENERATION_INTERVAL) {
                        generateMonsters(currentLatLng)
                        lastMonsterGenerationTime = currentTime
                    }
                }
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun updateMapLocation(latLng: LatLng) {

        // Aktualizuj mapę z nową lokalizac
        mGoogleMap?.addMarker(MarkerOptions().position(latLng).title("My Location"))
        mGoogleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
    }

    private fun startLocationUpdates() {
        // Sprawdź uprawnienia
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        ) {
            // Włącz aktualizacje lokalizacji
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        } else {
            // Poproś o uprawnienia, jeżeli nie są dostępne
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun generateRandomLocationNearby(center: LatLng, radius: Double): LatLng {
        val random = Random.Default
        val lat = center.latitude + (random.nextDouble() * 2 - 1) * (radius / 51000.0)
        val lng = center.longitude + (random.nextDouble() * 2 - 1) * (radius / (51000.0 * cos(
            Math.toRadians(center.latitude)
        )))
        return LatLng(lat, lng)
    }
    private fun generateMonsters(center: LatLng) {
        val nearbyPoints = mutableListOf<LatLng>()
        updateMonsterCountTextView(nearbyPoints.size)
        updateMonsterCountCatchTextView()
        repeat(10) {
            val randomLocation = generateRandomLocationNearby(center, 500.0)
            val marker = mGoogleMap?.addMarker(MarkerOptions().position(randomLocation).title("Point $it"))
            marker?.let {
                markers.add(marker)
            }
            val distance = calculateDistance(center, randomLocation)
            if (distance < 600) {
                nearbyPoints.add(randomLocation)
            }
        }
    }


    private fun calculateDistance(location1: LatLng, location2: LatLng): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            location1.latitude, location1.longitude,
            location2.latitude, location2.longitude, results
        )
        return results[0]
    }



    private fun checkMonsters() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { currentLocation ->
                currentLocation?.let {
                    val currentLatLng = LatLng(currentLocation.latitude, currentLocation.longitude)

                    // Filter out markers with the title "My Location"
                    val nearbyMonsters = markers.filter { marker ->
                        marker.title != "My Location" && calculateDistance(currentLatLng, marker.position) < 600
                    }

                    runOnUiThread {
                        for (monsterMarker in nearbyMonsters) {
                            val distance = calculateDistance(currentLatLng, monsterMarker.position)
                            if (distance < 300) {
                                showCatchMonsterDialog(monsterMarker)
                            }
                        }
                        updateMonsterCountTextView(nearbyMonsters.size)
                    }
                }
            }
        } else {
            if (!permissionRequested) {
                permissionRequested = true
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }


    private fun showCatchMonsterDialog(monsterMarker: Marker) {
        val title = "Potwór " + monsterMarker.title + " w zasięgu!"
        val message = "Czy chcesz złapać potwora w odległości 300 metrów?"

        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle(title)
        alertDialogBuilder.setMessage(message)
        alertDialogBuilder.setPositiveButton("Tak") { dialogInterface: DialogInterface, i: Int ->
            // Remove the monster marker from the map
            monsterMarker.remove()

            // Remove the monster marker from the markers list
            markers.remove(monsterMarker)
            monstersCaughtCount++
            // Update the monster count TextView
            updateMonsterCountCatchTextView()
            updateMonsterCountTextView(markers.size)

        }

        alertDialogBuilder.setNegativeButton("Nie") { dialogInterface: DialogInterface, i: Int ->
            // Handle the case when the player chooses not to catch the monster
        }

        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }


}