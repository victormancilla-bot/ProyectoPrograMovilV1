package bo.edu.ucb.encuentra.core.data.remote

import bo.edu.ucb.encuentra.core.data.sync.RemoteCollection
import bo.edu.ucb.encuentra.core.data.sync.RemoteFilter
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException

/**
 * L06 · Una colección de Firestore (GitLive). La persistencia offline de Firestore viene activada en Android:
 * el listener responde desde la caché sin internet y las escrituras quedan en cola hasta reconectar.
 */
class FirestoreCollection<D : Any>(
    private val name: String,
    private val serializer: KSerializer<D>
) : RemoteCollection<D> {

    private val collection get() = Firebase.firestore.collection(name)

    override fun observe(filter: RemoteFilter?): Flow<Map<String, D>> {
        val query = filter?.let { f -> collection.where { f.field equalTo f.value } } ?: collection
        return query.snapshots.map { snapshot ->
            snapshot.documents.mapNotNull { doc ->
                // Un documento mal formado no tumba el listener: se ignora.
                val dto = try {
                    doc.data(serializer)
                } catch (_: SerializationException) {
                    null
                }
                dto?.let { doc.id to it }
            }.toMap()
        }
    }

    override suspend fun push(id: String, dto: D) {
        collection.document(id).set(serializer, dto)
    }

    companion object {
        const val LOST_REPORTS = "lost_reports"
        const val FOUND_ITEMS = "found_items"
    }
}
