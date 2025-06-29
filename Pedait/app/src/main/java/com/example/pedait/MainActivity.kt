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
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var fusedLocationClient: FusedLocationProviderClient

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
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

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
            // ✅ Langsung buka scanner tanpa cek lokasi awal
            scannerLauncher.launch(
                ScanOptions().setPrompt("Scan QR Code").setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            )
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

                val qrLocation = LatLng(lat, lng)

                // ✅ Validasi lokasi pengguna terhadap QR code
                checkUserDistanceAndSendAttendance(
                    qrLocation,
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
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Izin lokasi tidak diberikan", Toast.LENGTH_LONG).show()
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val userLatLng = LatLng(location.latitude, location.longitude)
                val distance = calculateDistance(userLatLng, qrLocation)

                if (distance <= radius) {
                    checkIfAlreadyAttended(sessionId, courseId, meetingId)
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

    private fun checkIfAlreadyAttended(sessionId: String, courseId: String, meetingId: String) {
        val currentUser = auth.currentUser ?: return

        firestore.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { doc ->
                val studentId = doc.getString("nim") ?: return@addOnSuccessListener

                firestore.collection("course").document(courseId)
                    .collection("meetings").document(meetingId)
                    .collection("sessions").document(sessionId)
                    .collection("attendances").document(studentId)
                    .get()
                    .addOnSuccessListener { attDoc ->
                        val status = attDoc.getString("status")?.lowercase()
                        if (status == "hadir") {
                            Toast.makeText(this, "Anda sudah presensi sebelumnya", Toast.LENGTH_LONG).show()
                        } else {
                            sendAttendanceToServer(sessionId, courseId, meetingId)
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Gagal cek status presensi", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal mengambil NIM", Toast.LENGTH_SHORT).show()
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

                    val sharedPref = getSharedPreferences("user_data", MODE_PRIVATE)
                    sharedPref.edit().putString("nim", studentId).apply()

                    val json = JSONObject().apply {
                        put("session_id", sessionId)
                        put("student_id", studentId)
                        put("student_name", studentName)
                        put("course_id", courseId)
                        put("meeting_id", meetingId)
                    }

                    val requestBody = json.toString().toRequestBody("application/json".toMediaType())

                    val request = Request.Builder()
                        .url("http://10.70.1.196:5000/attended")
                        .post(requestBody)
                        .build()

                    OkHttpClient().newCall(request).enqueue(object : Callback {
                        override fun onResponse(call: Call, response: Response) {
                            val responseBody = response.body?.string()
                            Log.e("Attendance", "Response code: ${response.code}, body: $responseBody")
                            runOnUiThread {
                                if (response.code == 410) {
                                    Toast.makeText(this@MainActivity, "QR Code sudah kadaluarsa", Toast.LENGTH_LONG).show()
                                } else if (response.isSuccessful) {
                                    Toast.makeText(this@MainActivity, "Presensi berhasil", Toast.LENGTH_SHORT).show()
                                    // Ambil NIM dari SharedPreferences
                                    val sharedPref = getSharedPreferences("user_data", MODE_PRIVATE)
                                    val nim = sharedPref.getString("nim", null)

                                    if (nim != null) {
                                        val meetingsFragment = MeetingsFragment.newInstance(courseId, nim)
                                        openFragment(meetingsFragment)
                                        bottomNavigationView.menu.findItem(R.id.menuPresence).isChecked = true
                                    } else {
                                        Toast.makeText(this@MainActivity, "NIM tidak ditemukan", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
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
            // tidak perlu apa-apa karena scan langsung dibuka lewat FAB
        } else {
            Toast.makeText(this, "Izin lokasi diperlukan untuk scan", Toast.LENGTH_LONG).show()
        }
    }
}
