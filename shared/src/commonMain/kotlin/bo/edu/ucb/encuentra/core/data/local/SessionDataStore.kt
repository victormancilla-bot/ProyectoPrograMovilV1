package bo.edu.ucb.encuentra.core.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

/** Archivo de DataStore donde se guarda la sesión (S01). Cada plataforma decide la carpeta. */
const val SESSION_DATASTORE_FILE = "session.preferences_pb"

fun createSessionDataStore(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { producePath().toPath() })
