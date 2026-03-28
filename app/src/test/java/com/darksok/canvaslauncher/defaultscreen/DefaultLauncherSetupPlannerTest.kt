package com.darksok.canvaslauncher.defaultscreen

import android.os.Build
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DefaultLauncherSetupPlannerTest {

    @Test
    fun `requests role on android q and newer when role is available and not held`() {
        val action = DefaultLauncherSetupPlanner.decideAction(
            sdkInt = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
            roleManagerAvailable = true,
            roleHeld = false,
        )

        assertThat(action).isEqualTo(DefaultLauncherSetupAction.REQUEST_ROLE)
    }

    @Test
    fun `opens settings when role is already held`() {
        val action = DefaultLauncherSetupPlanner.decideAction(
            sdkInt = Build.VERSION_CODES.TIRAMISU,
            roleManagerAvailable = true,
            roleHeld = true,
        )

        assertThat(action).isEqualTo(DefaultLauncherSetupAction.OPEN_HOME_SETTINGS)
    }

    @Test
    fun `opens settings on pre q devices`() {
        val action = DefaultLauncherSetupPlanner.decideAction(
            sdkInt = Build.VERSION_CODES.P,
            roleManagerAvailable = true,
            roleHeld = false,
        )

        assertThat(action).isEqualTo(DefaultLauncherSetupAction.OPEN_HOME_SETTINGS)
    }
}
