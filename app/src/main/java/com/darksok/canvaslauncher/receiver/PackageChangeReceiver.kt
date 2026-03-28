package com.darksok.canvaslauncher.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.darksok.canvaslauncher.core.packages.events.PackageEvent
import com.darksok.canvaslauncher.core.packages.events.PackageEventsBus
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PackageChangeReceiver : BroadcastReceiver() {

    @Inject
    lateinit var packageEventsBus: PackageEventsBus

    override fun onReceive(
        context: Context?,
        intent: Intent?,
    ) {
        val action = intent?.action ?: return
        val packageName = intent.data?.schemeSpecificPart ?: return

        val event = when (action) {
            Intent.ACTION_PACKAGE_ADDED -> {
                val replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                if (replacing) PackageEvent.Changed(packageName) else PackageEvent.Added(packageName)
            }
            Intent.ACTION_PACKAGE_REMOVED -> {
                val replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                if (replacing) null else PackageEvent.Removed(packageName)
            }
            Intent.ACTION_PACKAGE_CHANGED -> PackageEvent.Changed(packageName)
            else -> null
        } ?: return

        val pendingResult = goAsync()
        try {
            packageEventsBus.publish(event)
        } finally {
            pendingResult.finish()
        }
    }
}
