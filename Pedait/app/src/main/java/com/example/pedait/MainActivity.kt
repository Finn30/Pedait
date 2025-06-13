package com.example.pedait

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.pedait.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import androidx.core.view.size

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Gedung A
    private val allowedLocation = LatLng(-8.5871109, 116.0971896)

    // Gedung D
//    private val allowedLocation = LatLng(-8.5867426, 116.0967105)

    // Gedung X
//    private val allowedLocation = LatLng(-8.6223672, 116.0877100)

    private val locationThreshold = 50

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        val fab = findViewById<FloatingActionButton>(R.id.scanQRBtn)

        bottomNavigationView.background = null
        bottomNavigationView.menu.findItem(R.id.placeholder).isEnabled = false

        handleNavigationIntent(intent)

        bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.menuHome -> openFragment(HomeFragment())
                R.id.menuSchedule -> openFragment(ScheduleFragment())
                R.id.menuPresence -> openFragment(PresenceFragment())
                R.id.menuProfile -> openFragment(ProfileFragment())
            }
            true
        }

        fab.setOnClickListener {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            checkLocationThenAllowScan()
        }

    }

    private fun openFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.map_container, fragment)
            .commit()
    }

    private fun navigateToFragment(fragment: Fragment, menuItemId: Int) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.map_container, fragment)
            .commit()
        bottomNavigationView.selectedItemId = menuItemId
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNavigationIntent(intent)
    }

    private fun handleNavigationIntent(intent: Intent) {
        when (intent.getStringExtra("navigateTo")) {
            "presence" -> navigateToFragment(PresenceFragment(), R.id.menuPresence)
            "schedule" -> navigateToFragment(ScheduleFragment(), R.id.menuSchedule)
            "profile" -> navigateToFragment(ProfileFragment(), R.id.menuProfile)
            else -> navigateToFragment(HomeFragment(), R.id.menuHome)
        }
    }

    fun distanceBetween(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val loc1 = Location("").apply {
            latitude = lat1
            longitude = lng1
        }
        val loc2 = Location("").apply {
            latitude = lat2
            longitude = lng2
        }
        return loc1.distanceTo(loc2) // meter
    }


    @SuppressLint("MissingPermission")
    private fun checkLocationThenAllowScan() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Izin lokasi tidak diberikan", Toast.LENGTH_LONG).show()
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val userLatLng = LatLng(location.latitude, location.longitude)
                val distance = calculateDistance(userLatLng, allowedLocation)

                if (distance <= locationThreshold) {
                    // Lokasi valid, baru boleh scan
                    scannerLauncher.launch(
                        ScanOptions().setPrompt("Scan QR Code").setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                    )
                } else {
                    Toast.makeText(this, "Anda berada di luar area yang diizinkan untuk scan", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Tidak bisa mendapatkan lokasi", Toast.LENGTH_LONG).show()
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

    private fun calculateDistance(start: LatLng, end: LatLng): Float {
        val results = FloatArray(1)
        Location.distanceBetween(start.latitude, start.longitude, end.latitude, end.longitude, results)
        return results[0]
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
                val courseId = qrData.optString("course_id", "unknown_course")
                val meetingId = qrData.optString("meeting_id", "unknown_meeting")

                // Ganti allowedLocation dengan lokasi dari QR
                val newAllowedLocation = LatLng(lat, lng)

//                checkUserDistanceAndSendAttendance(newAllowedLocation, sessionId, radius)
                checkUserDistanceAndSendAttendance(
                    LatLng(lat, lng),
                    sessionId,
                    radius,
                    courseId,
                    meetingId
                )
            } catch (e: Exception) {
                Toast.makeText(this, "QR Code tidak valid", Toast.LENGTH_LONG).show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun checkUserDistanceAndSendAttendance(qrLocation: LatLng, sessionId: String, radius: Int, courseId: String, meetingId: String) {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val userLatLng = LatLng(location.latitude, location.longitude)
                val distance = calculateDistance(userLatLng, qrLocation)

                if (distance <= radius) {
                    sendAttendanceToServer(sessionId, courseId, meetingId)
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

    private fun sendAttendanceToServer(sessionId: String, courseId: String, meetingId: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Pengguna tidak ditemukan", Toast.LENGTH_LONG).show()
            return
        }

        val uid = currentUser.uid

        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val studentId = document.getString("nim") ?: "N/A"
                    val studentName = document.getString("nama") ?: "N/A"

                    // ✅ SIMPAN NIM DI SHARED PREFERENCES
                    val sharedPref = getSharedPreferences("user_data", MODE_PRIVATE)
                    sharedPref.edit().putString("nim", studentId).apply()

                    val json = JSONObject()
                    Log.e("Attendance", "Student ID: $studentId, Name: $studentName")
                    json.put("session_id", sessionId)
                    json.put("student_id", studentId)
                    json.put("student_name", studentName)
                    json.put("course_id", courseId)
                    json.put("meeting_id", meetingId)

                    val requestBody = json.toString().toRequestBody("application/json".toMediaType())

                    val request = Request.Builder()
                        .url("http://10.70.14.195:5000/attended")
                        .post(requestBody)
                        .build()

                    val client = OkHttpClient()
                    client.newCall(request).enqueue(object : Callback {
                        override fun onResponse(call: Call, response: Response) {
                            val responseBody = response.body?.string()
                            Log.e("Attendance", "Response code: ${response.code}, body: $responseBody")
                            if (response.isSuccessful) {
                                runOnUiThread {
                                    Toast.makeText(this@MainActivity, "Presensi berhasil", Toast.LENGTH_SHORT).show()
                                    navigateToFragment(PresenceFragment(), R.id.menuPresence)
                                }
                            } else {
                                runOnUiThread {
                                    Toast.makeText(this@MainActivity, "Gagal mengirim data presensi", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }

                        override fun onFailure(call: Call, e: IOException) {
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    })

                } else {
                    Toast.makeText(this, "Data pengguna tidak ditemukan di Firestore", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal mengambil data pengguna: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkUserLocationAndScan()
        } else {
            Toast.makeText(this, "Izin lokasi diperlukan untuk scan", Toast.LENGTH_LONG).show()
        }
    }
}