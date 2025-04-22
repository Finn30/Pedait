package com.example.pedait

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import okhttp3.*
import java.io.IOException


class HomeActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var scanQRBtn: FloatingActionButton
    private lateinit var findLocationBtn: FloatingActionButton
    private lateinit var scannedValueTv: TextView
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

//    private val allowedLocation = LatLng(-8.586907, 116.092187)

//    private val allowedLocation = LatLng(-8.6224467, 116.0875726)

    // Gedung A
    private val allowedLocation = LatLng(-8.5871109, 116.0971896)

    // Gedung D
//        private val allowedLocation = LatLng(-8.5867426, 116.0967105)

    private val locationThreshold = 50

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        scanQRBtn = findViewById(R.id.scanQRBtn)
        findLocationBtn = findViewById(R.id.findLocationBtn)
        scannedValueTv = findViewById(R.id.textResult)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationPermissionRequest.launch(
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        findLocationBtn.setOnClickListener {
            getCurrentLocation()
        }

        scanQRBtn.setOnClickListener {
            checkUserLocationAndScan()
        }
    }

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            checkUserLocation()
        } else {
            Toast.makeText(this, "Izin lokasi diperlukan untuk menggunakan fitur ini", Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun checkUserLocation() {
        if (!isLocationEnabled()) {
            Toast.makeText(this, "Aktifkan GPS untuk mendapatkan lokasi", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            return
        }

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Izin lokasi belum diberikan", Toast.LENGTH_LONG).show()
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val userLatLng = LatLng(location.latitude, location.longitude)
                val distance = calculateDistance(userLatLng, allowedLocation)

                if (distance <= locationThreshold) {
                    scanQRBtn.isEnabled = true
                } else {
                    scanQRBtn.isEnabled = true
                    Toast.makeText(this, "Anda berada di luar area yang diizinkan", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Lokasi tidak ditemukan, coba lagi", Toast.LENGTH_LONG).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Gagal mendapatkan lokasi: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        if (!isLocationEnabled()) {
            Toast.makeText(this, "Aktifkan GPS untuk mendapatkan lokasi", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            return
        }

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Izin lokasi belum diberikan", Toast.LENGTH_LONG).show()
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val userLatLng = LatLng(location.latitude, location.longitude)
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 19f))
                Toast.makeText(this, "Lokasi ditemukan!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Lokasi tidak ditemukan, coba lagi", Toast.LENGTH_LONG).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Gagal mendapatkan lokasi: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkUserLocationAndScan() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Izin lokasi tidak diberikan, aktifkan di pengaturan", Toast.LENGTH_LONG).show()
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val userLatLng = LatLng(location.latitude, location.longitude)
                val distance = calculateDistance(userLatLng, allowedLocation)

                if (distance <= locationThreshold) {
                    scannerLauncher.launch(
                        ScanOptions().setPrompt("Scan QR Code").setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                    )
                } else {
                    Toast.makeText(this, "Anda berada di luar area yang diizinkan", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Lokasi tidak ditemukan, coba lagi", Toast.LENGTH_LONG).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Gagal mendapatkan lokasi: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        map.uiSettings.isMyLocationButtonEnabled = false
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isZoomGesturesEnabled = true

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map.isMyLocationEnabled = true
        }

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(allowedLocation, 19f))
    }

    private val scannerLauncher = registerForActivityResult<ScanOptions, ScanIntentResult>(
        ScanContract()
    ) { result ->
        if (result.contents == null) {
            Toast.makeText(this, "Scan dibatalkan", Toast.LENGTH_SHORT).show()
        } else {
            try {
                val qrData = JSONObject(result.contents)
                val sessionId = qrData.getString("session_id")
                val lat = qrData.getDouble("lat")
                val lng = qrData.getDouble("lng")
                val radius = qrData.optInt("radius", 50)

                // Ganti allowedLocation dengan lokasi dari QR
                val newAllowedLocation = LatLng(lat, lng)

                checkUserDistanceAndSendAttendance(newAllowedLocation, sessionId, radius)

            } catch (e: Exception) {
                Toast.makeText(this, "QR Code tidak valid", Toast.LENGTH_LONG).show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun checkUserDistanceAndSendAttendance(qrLocation: LatLng, sessionId: String, radius: Int) {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val userLatLng = LatLng(location.latitude, location.longitude)
                val distance = calculateDistance(userLatLng, qrLocation)

                if (distance <= radius) {
                    sendAttendanceToServer(sessionId)
                } else {
                    Toast.makeText(this, "Anda berada di luar area yang diizinkan", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Lokasi tidak ditemukan, coba lagi", Toast.LENGTH_LONG).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Gagal mendapatkan lokasi: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }



    private fun sendAttendanceToServer(sessionId: String) {
        // Ganti dengan ID mahasiswa yang sesuai
        val studentId = "123456"
        val studentName = "Budi Santoso"

        val json = JSONObject()
        json.put("session_id", sessionId)
        json.put("student_id", studentId)
        json.put("student_name", studentName)


        val requestBody = json.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("http://10.70.13.4:5000/attended")
            .post(requestBody)
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@HomeActivity, "Presensi berhasil", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@HomeActivity, "Gagal mengirim data presensi", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@HomeActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun calculateDistance(start: LatLng, end: LatLng): Float {
        val results = FloatArray(1)
        Location.distanceBetween(start.latitude, start.longitude, end.latitude, end.longitude, results)
        return results[0]
    }
}
