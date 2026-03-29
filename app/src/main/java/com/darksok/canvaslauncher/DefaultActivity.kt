package com.darksok.canvaslauncher

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.darksok.canvaslauncher.defaultscreen.DefaultRoute
import com.darksok.canvaslauncher.i18n.AppLocaleManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DefaultActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocaleManager.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        setContent {
            DefaultRoute()
        }
    }

    override fun onResume() {
        super.onResume()
        if (AppLocaleManager.shouldRecreateForPreferredLanguage(this)) {
            recreate()
        }
    }
}
