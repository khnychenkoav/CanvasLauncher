package com.darksok.canvaslauncher.feature.canvas

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LabelVisibilityPolicyTest {

    @Test
    fun `stays visible until scale crosses hide threshold`() {
        val stillVisible = LabelVisibilityPolicy.nextVisibility(
            previousVisible = true,
            scale = 0.58f,
        )

        assertThat(stillVisible).isTrue()
    }

    @Test
    fun `hides below hide threshold`() {
        val hidden = LabelVisibilityPolicy.nextVisibility(
            previousVisible = true,
            scale = 0.55f,
        )

        assertThat(hidden).isFalse()
    }

    @Test
    fun `shows only above show threshold`() {
        val stillHidden = LabelVisibilityPolicy.nextVisibility(
            previousVisible = false,
            scale = 0.62f,
        )
        val visible = LabelVisibilityPolicy.nextVisibility(
            previousVisible = false,
            scale = 0.66f,
        )

        assertThat(stillHidden).isFalse()
        assertThat(visible).isTrue()
    }
}
