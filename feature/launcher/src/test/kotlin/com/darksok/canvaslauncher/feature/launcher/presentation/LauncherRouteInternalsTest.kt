package com.darksok.canvaslauncher.feature.launcher.presentation

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.net.Uri
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LauncherRouteInternalsTest {

    private val app: Application = ApplicationProvider.getApplicationContext()
    private val context: Context = app

    @Test
    fun `permission helpers react to granted permissions and notification listener`() {
        val missingBefore = invokeList("collectMissingRequiredPermissions", context)
        assertThat(missingBefore).isNotEmpty()

        val runtimeBefore = invokeStringArray("collectRequiredRuntimePermissionsToRequest", context)
        assertThat(runtimeBefore.toList()).contains(Manifest.permission.READ_CONTACTS)
        assertThat(runtimeBefore.toList()).contains(Manifest.permission.ACCESS_FINE_LOCATION)

        shadowOf(app).grantPermissions(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS,
        )

        val runtimeAfter = invokeStringArray("collectRequiredRuntimePermissionsToRequest", context)
        assertThat(runtimeAfter).isEmpty()

        val hasAccessBefore = invokeBoolean("hasNotificationListenerAccess", context)
        assertThat(hasAccessBefore).isFalse()

        val listener = "${context.packageName}/${CanvasNotificationListenerService::class.java.name}"
        Settings.Secure.putString(
            context.contentResolver,
            "enabled_notification_listeners",
            listener,
        )
        val hasAccessAfter = invokeBoolean("hasNotificationListenerAccess", context)
        assertThat(hasAccessAfter).isTrue()
    }

    @Test
    fun `query and dial helpers validate blank input and open when resolvable`() {
        val blankQuery = invokeBoolean("openQueryInDefaultBrowser", context, "   ")
        assertThat(blankQuery).isFalse()

        val queryIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.google.com/search?q=test"),
        )
        registerResolvableIntent(queryIntent)
        val queryOpened = invokeBoolean("openQueryInDefaultBrowser", context, "test")
        assertThat(queryOpened).isTrue()

        val blankDial = invokeBoolean("openContactInDialer", context, " ")
        assertThat(blankDial).isFalse()

        val dialIntent = Intent(
            Intent.ACTION_DIAL,
            Uri.parse("tel:12345"),
        )
        registerResolvableIntent(dialIntent)
        val dialOpened = invokeBoolean("openContactInDialer", context, "12345")
        assertThat(dialOpened).isTrue()
    }

    @Test
    fun `launchIntentCompat returns false for unresolved intents and true for resolved`() {
        val unresolved = invokeBoolean(
            "launchIntentCompat",
            context,
            Intent("com.darksok.canvaslauncher.UNRESOLVED"),
        )
        assertThat(unresolved).isFalse()

        val resolvedIntent = Intent("com.darksok.canvaslauncher.RESOLVED")
        registerResolvableIntent(resolvedIntent)
        val resolved = invokeBoolean("launchIntentCompat", context, resolvedIntent)
        assertThat(resolved).isTrue()
    }

    @Test
    fun `settings and uninstall helpers open when at least one intent is resolvable`() {
        val permissionIntent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null),
        )
        registerResolvableIntent(permissionIntent)
        val appSettingsOpened = invokeBoolean("openAppPermissionSettings", context)
        assertThat(appSettingsOpened).isTrue()

        val listenerSettingsIntent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        registerResolvableIntent(listenerSettingsIntent)
        val listenerSettingsOpened = invokeBoolean("openNotificationListenerSettings", context)
        assertThat(listenerSettingsOpened).isTrue()

        val uninstallIntent = Intent(
            Intent.ACTION_DELETE,
            Uri.fromParts("package", "pkg.to.remove", null),
        ).putExtra(Intent.EXTRA_RETURN_RESULT, false)
        registerResolvableIntent(uninstallIntent)
        val uninstallOpened = invokeBoolean("openUninstallApp", context, "pkg.to.remove")
        assertThat(uninstallOpened).isTrue()
    }

    @Test
    fun `findActivity unwraps nested context wrappers`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val wrapped = ContextWrapper(ContextWrapper(activity))
        val found = invokeContextToActivity("findActivity", wrapped)
        val notFound = invokeContextToActivity("findActivity", context)

        assertThat(found).isNotNull()
        assertThat(notFound).isNull()
    }

    private fun registerResolvableIntent(intent: Intent) {
        val resolveInfo = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = "com.example.handler"
                name = "HandlerActivity"
            }
        }
        shadowOf(context.packageManager).addResolveInfoForIntent(intent, resolveInfo)
    }

    private fun invokeBoolean(
        methodName: String,
        context: Context,
        text: String,
    ): Boolean {
        val method = launcherRouteKtClass.getDeclaredMethod(methodName, Context::class.java, String::class.java)
        method.isAccessible = true
        return method.invoke(null, context, text) as Boolean
    }

    private fun invokeBoolean(
        methodName: String,
        context: Context,
    ): Boolean {
        val method = launcherRouteKtClass.getDeclaredMethod(methodName, Context::class.java)
        method.isAccessible = true
        return method.invoke(null, context) as Boolean
    }

    private fun invokeBoolean(
        methodName: String,
        context: Context,
        intent: Intent,
    ): Boolean {
        val method = launcherRouteKtClass.getDeclaredMethod(methodName, Context::class.java, Intent::class.java)
        method.isAccessible = true
        return method.invoke(null, context, intent) as Boolean
    }

    private fun invokeContextToActivity(
        methodName: String,
        context: Context,
    ): Activity? {
        val method = launcherRouteKtClass.getDeclaredMethod(methodName, Context::class.java)
        method.isAccessible = true
        return method.invoke(null, context) as Activity?
    }

    private fun invokeList(
        methodName: String,
        context: Context,
    ): List<*> {
        val method = launcherRouteKtClass.getDeclaredMethod(methodName, Context::class.java)
        method.isAccessible = true
        return method.invoke(null, context) as List<*>
    }

    private fun invokeStringArray(
        methodName: String,
        context: Context,
    ): Array<String> {
        val method = launcherRouteKtClass.getDeclaredMethod(methodName, Context::class.java)
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return method.invoke(null, context) as Array<String>
    }

    private companion object {
        private val launcherRouteKtClass: Class<*> =
            Class.forName("com.darksok.canvaslauncher.feature.launcher.presentation.LauncherRouteKt")
    }
}
