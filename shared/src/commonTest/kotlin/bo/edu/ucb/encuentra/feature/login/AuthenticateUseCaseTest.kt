package bo.edu.ucb.encuentra.feature.login

import bo.edu.ucb.encuentra.core.domain.model.Role
import bo.edu.ucb.encuentra.core.domain.model.UserModel
import bo.edu.ucb.encuentra.core.domain.repository.SessionRepository
import bo.edu.ucb.encuentra.feature.login.domain.usecase.AuthenticateUseCase
import bo.edu.ucb.encuentra.feature.login.domain.vo.Email
import bo.edu.ucb.encuentra.feature.login.domain.vo.Password
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthenticateUseCaseTest {

    private class FakeSession : SessionRepository {
        var user: UserModel? = null
        override fun getCurrentUser() = user
        override fun setCurrentUser(user: UserModel) { this.user = user }
        override fun clearSession() { user = null }
    }

    @Test
    fun staffLoginSavesSessionWithStaffRole() = runTest {
        val session = FakeSession()
        val result = AuthenticateUseCase(session)(Email("staff@ucb.edu.bo"), Password("password"))

        assertTrue(result.isSuccess)
        assertEquals(Role.STAFF, session.user?.role)
    }

    @Test
    fun wrongPasswordFailsAndDoesNotSaveSession() = runTest {
        val session = FakeSession()
        val result = AuthenticateUseCase(session)(Email("estudiante@ucb.edu.bo"), Password("otra"))

        assertTrue(result.isFailure)
        assertNull(session.user)
    }
}
