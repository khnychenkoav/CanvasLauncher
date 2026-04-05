package com.darksok.canvaslauncher.feature.apps.data

import com.darksok.canvaslauncher.core.common.coroutines.DispatchersProvider
import com.darksok.canvaslauncher.core.database.dao.AppDao
import com.darksok.canvaslauncher.core.database.entity.AppEntity
import com.darksok.canvaslauncher.core.model.app.CanvasApp
import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RoomCanvasAppsStoreTest {

    @Test
    fun `observe apps maps entities to domain models`() = runTest {
        val dao = FakeAppDao(
            initial = listOf(
                AppEntity("pkg.one", "One", 1f, 2f),
                AppEntity("pkg.two", "Two", 3f, 4f),
            ),
        )
        val store = RoomCanvasAppsStore(dao, ImmediateDispatchersProvider)

        val observed = store.observeApps().first()

        assertThat(observed).containsExactly(
            CanvasApp("pkg.one", "One", WorldPoint(1f, 2f)),
            CanvasApp("pkg.two", "Two", WorldPoint(3f, 4f)),
        ).inOrder()
    }

    @Test
    fun `observe apps emits empty list when dao empty`() = runTest {
        val store = RoomCanvasAppsStore(FakeAppDao(), ImmediateDispatchersProvider)

        assertThat(store.observeApps().first()).isEmpty()
    }

    @Test
    fun `get apps snapshot maps entities to domain models`() = runTest {
        val dao = FakeAppDao(initial = listOf(AppEntity("pkg", "Label", 5f, 6f)))
        val store = RoomCanvasAppsStore(dao, ImmediateDispatchersProvider)

        assertThat(store.getAppsSnapshot()).containsExactly(CanvasApp("pkg", "Label", WorldPoint(5f, 6f)))
    }

    @Test
    fun `get apps snapshot returns empty list when dao empty`() = runTest {
        val store = RoomCanvasAppsStore(FakeAppDao(), ImmediateDispatchersProvider)

        assertThat(store.getAppsSnapshot()).isEmpty()
    }

    @Test
    fun `upsert apps stores all mapped entities`() = runTest {
        val dao = FakeAppDao()
        val store = RoomCanvasAppsStore(dao, ImmediateDispatchersProvider)
        val apps = listOf(
            CanvasApp("pkg.one", "One", WorldPoint(1f, 2f)),
            CanvasApp("pkg.two", "Two", WorldPoint(3f, 4f)),
        )

        store.upsertApps(apps)

        assertThat(dao.entities()).containsExactly(
            AppEntity("pkg.one", "One", 1f, 2f),
            AppEntity("pkg.two", "Two", 3f, 4f),
        )
        assertThat(dao.lastUpsertBatchSize).isEqualTo(2)
    }

    @Test
    fun `upsert apps with empty list does not call dao`() = runTest {
        val dao = FakeAppDao()
        val store = RoomCanvasAppsStore(dao, ImmediateDispatchersProvider)

        store.upsertApps(emptyList())

        assertThat(dao.batchUpsertCalls).isEqualTo(0)
    }

    @Test
    fun `upsert apps overwrites existing packages with latest values`() = runTest {
        val dao = FakeAppDao(initial = listOf(AppEntity("pkg", "Old", 1f, 1f)))
        val store = RoomCanvasAppsStore(dao, ImmediateDispatchersProvider)

        store.upsertApps(listOf(CanvasApp("pkg", "New", WorldPoint(8f, 9f))))

        assertThat(dao.entities()).containsExactly(AppEntity("pkg", "New", 8f, 9f))
    }

    @Test
    fun `upsert app stores mapped entity`() = runTest {
        val dao = FakeAppDao()
        val store = RoomCanvasAppsStore(dao, ImmediateDispatchersProvider)

        store.upsertApp(CanvasApp("pkg.single", "Single", WorldPoint(7f, 8f)))

        assertThat(dao.entities()).containsExactly(AppEntity("pkg.single", "Single", 7f, 8f))
        assertThat(dao.singleUpsertCalls).isEqualTo(1)
    }

    @Test
    fun `upsert app overwrites existing package`() = runTest {
        val dao = FakeAppDao(initial = listOf(AppEntity("pkg", "Old", 0f, 0f)))
        val store = RoomCanvasAppsStore(dao, ImmediateDispatchersProvider)

        store.upsertApp(CanvasApp("pkg", "Updated", WorldPoint(-1f, -2f)))

        assertThat(dao.entities()).containsExactly(AppEntity("pkg", "Updated", -1f, -2f))
    }

    @Test
    fun `remove packages deletes all requested package names`() = runTest {
        val dao = FakeAppDao(
            initial = listOf(
                AppEntity("keep", "Keep", 0f, 0f),
                AppEntity("drop.one", "One", 1f, 1f),
                AppEntity("drop.two", "Two", 2f, 2f),
            ),
        )
        val store = RoomCanvasAppsStore(dao, ImmediateDispatchersProvider)

        store.removePackages(setOf("drop.one", "drop.two"))

        assertThat(dao.entities()).containsExactly(AppEntity("keep", "Keep", 0f, 0f))
    }

    @Test
    fun `remove packages with empty set does not call dao`() = runTest {
        val dao = FakeAppDao(initial = listOf(AppEntity("keep", "Keep", 0f, 0f)))
        val store = RoomCanvasAppsStore(dao, ImmediateDispatchersProvider)

        store.removePackages(emptySet())

        assertThat(dao.deleteManyCalls).isEqualTo(0)
        assertThat(dao.entities()).hasSize(1)
    }

    @Test
    fun `remove packages deduplicates through set semantics`() = runTest {
        val dao = FakeAppDao(initial = listOf(AppEntity("drop", "Drop", 0f, 0f)))
        val store = RoomCanvasAppsStore(dao, ImmediateDispatchersProvider)

        store.removePackages(linkedSetOf("drop", "drop"))

        assertThat(dao.lastDeletedPackages).containsExactly("drop")
    }

    @Test
    fun `remove package deletes requested package name`() = runTest {
        val dao = FakeAppDao(
            initial = listOf(
                AppEntity("keep", "Keep", 0f, 0f),
                AppEntity("drop", "Drop", 0f, 0f),
            ),
        )
        val store = RoomCanvasAppsStore(dao, ImmediateDispatchersProvider)

        store.removePackage("drop")

        assertThat(dao.entities()).containsExactly(AppEntity("keep", "Keep", 0f, 0f))
    }

    @Test
    fun `remove package records requested name even when absent`() = runTest {
        val dao = FakeAppDao(initial = listOf(AppEntity("keep", "Keep", 0f, 0f)))
        val store = RoomCanvasAppsStore(dao, ImmediateDispatchersProvider)

        store.removePackage("missing")

        assertThat(dao.lastDeletedPackage).isEqualTo("missing")
        assertThat(dao.entities()).containsExactly(AppEntity("keep", "Keep", 0f, 0f))
    }

    @Test
    fun `update position forwards x and y to dao`() = runTest {
        val dao = FakeAppDao(initial = listOf(AppEntity("pkg", "App", 1f, 2f)))
        val store = RoomCanvasAppsStore(dao, ImmediateDispatchersProvider)

        store.updatePosition("pkg", WorldPoint(-10f, 20f))

        assertThat(dao.lastUpdatedPackage).isEqualTo("pkg")
        assertThat(dao.lastUpdatedX).isEqualTo(-10f)
        assertThat(dao.lastUpdatedY).isEqualTo(20f)
    }

    @Test
    fun `update position mutates existing stored entity`() = runTest {
        val dao = FakeAppDao(initial = listOf(AppEntity("pkg", "App", 1f, 2f)))
        val store = RoomCanvasAppsStore(dao, ImmediateDispatchersProvider)

        store.updatePosition("pkg", WorldPoint(50f, 60f))

        assertThat(dao.entities()).containsExactly(AppEntity("pkg", "App", 50f, 60f))
    }

    @Test
    fun `update position leaves unrelated entities untouched`() = runTest {
        val dao = FakeAppDao(
            initial = listOf(
                AppEntity("target", "Target", 1f, 2f),
                AppEntity("other", "Other", 3f, 4f),
            ),
        )
        val store = RoomCanvasAppsStore(dao, ImmediateDispatchersProvider)

        store.updatePosition("target", WorldPoint(9f, 10f))

        assertThat(dao.entities()).containsExactly(
            AppEntity("target", "Target", 9f, 10f),
            AppEntity("other", "Other", 3f, 4f),
        )
    }

    @Test
    fun `update position on missing package does not create entity`() = runTest {
        val dao = FakeAppDao(initial = listOf(AppEntity("keep", "Keep", 1f, 2f)))
        val store = RoomCanvasAppsStore(dao, ImmediateDispatchersProvider)

        store.updatePosition("missing", WorldPoint(9f, 10f))

        assertThat(dao.entities()).containsExactly(AppEntity("keep", "Keep", 1f, 2f))
    }

    @Test
    fun `observe apps reflects later dao changes`() = runTest {
        val dao = FakeAppDao(initial = listOf(AppEntity("pkg", "One", 1f, 2f)))
        val store = RoomCanvasAppsStore(dao, ImmediateDispatchersProvider)
        store.upsertApp(CanvasApp("pkg.two", "Two", WorldPoint(3f, 4f)))

        assertThat(store.observeApps().first()).containsExactly(
            CanvasApp("pkg", "One", WorldPoint(1f, 2f)),
            CanvasApp("pkg.two", "Two", WorldPoint(3f, 4f)),
        )
    }

    @Test
    fun `get apps snapshot reflects removals`() = runTest {
        val dao = FakeAppDao(initial = listOf(AppEntity("drop", "Drop", 1f, 2f)))
        val store = RoomCanvasAppsStore(dao, ImmediateDispatchersProvider)
        store.removePackage("drop")

        assertThat(store.getAppsSnapshot()).isEmpty()
    }

    @Test
    fun `upsert apps preserves insertion order from dao state`() = runTest {
        val dao = FakeAppDao()
        val store = RoomCanvasAppsStore(dao, ImmediateDispatchersProvider)

        store.upsertApps(
            listOf(
                CanvasApp("first", "First", WorldPoint(1f, 1f)),
                CanvasApp("second", "Second", WorldPoint(2f, 2f)),
            ),
        )

        assertThat(store.getAppsSnapshot().map { it.packageName }).containsExactly("first", "second").inOrder()
    }

    @Test
    fun `upsert apps batch with duplicate packages keeps last value`() = runTest {
        val dao = FakeAppDao()
        val store = RoomCanvasAppsStore(dao, ImmediateDispatchersProvider)

        store.upsertApps(
            listOf(
                CanvasApp("dup", "First", WorldPoint(1f, 1f)),
                CanvasApp("dup", "Second", WorldPoint(2f, 2f)),
            ),
        )

        assertThat(dao.entities()).containsExactly(AppEntity("dup", "Second", 2f, 2f))
    }

    private object ImmediateDispatchersProvider : DispatchersProvider {
        override val io = Dispatchers.Unconfined
        override val default = Dispatchers.Unconfined
        override val main = Dispatchers.Unconfined
    }

    private class FakeAppDao(
        initial: List<AppEntity> = emptyList(),
    ) : AppDao {
        private val state = MutableStateFlow(initial)
        var batchUpsertCalls: Int = 0
        var singleUpsertCalls: Int = 0
        var deleteManyCalls: Int = 0
        var lastUpsertBatchSize: Int = 0
        var lastDeletedPackages: List<String> = emptyList()
        var lastDeletedPackage: String? = null
        var lastUpdatedPackage: String? = null
        var lastUpdatedX: Float? = null
        var lastUpdatedY: Float? = null

        override fun observeApps(): Flow<List<AppEntity>> = state

        override suspend fun getAppsOnce(): List<AppEntity> = state.value

        override suspend fun upsert(apps: List<AppEntity>) {
            batchUpsertCalls += 1
            lastUpsertBatchSize = apps.size
            val merged = LinkedHashMap(state.value.associateBy { it.packageName })
            apps.forEach { merged[it.packageName] = it }
            state.value = merged.values.toList()
        }

        override suspend fun upsert(app: AppEntity) {
            singleUpsertCalls += 1
            val merged = LinkedHashMap(state.value.associateBy { it.packageName })
            merged[app.packageName] = app
            state.value = merged.values.toList()
        }

        override suspend fun deleteByPackageNames(packageNames: List<String>) {
            deleteManyCalls += 1
            lastDeletedPackages = packageNames
            state.value = state.value.filterNot { it.packageName in packageNames.toSet() }
        }

        override suspend fun deleteByPackageName(packageName: String) {
            lastDeletedPackage = packageName
            state.value = state.value.filterNot { it.packageName == packageName }
        }

        override suspend fun updatePosition(packageName: String, x: Float, y: Float) {
            lastUpdatedPackage = packageName
            lastUpdatedX = x
            lastUpdatedY = y
            state.value = state.value.map { entity ->
                if (entity.packageName == packageName) entity.copy(x = x, y = y) else entity
            }
        }

        fun entities(): List<AppEntity> = state.value
    }
}
