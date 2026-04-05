package com.darksok.canvaslauncher.core.packages.icon

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

class LruIconCacheGatewayContractTest {

    @Test
    fun `gateway uses lru cache sized by icon cache bytes`() {
        assertThat(source()).contains("LruCache<String, Bitmap>(CanvasConstants.Icon.CACHE_MAX_BYTES)")
    }

    @Test
    fun `gateway reports bitmap byte count to lru cache`() {
        assertThat(source()).contains("override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount")
    }

    @Test
    fun `preload deduplicates package names`() {
        assertThat(source()).contains("asSequence()")
        assertThat(source()).contains("distinct()")
    }

    @Test
    fun `preload skips packages already cached`() {
        assertThat(source()).contains("getCached(packageName) == null")
    }

    @Test
    fun `preload tracks in flight loads`() {
        assertThat(source()).contains("inFlightLoads.add(packageName)")
        assertThat(source()).contains("inFlightLoads.remove(packageName)")
    }

    @Test
    fun `preload processes targets in chunks`() {
        assertThat(source()).contains("targets.chunked(PRELOAD_BATCH_SIZE)")
    }

    @Test
    fun `load bitmap checks memory cache before disk`() {
        val source = source()

        assertThat(source.indexOf("memoryCache.get(packageName)?.let { return it }")).isLessThan(source.indexOf("loadFromDisk(packageName)?.let { return it }"))
    }

    @Test
    fun `persist batch writes only missing files`() {
        assertThat(source()).contains("if (!file.exists()) {")
        assertThat(source()).contains("saveToDisk(packageName, bitmap)")
    }

    @Test
    fun `remove deletes memory entry and disk file`() {
        assertThat(source()).contains("memoryCache.remove(packageName)")
        assertThat(source()).contains("iconFile(packageName).delete()")
    }

    @Test
    fun `icon file appends png suffix`() {
        assertThat(source()).contains("File(diskCacheDir, \"\$packageName.png\")")
    }

    @Test
    fun `gateway defines expected disk cache directory and batch constants`() {
        val source = source()

        assertThat(source).contains("const val DISK_CACHE_DIR = \"icon_cache_v1\"")
        assertThat(source).contains("const val PRELOAD_PARALLELISM = 4")
        assertThat(source).contains("const val PRELOAD_BATCH_SIZE = 20")
    }

    @Test
    fun `bitmap store contract exposes state flow and cached lookup`() {
        val source = readSource("src/main/kotlin/com/darksok/canvaslauncher/core/packages/icon/IconBitmapStore.kt")

        assertThat(source).contains("val icons: StateFlow<Map<String, Bitmap>>")
        assertThat(source).contains("fun getCached(packageName: String): Bitmap?")
    }

    private fun source(): String = readSource("src/main/kotlin/com/darksok/canvaslauncher/core/packages/icon/LruIconCacheGateway.kt")

    private fun readSource(relativePath: String): String {
        val moduleFile = File(relativePath)
        val rootFile = File("core/packages/$relativePath")
        val file = when {
            moduleFile.exists() -> moduleFile
            rootFile.exists() -> rootFile
            else -> error("Missing source file for test: $relativePath")
        }
        return file.readText()
    }
}
