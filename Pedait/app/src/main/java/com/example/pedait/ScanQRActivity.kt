package com.example.pedait

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.pedait.HomeActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
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

class ScanQRActivity : AppCompatActivity() {

    private lateinit var scannedValueTv: TextView
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Gedung A
    private val allowedLocation = LatLng(-8.5871109, 116.0971896)

    private val locationThreshold = 50

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_scan_qractivity)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                100
            )
        }

        scannedValueTv = findViewById(R.id.textResult)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        checkUserLocationAndScan()
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
            .url("http://10.70.0.52:5000/attended")
            .post(requestBody)
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@ScanQRActivity, "Presensi berhasil", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@ScanQRActivity, "Gagal mengirim data presensi", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@ScanQRActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        })
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