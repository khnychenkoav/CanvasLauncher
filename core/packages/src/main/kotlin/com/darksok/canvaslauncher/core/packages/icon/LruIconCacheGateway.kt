package com.darksok.canvaslauncher.core.packages.icon

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.LruCache
import com.darksok.canvaslauncher.core.common.coroutines.DispatchersProvider
import com.darksok.canvaslauncher.core.model.canvas.CanvasConstants
import com.darksok.canvaslauncher.domain.repository.IconCacheGateway
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

@Singleton
class LruIconCacheGateway @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val packageManager: PackageManager,
    private val dispatchersProvider: DispatchersProvider,
) : IconCacheGateway, IconBitmapStore {

    private val memoryCache = object : LruCache<String, Bitmap>(CanvasConstants.Icon.CACHE_MAX_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }
    private val cacheLock = Any()
    private val inFlightLoads = ConcurrentHashMap.newKeySet<String>()

    private val iconMapFlow = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    override val icons: StateFlow<Map<String, Bitmap>> = iconMapFlow.asStateFlow()

    private val diskCacheDir: File by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        File(appContext.cacheDir, DISK_CACHE_DIR).apply { mkdirs() }
    }

    override fun getCached(packageName: String): Bitmap? = synchronized(cacheLock) {
        memoryCache.get(packageName)
    }

    override suspend fun preload(packageNames: Collection<String>) {
        val targets = packageNames
            .asSequence()
            .distinct()
            .filter { packageName ->
                getCached(packageName) == null && inFlightLoads.add(packageName)
            }
            .toList()
        if (targets.isEmpty()) return

        try {
            withContext(dispatchersProvider.io) {
                targets.chunked(PRELOAD_BATCH_SIZE).forEach { batch ->
                    if (!isActive) return@withContext
                    val loaded = loadBatch(batch)
                    cacheLoadedBatch(loaded)
                    persistBatchToDiskIfMissing(loaded)
                }
            }
        } finally {
            targets.forEach { packageName ->
                inFlightLoads.remove(packageName)
            }
        }
    }

    override suspend fun invalidate(packageName: String) {
        remove(packageName)
    }

    override suspend fun remove(packageName: String) {
        withContext(dispatchersProvider.default) {
            synchronized(cacheLock) {
                memoryCache.remove(packageName)
            }
            iconMapFlow.update { current -> current - packageName }
        }
        withContext(dispatchersProvider.io) {
            iconFile(packageName).delete()
        }
    }

    private suspend fun cacheLoadedBatch(newEntries: Map<String, Bitmap>) {
        if (newEntries.isEmpty()) return
        withContext(dispatchersProvider.default) {
            synchronized(cacheLock) {
                newEntries.forEach { (packageName, bitmap) ->
                    if (memoryCache.get(packageName) == null) {
                        memoryCache.put(packageName, bitmap)
                    }
                }
            }
            val merged = HashMap<String, Bitmap>(iconMapFlow.value.size + newEntries.size)
            merged.putAll(iconMapFlow.value)
            newEntries.forEach { (packageName, bitmap) ->
                merged[packageName] = synchronized(cacheLock) { memoryCache.get(packageName) } ?: bitmap
            }
            iconMapFlow.value = merged
        }
    }

    private fun loadBitmap(packageName: String): Bitmap? {
        synchronized(cacheLock) {
            memoryCache.get(packageName)?.let { return it }
        }

        loadFromDisk(packageName)?.let { return it }

        val drawable = resolvePreferredIcon(packageName)
            ?: return null
        return drawableToBitmap(drawable, CanvasConstants.Icon.CACHE_BITMAP_SIZE_PX)
    }

    private fun resolvePreferredIcon(packageName: String): Drawable? {
        val launcherActivityIcon = runCatching {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return@runCatching null
            val component = launchIntent.component ?: return@runCatching null
            packageManager.getActivityIcon(component)
        }.getOrNull()
        if (launcherActivityIcon != null) return launcherActivityIcon

        return try {
            packageManager.getApplicationIcon(packageName)
        } catch (_: NameNotFoundException) {
            null
        } catch (_: Throwable) {
            null
        }
    }

    private suspend fun loadBatch(packageNames: List<String>): Map<String, Bitmap> {
        if (packageNames.isEmpty()) return emptyMap()
        val result = ConcurrentHashMap<String, Bitmap>()
        val semaphore = Semaphore(PRELOAD_PARALLELISM)
        coroutineScope {
            packageNames.map { packageName ->
                async {
                    semaphore.withPermit {
                        loadBitmap(packageName)?.let { bitmap ->
                            result[packageName] = bitmap
                        }
                    }
                }
            }.awaitAll()
        }
        return result.toMap()
    }

    private fun loadFromDisk(packageName: String): Bitmap? {
        val file = iconFile(packageName)
        if (!file.exists()) return null
        return runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
    }

    private fun saveToDisk(packageName: String, bitmap: Bitmap) {
        val file = iconFile(packageName)
        runCatching {
            file.outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, output)
            }
        }
    }

    private fun persistBatchToDiskIfMissing(batch: Map<String, Bitmap>) {
        if (batch.isEmpty()) return
        batch.forEach { (packageName, bitmap) ->
            val file = iconFile(packageName)
            if (!file.exists()) {
                saveToDisk(packageName, bitmap)
            }
        }
    }

    private fun iconFile(packageName: String): File {
        return File(diskCacheDir, "$packageName.png")
    }

    private fun drawableToBitmap(
        drawable: Drawable,
        sizePx: Int,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val drawableWidth = drawable.intrinsicWidth.takeIf { it > 0 } ?: sizePx
        val drawableHeight = drawable.intrinsicHeight.takeIf { it > 0 } ?: sizePx
        val scale = minOf(
            sizePx.toFloat() / drawableWidth.toFloat(),
            sizePx.toFloat() / drawableHeight.toFloat(),
        )
        val drawWidth = (drawableWidth * scale).toInt().coerceAtLeast(1)
        val drawHeight = (drawableHeight * scale).toInt().coerceAtLeast(1)
        val left = (sizePx - drawWidth) / 2
        val top = (sizePx - drawHeight) / 2
        drawable.setBounds(left, top, left + drawWidth, top + drawHeight)
        drawable.draw(canvas)
        return normalizeBitmap(bitmap, sizePx)
    }

    private fun normalizeBitmap(
        source: Bitmap,
        sizePx: Int,
    ): Bitmap {
        val bounds = findVisibleBounds(source) ?: return source
        val alphaBounds = bounds.primary ?: bounds.fallback
        val sourceWidth = alphaBounds.width().coerceAtLeast(1)
        val sourceHeight = alphaBounds.height().coerceAtLeast(1)
        val insetRatio = minOf(
            sourceWidth.toFloat() / source.width.toFloat(),
            sourceHeight.toFloat() / source.height.toFloat(),
        )
        val targetOccupancy = when {
            insetRatio < 0.78f -> ICON_TARGET_OCCUPANCY_HEAVY_INSET
            insetRatio < 0.86f -> ICON_TARGET_OCCUPANCY_MEDIUM_INSET
            insetRatio < 0.94f -> ICON_TARGET_OCCUPANCY_LIGHT_INSET
            else -> ICON_TARGET_OCCUPANCY_FULL
        }
        val targetSizePx = (sizePx * targetOccupancy).toInt().coerceAtLeast(1)
        val scale = minOf(
            targetSizePx.toFloat() / sourceWidth.toFloat(),
            targetSizePx.toFloat() / sourceHeight.toFloat(),
        )
        val drawWidth = (sourceWidth * scale).toInt().coerceAtLeast(1)
        val drawHeight = (sourceHeight * scale).toInt().coerceAtLeast(1)
        val left = (sizePx - drawWidth) / 2
        val top = (sizePx - drawHeight) / 2
        if (alphaBounds.left == 0 &&
            alphaBounds.top == 0 &&
            alphaBounds.right == source.width &&
            alphaBounds.bottom == source.height &&
            drawWidth == source.width &&
            drawHeight == source.height
        ) {
            return source
        }
        val normalized = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(normalized)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val destination = Rect(left, top, left + drawWidth, top + drawHeight)
        canvas.drawBitmap(source, alphaBounds, destination, paint)
        val edgeFillColor = sampleEdgeFillColor(source, alphaBounds)
        return ensureOpaqueFill(normalized, edgeFillColor)
    }

    private fun ensureOpaqueFill(
        bitmap: Bitmap,
        fillColor: Int?,
    ): Bitmap {
        if (fillColor == null) return bitmap
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return bitmap

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var transparentCount = 0

        for (pixel in pixels) {
            val alpha = (pixel ushr 24) and 0xFF
            if (alpha < ALPHA_FILL_TRANSPARENT_THRESHOLD) {
                transparentCount++
            }
        }
        val transparentRatio = transparentCount.toFloat() / pixels.size.toFloat()
        if (transparentRatio < MIN_TRANSPARENT_RATIO_FOR_FILL) return bitmap

        val filled = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(filled)
        canvas.drawColor(fillColor)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        return filled
    }

    private fun sampleEdgeFillColor(
        bitmap: Bitmap,
        bounds: Rect,
    ): Int? {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return null

        val left = bounds.left.coerceIn(0, width - 1)
        val top = bounds.top.coerceIn(0, height - 1)
        val right = (bounds.right - 1).coerceIn(0, width - 1)
        val bottom = (bounds.bottom - 1).coerceIn(0, height - 1)
        if (right < left || bottom < top) return null

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        fun averagePerimeterColor(alphaThreshold: Int): Int? {
            var redSum = 0L
            var greenSum = 0L
            var blueSum = 0L
            var sampleCount = 0

            forEachPerimeterPixel(left, top, right, bottom) { x, y ->
                val pixel = pixels[y * width + x]
                val alpha = (pixel ushr 24) and 0xFF
                if (alpha > alphaThreshold) {
                    redSum += (pixel ushr 16) and 0xFF
                    greenSum += (pixel ushr 8) and 0xFF
                    blueSum += pixel and 0xFF
                    sampleCount++
                }
            }

            if (sampleCount == 0) return null
            val red = (redSum / sampleCount).toInt().coerceIn(0, 255)
            val green = (greenSum / sampleCount).toInt().coerceIn(0, 255)
            val blue = (blueSum / sampleCount).toInt().coerceIn(0, 255)
            return (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
        }

        return averagePerimeterColor(ALPHA_EDGE_COLOR_THRESHOLD)
            ?: averagePerimeterColor(ALPHA_VISIBLE_FALLBACK_THRESHOLD)
    }

    private inline fun forEachPerimeterPixel(
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        block: (x: Int, y: Int) -> Unit,
    ) {
        for (x in left..right) {
            block(x, top)
            if (bottom != top) block(x, bottom)
        }
        if (bottom - top <= 1) return
        for (y in (top + 1) until bottom) {
            block(left, y)
            if (right != left) block(right, y)
        }
    }

    private fun findVisibleBounds(bitmap: Bitmap): VisibleBounds? {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return null

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var primaryMinX = width
        var primaryMinY = height
        var primaryMaxX = -1
        var primaryMaxY = -1
        var fallbackMinX = width
        var fallbackMinY = height
        var fallbackMaxX = -1
        var fallbackMaxY = -1

        var index = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                val alpha = (pixels[index] ushr 24) and 0xFF
                if (alpha > ALPHA_VISIBLE_FALLBACK_THRESHOLD) {
                    if (x < fallbackMinX) fallbackMinX = x
                    if (y < fallbackMinY) fallbackMinY = y
                    if (x > fallbackMaxX) fallbackMaxX = x
                    if (y > fallbackMaxY) fallbackMaxY = y
                }
                if (alpha > ALPHA_VISIBLE_PRIMARY_THRESHOLD) {
                    if (x < primaryMinX) primaryMinX = x
                    if (y < primaryMinY) primaryMinY = y
                    if (x > primaryMaxX) primaryMaxX = x
                    if (y > primaryMaxY) primaryMaxY = y
                }
                index++
            }
        }

        if (fallbackMaxX < fallbackMinX || fallbackMaxY < fallbackMinY) return null
        val fallback = Rect(fallbackMinX, fallbackMinY, fallbackMaxX + 1, fallbackMaxY + 1)
        val primary = if (primaryMaxX < primaryMinX || primaryMaxY < primaryMinY) {
            null
        } else {
            Rect(primaryMinX, primaryMinY, primaryMaxX + 1, primaryMaxY + 1)
        }
        return VisibleBounds(primary = primary, fallback = fallback)
    }

    private companion object {
        const val DISK_CACHE_DIR = "icon_cache_v7"
        const val PNG_QUALITY = 100
        const val PRELOAD_PARALLELISM = 4
        const val PRELOAD_BATCH_SIZE = 20
        const val ICON_TARGET_OCCUPANCY_FULL = 1.04f
        const val ICON_TARGET_OCCUPANCY_LIGHT_INSET = 1.10f
        const val ICON_TARGET_OCCUPANCY_MEDIUM_INSET = 1.16f
        const val ICON_TARGET_OCCUPANCY_HEAVY_INSET = 1.22f
        const val ALPHA_VISIBLE_PRIMARY_THRESHOLD = 84
        const val ALPHA_VISIBLE_FALLBACK_THRESHOLD = 12
        const val ALPHA_EDGE_COLOR_THRESHOLD = 160
        const val ALPHA_FILL_TRANSPARENT_THRESHOLD = 10
        const val MIN_TRANSPARENT_RATIO_FOR_FILL = 0.08f
    }
}

private data class VisibleBounds(
    val primary: Rect?,
    val fallback: Rect,
)
