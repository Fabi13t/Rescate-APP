package com.rescate.app.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.rescate.app.R
import com.rescate.app.data.local.SessionManager
import com.rescate.app.data.model.Role
import com.rescate.app.ui.main.MainActivity
import com.rescate.app.ui.merchant.MerchantActivity

 
class AuthActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val session = SessionManager.get(this)
        val user = session.user
        if (session.isLoggedIn && user != null) {
            openHome(user.role)
            return
        }
        setContentView(R.layout.activity_auth)
    }

    fun openHome(role: Role) {
        val target = if (role == Role.COMERCIO) MerchantActivity::class.java else MainActivity::class.java
        startActivity(Intent(this, target))
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.fade_up, R.anim.fade_out)
        finish()
    }
}
