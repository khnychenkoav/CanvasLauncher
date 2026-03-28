package com.darksok.canvaslauncher.defaultscreen

import android.os.Build

internal enum class DefaultLauncherSetupAction {
    REQUEST_ROLE,
    OPEN_HOME_SETTINGS,
}

internal object DefaultLauncherSetupPlanner {

    fun decideAction(
        sdkInt: Int,
        roleManagerAvailable: Boolean,
        roleHeld: Boolean,
    ): DefaultLauncherSetupAction {
        val canRequestRole = sdkInt >= Build.VERSION_CODES.Q &&
            roleManagerAvailable &&
            !roleHeld
        return if (canRequestRole) {
            DefaultLauncherSetupAction.REQUEST_ROLE
        } else {
            DefaultLauncherSetupAction.OPEN_HOME_SETTINGS
        }
    }
}
