package com.darksok.canvaslauncher.core.common.result

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppResultTest {

    @Test
    fun `map transforms success integer to string`() {
        val result = AppResult.Success(42)
        val mapped = result.map { value -> "value-$value" }
        assertThat(mapped).isEqualTo(AppResult.Success("value-42"))
    }

    @Test
    fun `map transforms success to null payload`() {
        val result = AppResult.Success("input")
        val mapped = result.map<String, String?> { null }
        assertThat(mapped).isEqualTo(AppResult.Success(null))
    }

    @Test
    fun `map transforms success to collection`() {
        val result = AppResult.Success(listOf(1, 2, 3))
        val mapped = result.map { values -> values.map { it * 2 } }
        assertThat(mapped).isEqualTo(AppResult.Success(listOf(2, 4, 6)))
    }

    @Test
    fun `map keeps success wrapper when transform returns same reference`() {
        val payload = mutableListOf("a", "b")
        val result = AppResult.Success(payload)
        val mapped = result.map { it }
        assertThat((mapped as AppResult.Success).value).isSameInstanceAs(payload)
    }

    @Test
    fun `map on failure returns same instance for not found`() {
        val failure = AppResult.Failure(AppError.NotFound)
        val mapped = failure.map { "ignored" }
        assertThat(mapped).isSameInstanceAs(failure)
    }

    @Test
    fun `map on failure returns same instance for launch unavailable`() {
        val failure = AppResult.Failure(AppError.LaunchUnavailable)
        val mapped = failure.map { "ignored" }
        assertThat(mapped).isSameInstanceAs(failure)
    }

    @Test
    fun `map on failure returns same instance for unknown error`() {
        val failure = AppResult.Failure(AppError.Unknown("boom"))
        val mapped = failure.map { "ignored" }
        assertThat(mapped).isSameInstanceAs(failure)
    }

    @Test
    fun `map on failure does not invoke transform`() {
        val failure = AppResult.Failure(AppError.NotFound)
        var invoked = false
        failure.map {
            invoked = true
            it.toString()
        }
        assertThat(invoked).isFalse()
    }

    @Test
    fun `map allows empty string transformation`() {
        val result = AppResult.Success("CanvasLauncher")
        val mapped = result.map { "" }
        assertThat(mapped).isEqualTo(AppResult.Success(""))
    }

    @Test
    fun `map supports boolean false result`() {
        val result = AppResult.Success("CanvasLauncher")
        val mapped = result.map { false }
        assertThat(mapped).isEqualTo(AppResult.Success(false))
    }

    @Test
    fun `map supports unit result`() {
        val result = AppResult.Success("CanvasLauncher")
        val mapped = result.map { Unit }
        assertThat(mapped).isEqualTo(AppResult.Success(Unit))
    }

    @Test
    fun `map can convert to nested app result`() {
        val result = AppResult.Success("payload")
        val mapped = result.map { value -> AppResult.Success(value.length) }
        assertThat(mapped).isEqualTo(AppResult.Success(AppResult.Success(7)))
    }

    @Test
    fun `map can be chained on success`() {
        val result = AppResult.Success(3)
        val mapped = result.map { it + 1 }.map { it * 2 }.map { "size=$it" }
        assertThat(mapped).isEqualTo(AppResult.Success("size=8"))
    }

    @Test
    fun `map short circuits chained operations after failure`() {
        val failure = AppResult.Failure(AppError.NotFound)
        var secondInvoked = false
        val mapped = failure.map { it.toString() }.map {
            secondInvoked = true
            it.length
        }
        assertThat(mapped).isSameInstanceAs(failure)
        assertThat(secondInvoked).isFalse()
    }

    @Test(expected = IllegalStateException::class)
    fun `map propagates transform exception`() {
        AppResult.Success("input").map<String, String> { error("transform failure") }
    }

    @Test
    fun `failure preserves unknown message`() {
        val failure = AppResult.Failure(AppError.Unknown("network"))
        val mapped = failure.map { 1 }
        assertThat((mapped as AppResult.Failure).error).isEqualTo(AppError.Unknown("network"))
    }

    @Test
    fun `failure preserves null unknown message`() {
        val failure = AppResult.Failure(AppError.Unknown())
        val mapped = failure.map { 1 }
        assertThat((mapped as AppResult.Failure).error).isEqualTo(AppError.Unknown(null))
    }

    @Test
    fun `success equality remains data based after mapping`() {
        val mapped = AppResult.Success(5).map { it * it }
        assertThat(mapped).isEqualTo(AppResult.Success(25))
        assertThat(mapped).isNotSameInstanceAs(AppResult.Success(25))
    }

    @Test
    fun `map supports empty list payload`() {
        val result = AppResult.Success(emptyList<Int>())
        val mapped = result.map { values -> values.sum() }
        assertThat(mapped).isEqualTo(AppResult.Success(0))
    }

    @Test
    fun `map supports negative numeric transformation`() {
        val result = AppResult.Success(-3)
        val mapped = result.map { value -> value * 10 }
        assertThat(mapped).isEqualTo(AppResult.Success(-30))
    }

    @Test
    fun `map supports long string normalization`() {
        val result = AppResult.Success("  Canvas Launcher  ")
        val mapped = result.map { value -> value.trim().lowercase() }
        assertThat(mapped).isEqualTo(AppResult.Success("canvas launcher"))
    }

    @Test
    fun `map can transform success object into derived property`() {
        val result = AppResult.Success(SamplePayload(name = "Canvas", version = 7))
        val mapped = result.map { payload -> payload.version }
        assertThat(mapped).isEqualTo(AppResult.Success(7))
    }

    @Test
    fun `map can transform success object into composed string`() {
        val result = AppResult.Success(SamplePayload(name = "Canvas", version = 7))
        val mapped = result.map { payload -> "${payload.name}-${payload.version}" }
        assertThat(mapped).isEqualTo(AppResult.Success("Canvas-7"))
    }

    private data class SamplePayload(
        val name: String,
        val version: Int,
    )
}
