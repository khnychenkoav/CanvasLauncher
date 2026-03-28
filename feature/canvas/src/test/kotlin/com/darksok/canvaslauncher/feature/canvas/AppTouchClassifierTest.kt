package com.darksok.canvaslauncher.feature.canvas

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppTouchClassifierTest {

    @Test
    fun `single pointer with small move triggers tap`() {
        val shouldTap = AppTouchClassifier.shouldTriggerTap(
            maxPointerCount = 1,
            totalMoveDistancePx = 6f,
            touchSlopPx = 12f,
            isTapSuppressed = false,
        )

        assertThat(shouldTap).isTrue()
    }

    @Test
    fun `single pointer drag does not trigger tap`() {
        val shouldTap = AppTouchClassifier.shouldTriggerTap(
            maxPointerCount = 1,
            totalMoveDistancePx = 18f,
            touchSlopPx = 12f,
            isTapSuppressed = false,
        )

        assertThat(shouldTap).isFalse()
    }

    @Test
    fun `multi touch never triggers tap`() {
        val shouldTap = AppTouchClassifier.shouldTriggerTap(
            maxPointerCount = 2,
            totalMoveDistancePx = 0f,
            touchSlopPx = 12f,
            isTapSuppressed = false,
        )

        assertThat(shouldTap).isFalse()
    }

    @Test
    fun `suppressed tap never triggers`() {
        val shouldTap = AppTouchClassifier.shouldTriggerTap(
            maxPointerCount = 1,
            totalMoveDistancePx = 1f,
            touchSlopPx = 12f,
            isTapSuppressed = true,
        )

        assertThat(shouldTap).isFalse()
    }
}
