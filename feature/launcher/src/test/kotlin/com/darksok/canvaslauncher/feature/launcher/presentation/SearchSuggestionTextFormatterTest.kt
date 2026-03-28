package com.darksok.canvaslauncher.feature.launcher.presentation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SearchSuggestionTextFormatterTest {

    @Test
    fun `blank query returns null`() {
        val result = SearchSuggestionTextFormatter.build(
            query = " ",
            suggestion = "Calendar",
            highlightColor = Color.Red,
        )

        assertThat(result).isNull()
    }

    @Test
    fun `non prefix suggestion returns null`() {
        val result = SearchSuggestionTextFormatter.build(
            query = "len",
            suggestion = "Calendar",
            highlightColor = Color.Red,
        )

        assertThat(result).isNull()
    }

    @Test
    fun `exact match returns null because there is no suffix to highlight`() {
        val result = SearchSuggestionTextFormatter.build(
            query = "calendar",
            suggestion = "Calendar",
            highlightColor = Color.Red,
        )

        assertThat(result).isNull()
    }

    @Test
    fun `prefix suggestion highlights only suffix`() {
        val highlight = Color(0xFF2266CC)
        val result = SearchSuggestionTextFormatter.build(
            query = "cal",
            suggestion = "Calendar",
            highlightColor = highlight,
        )

        checkNotNull(result)
        assertThat(result.text).isEqualTo("calendar")
        assertThat(result.spanStyles).hasSize(1)
        val span = result.spanStyles.first()
        assertThat(span.start).isEqualTo(3)
        assertThat(span.end).isEqualTo("calendar".length)
        assertThat(span.item.color).isEqualTo(highlight)
        assertThat(span.item.fontWeight).isEqualTo(FontWeight.SemiBold)
    }
}
