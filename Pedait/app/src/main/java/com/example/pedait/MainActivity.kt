package com.example.pedait

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.journeyapps.barcodescanner.CaptureActivity
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions

class MainActivity : AppCompatActivity() {

    private lateinit var scanQRBtn: FloatingActionButton
    private lateinit var scannedValueTv : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        scanQRBtn = findViewById(R.id.scanQRBtn)
        scannedValueTv = findViewById(R.id.textResult)

        registerUiListener()
    }

    private fun registerUiListener(){
        scanQRBtn.setOnClickListener {
            scannerLauncher.launch(ScanOptions().setPrompt("Scan QR Code").setDesiredBarcodeFormats(
                ScanOptions.QR_CODE))
        }
    }
    private val scannerLauncher = registerForActivityResult<ScanOptions, ScanIntentResult>(
        ScanContract()
    ){result ->
        if (result.contents == null){
            Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
        }else{
            scannedValueTv.text = buildString {
                append("Scanned Value : ")
                append(result.contents)
            }
        }
    }
}