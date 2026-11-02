package bo.edu.ucb.encuentra.feature.foundform

import bo.edu.ucb.encuentra.core.domain.model.Campus
import bo.edu.ucb.encuentra.core.domain.model.CustodyPlace
import bo.edu.ucb.encuentra.core.domain.model.FoundItem
import bo.edu.ucb.encuentra.core.domain.model.ItemStatus
import bo.edu.ucb.encuentra.core.domain.model.Role
import bo.edu.ucb.encuentra.core.domain.model.UserModel
import bo.edu.ucb.encuentra.core.domain.repository.SessionRepository
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.core.domain.vo.Email
import bo.edu.ucb.encuentra.feature.foundform.domain.repository.FoundItemRepository
import bo.edu.ucb.encuentra.feature.foundform.domain.usecase.RegisterFoundItemUseCase
import bo.edu.ucb.encuentra.feature.foundform.domain.usecase.ValidateFoundItemUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class FakeFoundItemRepository : FoundItemRepository {
    val saved = mutableListOf<FoundItem>()
    override suspend fun register(item: FoundItem) {
        saved += item
    }
}

internal class FakeSession(private var user: UserModel?) : SessionRepository {
    override fun getCurrentUser() = user
    override suspend fun setCurrentUser(user: UserModel) {
        this.user = user
    }
    override suspend fun clearSession() {
        user = null
    }
    override suspend fun restore() = Unit
}

internal fun demoUser(role: Role) = UserModel(
    id = "u-ana-staff",
    fullName = "Ana Staff",
    email = Email("staff@ucb.edu.bo"),
    role = role,
    campus = Campus.LA_PAZ,
    faculty = "Bienestar Estudiantil"
)

class RegisterFoundItemUseCaseTest {

    private val repository = FakeFoundItemRepository()

    private fun register(user: UserModel?) = RegisterFoundItemUseCase(
        ValidateFoundItemUseCase(FixedClock, BoliviaTime),
        repository,
        GetCurrentUserUseCase(FakeSession(user)),
        FixedClock
    )

    @Test
    fun savesAnAvailableItemWithCustodyAndRetention() = runTest {
        val draft = validFoundDraft.copy(
            title = "  Smartphone Negro ",
            brand = " ",
            custodyPlace = CustodyPlace.LOCKER,
            controlQuestions = listOf(" ¿Qué fondo de pantalla tiene? ", "")
        )
        val item = register(demoUser(Role.STAFF))(draft).getOrThrow()

        assertEquals(listOf(item), repository.saved)
        assertEquals(ItemStatus.DISPONIBLE, item.status)
        assertEquals("Smartphone Negro", item.title)
        assertNull(item.brand)
        assertEquals(CustodyPlace.LOCKER, item.custodyPlace)
        assertEquals(listOf("¿Qué fondo de pantalla tiene?"), item.controlQuestions)
        assertEquals("u-ana-staff", item.registeredBy)
        assertEquals(Campus.LA_PAZ, item.campus)
        assertEquals(FixedClock.now() + RegisterFoundItemUseCase.RETENTION, item.retentionUntil)
    }

    @Test
    fun invalidDraftIsNotSaved() = runTest {
        val result = register(demoUser(Role.STAFF))(validFoundDraft.copy(custodyPlace = null))
        assertTrue(result.isFailure)
        assertTrue(repository.saved.isEmpty())
    }

    @Test
    fun onlyStaffCanRegister() = runTest {
        val result = register(demoUser(Role.ESTUDIANTE))(validFoundDraft)
        assertEquals(RegisterFoundItemUseCase.NOT_STAFF, result.exceptionOrNull()?.message)
        assertEquals(RegisterFoundItemUseCase.NO_SESSION, register(null)(validFoundDraft).exceptionOrNull()?.message)
        assertTrue(repository.saved.isEmpty())
    }
}
