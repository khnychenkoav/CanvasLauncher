package com.darksok.canvaslauncher.defaultscreen

import android.os.Build
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DefaultLauncherSetupPlannerTest {

    @Test
    fun `requests role on android q and newer when role is available and not held`() {
        assertThat(
            DefaultLauncherSetupPlanner.decideAction(
                sdkInt = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
                roleManagerAvailable = true,
                roleHeld = false,
            ),
        ).isEqualTo(DefaultLauncherSetupAction.REQUEST_ROLE)
    }

    @Test
    fun `requests role exactly on android q`() {
        assertThat(
            DefaultLauncherSetupPlanner.decideAction(
                sdkInt = Build.VERSION_CODES.Q,
                roleManagerAvailable = true,
                roleHeld = false,
            ),
        ).isEqualTo(DefaultLauncherSetupAction.REQUEST_ROLE)
    }

    @Test
    fun `opens settings when role is already held`() {
        assertThat(
            DefaultLauncherSetupPlanner.decideAction(
                sdkInt = Build.VERSION_CODES.TIRAMISU,
                roleManagerAvailable = true,
                roleHeld = true,
            ),
        ).isEqualTo(DefaultLauncherSetupAction.OPEN_HOME_SETTINGS)
    }

    @Test
    fun `opens settings when role manager is unavailable on supported sdk`() {
        assertThat(
            DefaultLauncherSetupPlanner.decideAction(
                sdkInt = Build.VERSION_CODES.TIRAMISU,
                roleManagerAvailable = false,
                roleHeld = false,
            ),
        ).isEqualTo(DefaultLauncherSetupAction.OPEN_HOME_SETTINGS)
    }

    @Test
    fun `opens settings on pre q devices`() {
        assertThat(
            DefaultLauncherSetupPlanner.decideAction(
                sdkInt = Build.VERSION_CODES.P,
                roleManagerAvailable = true,
                roleHeld = false,
            ),
        ).isEqualTo(DefaultLauncherSetupAction.OPEN_HOME_SETTINGS)
    }

    @Test
    fun `opens settings on pre q even if role manager is unavailable`() {
        assertThat(
            DefaultLauncherSetupPlanner.decideAction(
                sdkInt = Build.VERSION_CODES.P,
                roleManagerAvailable = false,
                roleHeld = false,
            ),
        ).isEqualTo(DefaultLauncherSetupAction.OPEN_HOME_SETTINGS)
    }

    @Test
    fun `opens settings when every condition is false`() {
        assertThat(
            DefaultLauncherSetupPlanner.decideAction(
                sdkInt = Int.MIN_VALUE,
                roleManagerAvailable = false,
                roleHeld = true,
            ),
        ).isEqualTo(DefaultLauncherSetupAction.OPEN_HOME_SETTINGS)
    }

    @Test
    fun `role held still wins over availability`() {
        assertThat(
            DefaultLauncherSetupPlanner.decideAction(
                sdkInt = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
                roleManagerAvailable = false,
                roleHeld = true,
            ),
        ).isEqualTo(DefaultLauncherSetupAction.OPEN_HOME_SETTINGS)
    }
}
