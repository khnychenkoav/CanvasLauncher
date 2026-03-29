package com.darksok.canvaslauncher

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.darksok.canvaslauncher.feature.launcher.presentation.LauncherRoute
import com.darksok.canvaslauncher.i18n.AppLocaleManager
import com.darksok.canvaslauncher.settings.SettingsActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

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
            LauncherRoute(
                onOpenSettings = {
                    runCatching {
                        startActivity(Intent(this, SettingsActivity::class.java))
                    }
                },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (AppLocaleManager.shouldRecreateForPreferredLanguage(this)) {
            recreate()
        }
    }
}
