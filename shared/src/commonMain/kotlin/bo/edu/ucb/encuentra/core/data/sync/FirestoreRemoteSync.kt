package bo.edu.ucb.encuentra.core.data.sync

import bo.edu.ucb.encuentra.core.data.local.entity.FoundItemEntity
import bo.edu.ucb.encuentra.core.data.local.entity.LostReportEntity
import bo.edu.ucb.encuentra.core.data.remote.dto.FoundItemDto
import bo.edu.ucb.encuentra.core.data.remote.dto.LostReportDto
import bo.edu.ucb.encuentra.core.data.remote.dto.toDto
import bo.edu.ucb.encuentra.core.data.remote.dto.toEntity
import bo.edu.ucb.encuentra.core.domain.model.UserModel
import bo.edu.ucb.encuentra.core.domain.repository.RemoteAuth
import bo.edu.ucb.encuentra.core.domain.repository.RemoteSync
import bo.edu.ucb.encuentra.core.domain.usecase.RestoreSessionUseCase.Companion.DEMO_ID_PREFIX
import bo.edu.ucb.encuentra.core.util.safeCall
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * L06 · Repositorios offline-first de reportes de pérdida y objetos encontrados: Room + Firestore.
 *
 * Qué se sincroniza (las reglas de `firebase/firestore.rules` exigen lo mismo):
 * - `lost_reports`: el estudiante escucha y sube **solo los suyos**; el staff escucha todos (los necesita para el
 *   matching y los reclamos) y sube los cambios de estado que hace.
 * - `found_items`: todos escuchan la colección (catálogo y matching del estudiante); el staff los registra.
 * - Los datos de prueba del seed (dueño o registrador `u-...`) se quedan en el teléfono: no se suben.
 */
class FirestoreRemoteSync(
    private val lostReportTable: LocalTable<LostReportEntity>,
    private val foundItemTable: LocalTable<FoundItemEntity>,
    private val remoteAuth: RemoteAuth,
    private val lostReports: RemoteCollection<LostReportDto>,
    private val foundItems: RemoteCollection<FoundItemDto>
) : RemoteSync {

    override suspend fun syncWhileSignedIn(user: UserModel?) {
        // Sin cuenta de Google con sesión en Firebase, las reglas rechazarían todo: se trabaja solo con Room.
        if (user == null || user.id.startsWith(DEMO_ID_PREFIX) || remoteAuth.currentUid() != user.id) return
        coroutineScope {
            launch { runLogged("lost_reports") { lostReportsSync(user).run() } }
            launch { runLogged("found_items") { foundItemsSync().run() } }
        }
    }

    internal fun lostReportsSync(user: UserModel) = OfflineFirstSync<LostReportEntity, LostReportDto>(
        local = lostReportTable.rows,
        saveLocal = lostReportTable.upsertAll,
        remote = lostReports,
        idOf = LostReportEntity::id,
        toDto = LostReportEntity::toDto,
        toEntity = { id, dto, _ -> dto.toEntity(id) },
        // El estudiante solo escribe sus reportes; el staff también actualiza los ajenos (reclamos y entregas).
        shouldPush = { !it.ownerId.startsWith(DEMO_ID_PREFIX) && (user.role.isStaff || it.ownerId == user.id) },
        filter = if (user.role.isStaff) null else RemoteFilter(OWNER_FIELD, user.id),
        onPushFailed = ::logPushFailed
    )

    internal fun foundItemsSync() = OfflineFirstSync<FoundItemEntity, FoundItemDto>(
        local = foundItemTable.rows,
        saveLocal = foundItemTable.upsertAll,
        remote = foundItems,
        idOf = FoundItemEntity::id,
        toDto = FoundItemEntity::toDto,
        toEntity = { id, dto, local -> dto.toEntity(id, local) },
        shouldPush = { !it.registeredBy.startsWith(DEMO_ID_PREFIX) },
        onPushFailed = ::logPushFailed
    )

    /** Si Firebase no está configurado o el listener falla, la app sigue con Room (offline). */
    private suspend fun runLogged(name: String, block: suspend () -> Unit) {
        safeCall { block() }.onFailure { println("$TAG: se detuvo la sincronización de $name: ${it.message}") }
    }

    private fun logPushFailed(id: String, error: Throwable) {
        println("$TAG: no se pudo subir $id: ${error.message}")
    }

    private companion object {
        const val OWNER_FIELD = "ownerId"
        const val TAG = "UcbSync"
    }
}
