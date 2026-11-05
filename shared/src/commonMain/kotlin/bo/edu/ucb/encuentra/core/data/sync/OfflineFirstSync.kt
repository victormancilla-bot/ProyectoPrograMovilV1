package bo.edu.ucb.encuentra.core.data.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

/** Filtro de igualdad sobre un campo del documento (por ejemplo, los reportes de un dueño). */
data class RemoteFilter(val field: String, val value: String)

/** Una tabla de Room vista por la sincronización: todas sus filas y cómo guardar las que llegan del remoto. */
class LocalTable<E>(val rows: Flow<List<E>>, val upsertAll: suspend (List<E>) -> Unit)

/** Una colección remota (Firestore). Los tests usan una en memoria. */
interface RemoteCollection<D> {
    /** Listener en tiempo real: cada emisión trae TODOS los documentos que cumplen [filter], por id. */
    fun observe(filter: RemoteFilter? = null): Flow<Map<String, D>>

    /** Escribe el documento. Puede quedar suspendido sin conexión hasta que el servidor lo confirme. */
    suspend fun push(id: String, dto: D)
}

/**
 * L06 · Sincronización offline-first de una tabla de Room con una colección de Firestore.
 *
 * - **Room es la verdad local:** las pantallas y los casos de uso solo leen y escriben Room, con o sin internet.
 * - **Subida:** cada cambio en Room que difiera de lo último conocido en el remoto se escribe en Firestore. La
 *   escritura se lanza aparte: sin conexión queda en la cola local de Firestore (que sobrevive a cerrar la app) y
 *   no frena el resto.
 * - **Bajada (listener):** cada documento remoto que difiera de lo último conocido se guarda en Room.
 * - **Sin bucles:** [known] recuerda el último valor visto en cualquiera de los dos lados; el eco de una escritura
 *   propia llega igual a lo conocido y se ignora.
 * - **Avisos viejos de Room:** un aviso de Room emitido ANTES de guardar lo que llegó del remoto puede procesarse
 *   después (los eventos esperan en cola). Ese valor viejo no se sube: la fila queda en [awaitingEcho] hasta que
 *   Room muestre lo guardado. Sin esto, Room y Firestore se pisaban en bucle.
 * - **Al arrancar gana el remoto:** hasta recibir la primera foto de Firestore (sale de su caché, también sin
 *   internet) no se sube nada; así un teléfono con datos viejos no pisa cambios de otro. Después se suben las filas
 *   locales que el remoto no tiene.
 * - **Escritura rechazada** (reglas): Firestore revierte su caché, el listener trae el valor anterior y Room vuelve
 *   a él. El remoto manda.
 * - Borrados: la app no borra reportes ni objetos (cambian de estado), así que no se sincronizan.
 */
class OfflineFirstSync<E, D>(
    private val local: Flow<List<E>>,
    private val saveLocal: suspend (List<E>) -> Unit,
    private val remote: RemoteCollection<D>,
    private val idOf: (E) -> String,
    private val toDto: (E) -> D,
    private val toEntity: (id: String, dto: D, local: E?) -> E?,
    private val shouldPush: (E) -> Boolean = { true },
    private val filter: RemoteFilter? = null,
    private val onPushFailed: (id: String, error: Throwable) -> Unit = { _, _ -> }
) {
    private val known = mutableMapOf<String, D>()
    private val awaitingEcho = mutableMapOf<String, D>()
    private var localRows = emptyMap<String, E>()
    private var remoteReady = false

    /** Corre hasta que se cancele (al cerrar sesión). */
    suspend fun run(): Unit = coroutineScope {
        // Un solo colector: los eventos de ambos lados se procesan de a uno, sin carreras sobre [known].
        val remoteEvents: Flow<suspend () -> Unit> = remote.observe(filter).map { docs -> { applyRemote(docs) } }
        val localEvents: Flow<suspend () -> Unit> = local.map { rows ->
            {
                localRows = rows.associateBy(idOf)
                if (remoteReady) pushChanges(rows)
            }
        }
        merge(remoteEvents, localEvents).collect { event -> event() }
    }

    private suspend fun CoroutineScope.applyRemote(docs: Map<String, D>) {
        val incoming = docs.filter { (id, dto) -> known[id] != dto }
        incoming.forEach { (id, dto) -> known[id] = dto }
        val rows = incoming.mapNotNull { (id, dto) -> toEntity(id, dto, localRows[id]) }
        if (rows.isNotEmpty()) {
            rows.forEach { awaitingEcho[idOf(it)] = toDto(it) }
            saveLocal(rows)
        }
        if (!remoteReady) {
            remoteReady = true
            // Lo que solo existe en el teléfono (creado sin internet o antes de la primera sincronización).
            pushChanges(localRows.values.filter { idOf(it) !in known })
        }
    }

    private fun CoroutineScope.pushChanges(rows: Collection<E>) {
        rows.filter(shouldPush).forEach { row ->
            val id = idOf(row)
            val dto = toDto(row)
            val expected = awaitingEcho[id]
            if (expected != null) {
                // Hasta ver en Room lo que llegó del remoto, cualquier otro valor de esa fila es un aviso viejo.
                if (expected == dto) awaitingEcho.remove(id)
                return@forEach
            }
            if (known[id] == dto) return@forEach
            known[id] = dto
            launch {
                runCatching { remote.push(id, dto) }.onFailure { onPushFailed(id, it) }
            }
        }
    }
}
