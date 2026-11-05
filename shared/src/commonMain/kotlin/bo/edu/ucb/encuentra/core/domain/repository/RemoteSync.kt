package bo.edu.ucb.encuentra.core.domain.repository

import bo.edu.ucb.encuentra.core.domain.model.UserModel

/**
 * L06 · Sincroniza los datos locales (Room) con Firestore mientras [user] tenga sesión. Se cancela al cerrar
 * sesión o al cambiar de usuario. Las cuentas de prueba (`u-...`) no usan Firebase: con ellas la app trabaja solo
 * con Room.
 */
interface RemoteSync {
    suspend fun syncWhileSignedIn(user: UserModel?)
}

object NoRemoteSync : RemoteSync {
    override suspend fun syncWhileSignedIn(user: UserModel?) = Unit
}
