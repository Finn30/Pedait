package com.example.pedait

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.pedait.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

//    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
//        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        val fab = findViewById<FloatingActionButton>(R.id.scanQRBtn)
        bottomNavigationView.background = null

        val menu = bottomNavigationView.menu
        val placeholderItem = menu.findItem(R.id.placeholder)
        placeholderItem.isEnabled = false

        openFragment(HomeFragment())

        bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.menuHome -> openFragment(HomeFragment())
                R.id.menuSchedule -> openFragment(ScheduleFragment())
                R.id.menuPresence -> openFragment(PresenceFragment())
                R.id.menuProfile -> openFragment(ProfileFragment())
            }
            true
        }

        // FAB click - buka Scan QR Fragment atau Activity
        fab.setOnClickListener {
            startActivity(Intent(this, ScanQRActivity::class.java))
        }

    }

    private fun openFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.map_container, fragment)
            .commit()
    }
}