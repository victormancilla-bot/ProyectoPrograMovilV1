package bo.edu.ucb.encuentra.core.data

import bo.edu.ucb.encuentra.core.data.local.entity.FoundItemEntity
import bo.edu.ucb.encuentra.core.data.local.entity.LostReportEntity
import bo.edu.ucb.encuentra.core.data.local.mapper.toEntity
import bo.edu.ucb.encuentra.core.data.remote.dto.FoundItemDto
import bo.edu.ucb.encuentra.core.data.remote.dto.LostReportDto
import bo.edu.ucb.encuentra.core.data.remote.dto.toDto
import bo.edu.ucb.encuentra.core.data.remote.dto.toEntity
import bo.edu.ucb.encuentra.core.data.sync.FirestoreRemoteSync
import bo.edu.ucb.encuentra.core.data.sync.LocalTable
import bo.edu.ucb.encuentra.core.data.sync.RemoteCollection
import bo.edu.ucb.encuentra.core.data.sync.RemoteFilter
import bo.edu.ucb.encuentra.core.domain.model.ItemStatus
import bo.edu.ucb.encuentra.core.domain.model.ReportStatus
import bo.edu.ucb.encuentra.core.domain.model.Role
import bo.edu.ucb.encuentra.core.domain.model.UserModel
import bo.edu.ucb.encuentra.feature.login.FakeRemoteAuth
import bo.edu.ucb.encuentra.feature.matching.foundItem
import bo.edu.ucb.encuentra.feature.matching.lostReport
import bo.edu.ucb.encuentra.feature.myreports.mateo
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Tabla de Room de prueba: un StateFlow con upsert por id. */
internal class FakeTable<E>(initial: List<E>, private val idOf: (E) -> String) {
    val rows = MutableStateFlow(initial)
    val table = LocalTable(rows) { incoming -> upsert(incoming) }
    fun upsert(incoming: List<E>) {
        val byId = rows.value.associateBy(idOf).toMutableMap()
        incoming.forEach { byId[idOf(it)] = it }
        rows.value = byId.values.toList()
    }
    fun byId(id: String) = rows.value.firstOrNull { idOf(it) == id }
}

/**
 * Firestore de prueba. Como el real, una escritura se ve enseguida en el listener (caché local). Sin conexión
 * ([offline]) la escritura se queda suspendida para siempre; con [rejectAll] las reglas la rechazan y el documento
 * vuelve a su valor anterior.
 */
internal class FakeRemote<D>(initial: Map<String, D> = emptyMap(), private val fieldOf: (D, String) -> String? = { _, _ -> null }) :
    RemoteCollection<D> {
    val docs = MutableStateFlow(initial)
    val pushes = mutableListOf<String>()
    var offline = false
    var rejectAll = false
    var lastFilter: RemoteFilter? = null
    var observing = false

    override fun observe(filter: RemoteFilter?): Flow<Map<String, D>> {
        lastFilter = filter
        observing = true
        return docs.map { all -> all.filterValues { filter == null || fieldOf(it, filter.field) == filter.value } }
    }

    override suspend fun push(id: String, dto: D) {
        pushes += id
        val before = docs.value
        docs.value = before + (id to dto)
        if (offline) awaitCancellation()
        yield() // el servidor responde después
        if (rejectAll) {
            docs.value = before
            error("PERMISSION_DENIED")
        }
    }
}

private val estudiante = mateo.copy(id = "uid-mateo")
private val staff = mateo.copy(id = "uid-ana", role = Role.STAFF)

private fun report(id: String, owner: String, status: ReportStatus = ReportStatus.BUSCANDO) =
    lostReport(id = id, status = status).toEntity().copy(ownerId = owner)

private fun item(id: String, by: String = staff.id, status: ItemStatus = ItemStatus.DISPONIBLE) =
    foundItem(id = id, status = status).toEntity().copy(registeredBy = by)

class OfflineFirstSyncTest {

    private val reports = FakeTable<LostReportEntity>(emptyList()) { it.id }
    private val items = FakeTable<FoundItemEntity>(emptyList()) { it.id }
    private val remoteReports = FakeRemote<LostReportDto>(fieldOf = { dto, field -> if (field == "ownerId") dto.ownerId else null })
    private val remoteItems = FakeRemote<FoundItemDto>()
    private val auth = FakeRemoteAuth()

    private fun sync() = FirestoreRemoteSync(reports.table, items.table, auth, remoteReports, remoteItems)

    /** Arranca la sincronización de [user] en segundo plano (como el LaunchedEffect de AppNavHost). */
    private fun TestScope.signIn(user: UserModel) {
        auth.uid = user.id
        backgroundScope.launch { sync().syncWhileSignedIn(user) }
        runCurrent()
    }

    // ---- Mappers DTO ↔ Entity ----

    @Test
    fun lostReportsSurviveTheRoundTripThroughFirestore() {
        val entity = report("lr-1", estudiante.id)
        assertEquals(entity, entity.toDto().toEntity("lr-1"))
    }

    @Test
    fun foundItemsDoNotUploadTheExpectedAnswersButKeepTheLocalOnes() {
        val entity = item("fi-1").copy(expectedAnswers = listOf("Mateo"))
        val dto = entity.toDto()
        assertTrue("Mateo" !in dto.toString())
        assertEquals(entity, dto.toEntity("fi-1", local = entity))
        assertEquals(emptyList(), dto.toEntity("fi-1")?.expectedAnswers)
    }

    @Test
    fun documentsWithUnknownValuesAreIgnored() {
        assertNull(report("lr-1", estudiante.id).toDto().copy(status = "ESTADO_NUEVO").toEntity("lr-1"))
        assertNull(LostReportDto().toEntity("lr-vacio"))
        assertNull(item("fi-1").toDto().copy(category = "").toEntity("fi-1"))
    }

    // ---- Listeners que actualizan Room ----

    @Test
    fun aDocumentFromAnotherPhoneIsSavedInRoom() = runTest {
        signIn(staff)
        remoteItems.docs.value = mapOf("fi-remoto" to item("fi-remoto").toDto())
        runCurrent()
        assertEquals(item("fi-remoto"), items.byId("fi-remoto"))
        // Lo que llegó del remoto no se vuelve a subir.
        assertEquals(emptyList(), remoteItems.pushes)
    }

    @Test
    fun aChangeMadeOnThePhoneIsUploadedOnceWithoutLooping() = runTest {
        signIn(estudiante)
        reports.upsert(listOf(report("lr-nuevo", estudiante.id)))
        runCurrent()
        reports.upsert(listOf(report("lr-nuevo", estudiante.id, ReportStatus.CERRADO)))
        runCurrent()

        assertEquals(listOf("lr-nuevo", "lr-nuevo"), remoteReports.pushes)
        assertEquals("CERRADO", remoteReports.docs.value.getValue("lr-nuevo").status)
    }

    @Test
    fun onStartTheRemoteWinsAndLocalOnlyRowsAreUploaded() = runTest {
        // El teléfono tiene una versión vieja de lr-1 y un reporte hecho sin internet (lr-offline).
        reports.upsert(listOf(report("lr-1", estudiante.id), report("lr-offline", estudiante.id)))
        remoteReports.docs.value = mapOf("lr-1" to report("lr-1", estudiante.id, ReportStatus.EN_VERIFICACION).toDto())

        signIn(estudiante)

        assertEquals(ReportStatus.EN_VERIFICACION, reports.byId("lr-1")?.status)
        assertEquals(listOf("lr-offline"), remoteReports.pushes)
    }

    @Test
    fun withoutConnectionTheAppKeepsWorkingAndReceivingChanges() = runTest {
        remoteItems.offline = true
        signIn(staff)
        items.upsert(listOf(item("fi-sin-red")))
        runCurrent()
        // La escritura quedó en la cola de Firestore; mientras tanto siguen llegando cambios de otros.
        remoteItems.docs.value = remoteItems.docs.value + ("fi-otro" to item("fi-otro").toDto())
        runCurrent()
        assertEquals(listOf("fi-sin-red"), remoteItems.pushes)
        assertEquals(item("fi-otro"), items.byId("fi-otro"))
    }

    @Test
    fun aWriteRejectedByTheRulesGoesBackToTheRemoteValue() = runTest {
        remoteItems.docs.value = mapOf("fi-1" to item("fi-1").toDto())
        signIn(estudiante)
        remoteItems.rejectAll = true
        items.upsert(listOf(item("fi-1").copy(custodyDetail = "Lo moví yo")))
        runCurrent()
        assertEquals(item("fi-1"), items.byId("fi-1"))
    }

    // ---- Qué se sincroniza ----

    @Test
    fun aStudentOnlyListensToAndUploadsTheirOwnReports() = runTest {
        remoteReports.docs.value = mapOf(
            "lr-mio" to report("lr-mio", estudiante.id).toDto(),
            "lr-ajeno" to report("lr-ajeno", "uid-otro").toDto()
        )
        signIn(estudiante)
        reports.upsert(listOf(report("lr-local-ajeno", "uid-otro")))
        runCurrent()

        assertEquals(RemoteFilter("ownerId", estudiante.id), remoteReports.lastFilter)
        assertEquals(listOf("lr-mio"), reports.rows.value.map { it.id }.filter { it != "lr-local-ajeno" })
        assertEquals(emptyList(), remoteReports.pushes)
    }

    @Test
    fun staffListensToAllReports() = runTest {
        remoteReports.docs.value = mapOf("lr-ajeno" to report("lr-ajeno", "uid-otro").toDto())
        signIn(staff)
        assertTrue(remoteReports.observing)
        assertNull(remoteReports.lastFilter)
        assertEquals(listOf("lr-ajeno"), reports.rows.value.map { it.id })
    }

    @Test
    fun seedDataAndDemoAccountsStayOnThePhone() = runTest {
        // El seed (u-...) no se sube aunque entre una cuenta de Google.
        reports.upsert(listOf(report("lr-seed", "u-mateo")))
        items.upsert(listOf(item("fi-seed", by = "u-ana-staff")))
        signIn(staff)
        assertEquals(emptyList(), remoteReports.pushes + remoteItems.pushes)

        // Con una cuenta de prueba no se usa Firebase.
        val demo = FakeRemote<LostReportDto>()
        reports.upsert(listOf(report("lr-2", "u-mateo")))
        FirestoreRemoteSync(reports.table, items.table, auth, demo, FakeRemote()).syncWhileSignedIn(mateo)
        assertFalse(demo.observing)
    }

    @Test
    fun anExpiredFirebaseSessionDoesNotSync() = runTest {
        auth.uid = null
        FirestoreRemoteSync(reports.table, items.table, auth, remoteReports, remoteItems).syncWhileSignedIn(estudiante)
        assertFalse(remoteReports.observing)
        assertEquals(emptyList(), remoteReports.pushes)
    }
}
