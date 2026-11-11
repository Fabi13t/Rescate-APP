package com.rescate.app.ui.merchant

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.rescate.app.R
import com.rescate.app.databinding.ActivityMerchantBinding

 
class MerchantActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMerchantBinding

    private val notifPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMerchantBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        binding.bottomNav.setupWithNavController(navHost.navController)
        val tabs = setOf(R.id.mHomeFragment, R.id.mOrdersFragment, R.id.mNewPackFragment, R.id.mPaymentsFragment, R.id.mProfileFragment)
        navHost.navController.addOnDestinationChangedListener { _, dest, _ ->
            val show = dest.id in tabs
            binding.bottomNav.animate().translationY(if (show) 0f else binding.bottomNav.height.toFloat()).setDuration(200)
                .withStartAction { if (show) binding.bottomNav.visibility = android.view.View.VISIBLE }
                .withEndAction { if (!show) binding.bottomNav.visibility = android.view.View.GONE }
                .start()
        }

        if (savedInstanceState == null && Build.VERSION.SDK_INT >= 33) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

     
    fun goTo(tabId: Int) {
        binding.bottomNav.selectedItemId = tabId
    }
}
