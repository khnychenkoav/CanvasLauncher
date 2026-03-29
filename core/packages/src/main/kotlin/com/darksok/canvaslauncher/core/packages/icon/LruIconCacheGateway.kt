package com.darksok.canvaslauncher.core.packages.icon

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
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

        val drawable = runCatching { packageManager.getApplicationIcon(packageName) }.getOrNull()
            ?: return null
        return drawableToBitmap(drawable, CanvasConstants.Icon.CACHE_BITMAP_SIZE_PX)
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
        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(canvas)
        return bitmap
    }

    private companion object {
        const val DISK_CACHE_DIR = "icon_cache_v1"
        const val PNG_QUALITY = 100
        const val PRELOAD_PARALLELISM = 4
        const val PRELOAD_BATCH_SIZE = 20
    }
}
