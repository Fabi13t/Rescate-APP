package com.rescate.app.ui.main

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.rescate.app.R
import com.rescate.app.databinding.ActivityMainBinding
import com.rescate.app.util.LocationHelper

 
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val topLevel = setOf(
        R.id.homeFragment, R.id.mapFragment, R.id.reservationsFragment, R.id.impactFragment, R.id.profileFragment
    )

    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        LocationHelper.current(this) { }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        val navController = navHost.navController
        binding.bottomNav.setupWithNavController(navController)

         
        navController.addOnDestinationChangedListener { _, destination, _ ->
            showBottomNav(destination.id in topLevel)
        }

        if (savedInstanceState == null) askPermissions()
    }

    private fun askPermissions() {
        val list = mutableListOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= 33) list += Manifest.permission.POST_NOTIFICATIONS
        permissions.launch(list.toTypedArray())
    }

    private fun showBottomNav(show: Boolean) {
        val nav = binding.bottomNav
        if (show && nav.visibility == View.VISIBLE && nav.translationY == 0f) return
        if (show) {
            nav.visibility = View.VISIBLE
            nav.animate().translationY(0f).setDuration(220).start()
        } else if (nav.visibility == View.VISIBLE) {
            nav.animate().translationY(nav.height.toFloat()).setDuration(180)
                .withEndAction { nav.visibility = View.GONE }.start()
        }
    }
}
