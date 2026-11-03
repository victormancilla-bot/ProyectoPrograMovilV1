package bo.edu.ucb.encuentra.feature.login

import bo.edu.ucb.encuentra.core.InMemoryUsers
import bo.edu.ucb.encuentra.core.data.local.seed.SeedData
import bo.edu.ucb.encuentra.core.domain.model.Role
import bo.edu.ucb.encuentra.core.domain.model.UserModel
import bo.edu.ucb.encuentra.core.domain.repository.SessionRepository
import bo.edu.ucb.encuentra.core.domain.repository.UserRepository
import bo.edu.ucb.encuentra.core.domain.vo.Email
import bo.edu.ucb.encuentra.core.domain.vo.Password
import bo.edu.ucb.encuentra.feature.login.domain.usecase.AuthenticateUseCase
import bo.edu.ucb.encuentra.feature.login.domain.usecase.ValidateInstitutionalEmailUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthenticateUseCaseTest {

    private class FakeSession : SessionRepository {
        var user: UserModel? = null
        override fun getCurrentUser() = user
        override suspend fun setCurrentUser(user: UserModel) { this.user = user }
        override suspend fun clearSession() { user = null }
        override suspend fun restore() = Unit
    }

    /** Usuarios del seed; cuenta las búsquedas para comprobar que un correo no institucional no consulta la base. */
    private class FakeUsers : InMemoryUsers() {
        var lookups = 0
        override suspend fun findByEmail(email: Email): UserModel? {
            lookups++
            return super.findByEmail(email)
        }
    }

    private val session = FakeSession()
    private val users = FakeUsers()
    private val authenticate = AuthenticateUseCase(users, session)

    @Test
    fun staffLoginSavesSessionWithStaffRole() = runTest {
        val result = authenticate(Email("staff@ucb.edu.bo"), Password(AuthenticateUseCase.DEMO_PASSWORD))

        assertTrue(result.isSuccess)
        assertEquals(Role.STAFF, session.user?.role)
    }

    @Test
    fun emailIsCaseInsensitive() = runTest {
        val result = authenticate(Email(" Estudiante@UCB.edu.bo "), Password(AuthenticateUseCase.DEMO_PASSWORD))

        assertEquals("u-mateo", result.getOrNull()?.id)
    }

    @Test
    fun wrongPasswordFailsAndDoesNotSaveSession() = runTest {
        val result = authenticate(Email("estudiante@ucb.edu.bo"), Password("otra123"))

        assertEquals(AuthenticateUseCase.WRONG_CREDENTIALS, result.exceptionOrNull()?.message)
        assertNull(session.user)
    }

    @Test
    fun unknownUserGetsTheSameMessageAsWrongPassword() = runTest {
        val result = authenticate(Email("nadie@ucb.edu.bo"), Password(AuthenticateUseCase.DEMO_PASSWORD))

        assertEquals(AuthenticateUseCase.WRONG_CREDENTIALS, result.exceptionOrNull()?.message)
        assertNull(session.user)
    }

    @Test
    fun nonInstitutionalEmailIsRejectedWithoutLookingUpUsers() = runTest {
        val result = authenticate(Email("mateo@gmail.com"), Password(AuthenticateUseCase.DEMO_PASSWORD))

        assertEquals(ValidateInstitutionalEmailUseCase.NOT_INSTITUTIONAL, result.exceptionOrNull()?.message)
        assertEquals(0, users.lookups)
        assertNull(session.user)
    }

    @Test
    fun emailStepValidation() {
        val validate = ValidateInstitutionalEmailUseCase()

        assertEquals(ValidateInstitutionalEmailUseCase.EMPTY, validate("  ").exceptionOrNull()?.message)
        assertEquals(ValidateInstitutionalEmailUseCase.NOT_INSTITUTIONAL, validate("ana@gmail.com").exceptionOrNull()?.message)
        assertEquals("ana.gomez@ucb.edu.bo", validate(" ana.gomez@ucb.edu.bo ").getOrNull()?.value)
    }
}
