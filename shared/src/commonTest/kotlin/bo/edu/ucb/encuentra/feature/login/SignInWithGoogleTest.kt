package bo.edu.ucb.encuentra.feature.login

import bo.edu.ucb.encuentra.core.InMemoryUsers
import bo.edu.ucb.encuentra.core.data.remote.UserDocument
import bo.edu.ucb.encuentra.core.data.remote.toModel
import bo.edu.ucb.encuentra.core.domain.model.Role
import bo.edu.ucb.encuentra.core.domain.model.UserModel
import bo.edu.ucb.encuentra.core.domain.repository.RemoteAccount
import bo.edu.ucb.encuentra.core.domain.repository.RemoteAuth
import bo.edu.ucb.encuentra.core.domain.repository.RemoteUserStore
import bo.edu.ucb.encuentra.core.domain.usecase.RestoreSessionUseCase
import bo.edu.ucb.encuentra.feature.foundform.FakeSession
import bo.edu.ucb.encuentra.feature.login.domain.usecase.SignInWithGoogleUseCase
import bo.edu.ucb.encuentra.feature.myreports.mateo
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Firebase Auth de prueba: el token decide qué cuenta entra. */
internal class FakeRemoteAuth(private val accounts: Map<String, RemoteAccount> = emptyMap()) : RemoteAuth {
    var uid: String? = null
    var signOuts = 0
    override suspend fun signInWithGoogle(idToken: String): RemoteAccount =
        (accounts[idToken] ?: error("token inválido")).also { uid = it.uid }
    override fun currentUid() = uid
    override suspend fun signOut() {
        signOuts++
        uid = null
    }
}

/** `users/{uid}` de prueba: los que ya existen conservan su rol; los nuevos se crean como ESTUDIANTE. */
internal class FakeRemoteUsers(private val existing: Map<String, UserDocument> = emptyMap()) : RemoteUserStore {
    val created = mutableListOf<String>()
    override suspend fun getOrCreate(account: RemoteAccount): UserModel =
        (existing[account.uid] ?: UserDocument(account.email, account.displayName ?: account.email).also { created += account.uid })
            .toModel(account.uid)
}

internal val anaGoogle = RemoteAccount("uid-ana", "ana.perez@ucb.edu.bo", "Ana Pérez")
private val gmail = RemoteAccount("uid-gmail", "ana@gmail.com", "Ana")

class SignInWithGoogleTest {

    private val auth = FakeRemoteAuth(mapOf("token-ana" to anaGoogle, "token-gmail" to gmail))
    private val users = InMemoryUsers()
    private val session = FakeSession(null)

    @Test
    fun firstLoginCreatesAStudentAccountAndStartsTheSession() = runTest {
        val remote = FakeRemoteUsers()
        val user = SignInWithGoogleUseCase(auth, remote, users, session)("token-ana").getOrThrow()

        assertEquals(Role.ESTUDIANTE, user.role)
        assertEquals("Ana Pérez", user.fullName)
        assertEquals(listOf("uid-ana"), remote.created)
        assertEquals(user, users.findById("uid-ana"))
        assertEquals(user, session.getCurrentUser())
    }

    @Test
    fun anExistingAccountKeepsTheRoleGivenByAnAdmin() = runTest {
        val remote = FakeRemoteUsers(mapOf("uid-ana" to UserDocument(anaGoogle.email, "Ana Pérez", role = Role.STAFF.name)))
        val user = SignInWithGoogleUseCase(auth, remote, users, session)("token-ana").getOrThrow()
        assertEquals(Role.STAFF, user.role)
        assertEquals(emptyList(), remote.created)
    }

    @Test
    fun nonInstitutionalAccountsAreRejectedAndSignedOut() = runTest {
        val result = SignInWithGoogleUseCase(auth, FakeRemoteUsers(), users, session)("token-gmail")
        assertEquals(SignInWithGoogleUseCase.NOT_INSTITUTIONAL, result.exceptionOrNull()?.message)
        assertEquals(1, auth.signOuts)
        assertNull(session.getCurrentUser())
    }

    @Test
    fun aFirebaseErrorShowsAFriendlyMessage() = runTest {
        val result = SignInWithGoogleUseCase(auth, FakeRemoteUsers(), users, session)("token-roto")
        assertEquals(SignInWithGoogleUseCase.GOOGLE_FAILED, result.exceptionOrNull()?.message)
    }

    @Test
    fun anExpiredFirebaseSessionLogsTheGoogleAccountOutButNotTheDemoAccounts() = runTest {
        val google = mateo.copy(id = "uid-ana")
        session.setCurrentUser(google)
        RestoreSessionUseCase(session, auth)() // Firebase ya no tiene sesión en el teléfono
        assertNull(session.getCurrentUser())

        session.setCurrentUser(google)
        auth.uid = "uid-ana"
        RestoreSessionUseCase(session, auth)()
        assertEquals(google, session.getCurrentUser())

        // Cuentas de prueba (u-...): no dependen de Firebase.
        session.setCurrentUser(mateo)
        auth.uid = null
        RestoreSessionUseCase(session, auth)()
        assertEquals(mateo, session.getCurrentUser())
    }
}
