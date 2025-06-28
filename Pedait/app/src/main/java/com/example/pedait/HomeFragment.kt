package com.example.pedait

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import okhttp3.*
import java.io.IOException

class HomeFragment : Fragment(), OnMapReadyCallback {

    private lateinit var scanQRBtn: FloatingActionButton
    private lateinit var findLocationBtn: FloatingActionButton
    private lateinit var scannedValueTv: TextView
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mapFragment: SupportMapFragment

    private val allowedLocation = LatLng(-8.5871109, 116.0971896)
    private val locationThreshold = 50

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

//        scanQRBtn = view.findViewById(R.id.scanQRBtn)
        findLocationBtn = view.findViewById(R.id.findLocationBtn)
//        scannedValueTv = view.findViewById(R.id.textResult)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        locationPermissionRequest.launch(
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )

        mapFragment = SupportMapFragment.newInstance()
        childFragmentManager.beginTransaction()
            .replace(R.id.map_container, mapFragment)
            .commit()
        mapFragment.getMapAsync(this)

//        scanQRBtn.setOnClickListener { checkUserLocationAndScan() }
        findLocationBtn.setOnClickListener { getCurrentLocation() }

        return view
    }

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            checkUserLocation()
        } else {
            Toast.makeText(requireContext(), "Izin lokasi diperlukan", Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun checkUserLocation() {
        if (!isLocationEnabled()) {
            Toast.makeText(requireContext(), "Aktifkan GPS", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            return
        }

        if (ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "Izin lokasi belum diberikan", Toast.LENGTH_LONG).show()
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val userLatLng = LatLng(it.latitude, it.longitude)
                val distance = calculateDistance(userLatLng, allowedLocation)

//                scanQRBtn.isEnabled = true
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        if (!isLocationEnabled()) {
            Toast.makeText(requireContext(), "Aktifkan GPS", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            return
        }

        if (ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "Izin lokasi belum diberikan", Toast.LENGTH_LONG).show()
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val userLatLng = LatLng(it.latitude, it.longitude)
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 19f))
                Toast.makeText(requireContext(), "Lokasi ditemukan!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkUserLocationAndScan() {
        if (ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "Izin lokasi tidak diberikan", Toast.LENGTH_LONG).show()
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val userLatLng = LatLng(it.latitude, it.longitude)
                val distance = calculateDistance(userLatLng, allowedLocation)

                if (distance <= locationThreshold) {
                    scannerLauncher.launch(ScanOptions().setPrompt("Scan QR").setDesiredBarcodeFormats(ScanOptions.QR_CODE))
                } else {
                    Toast.makeText(requireContext(), "Di luar area yang diizinkan", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        map.uiSettings.isMyLocationButtonEnabled = false
        map.uiSettings.isZoomControlsEnabled = false
        map.uiSettings.isZoomGesturesEnabled = true

        if (ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map.isMyLocationEnabled = true
        }

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(allowedLocation, 19f))
    }

    private val scannerLauncher = registerForActivityResult<ScanOptions, ScanIntentResult>(
        ScanContract()
    ) { result ->
        if (result.contents == null) {
            Toast.makeText(requireContext(), "Scan dibatalkan", Toast.LENGTH_SHORT).show()
        } else {
            try {
                val qrData = JSONObject(result.contents)
                val sessionId = qrData.getString("session_id")
                val lat = qrData.getDouble("lat")
                val lng = qrData.getDouble("lng")
                val radius = qrData.optInt("radius", 50)
                val newAllowedLocation = LatLng(lat, lng)

                checkUserDistanceAndSendAttendance(newAllowedLocation, sessionId, radius)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "QR tidak valid", Toast.LENGTH_LONG).show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun checkUserDistanceAndSendAttendance(qrLocation: LatLng, sessionId: String, radius: Int) {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val userLatLng = LatLng(it.latitude, it.longitude)
                val distance = calculateDistance(userLatLng, qrLocation)

                if (distance <= radius) {
                    sendAttendanceToServer(sessionId)
                } else {
                    Toast.makeText(requireContext(), "Di luar area yang diizinkan", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun sendAttendanceToServer(sessionId: String) {
        val studentId = "123456"
        val studentName = "Budi Santoso"

        val json = JSONObject().apply {
            put("session_id", sessionId)
            put("student_id", studentId)
            put("student_name", studentName)
        }

        val requestBody = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("http://10.70.13.4:5000/attended")
            .post(requestBody)
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                requireActivity().runOnUiThread {
                    val msg = if (response.isSuccessful) "Presensi berhasil" else "Gagal mengirim presensi"
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun calculateDistance(start: LatLng, end: LatLng): Float {
        val results = FloatArray(1)
        Location.distanceBetween(start.latitude, start.longitude, end.latitude, end.longitude, results)
        return results[0]
    }
}
