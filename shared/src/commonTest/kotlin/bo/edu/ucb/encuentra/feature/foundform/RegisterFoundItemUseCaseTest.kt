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
import bo.edu.ucb.encuentra.feature.reportwizard.FakeMatchingTrigger
import bo.edu.ucb.encuentra.feature.foundform.domain.usecase.RegisterFoundItemUseCase
import bo.edu.ucb.encuentra.feature.foundform.domain.usecase.ValidateFoundItemUseCase
import bo.edu.ucb.encuentra.core.FixedAppConfig
import bo.edu.ucb.encuentra.core.domain.usecase.GetAppConfigUseCase
import kotlinx.coroutines.test.runTest
import kotlin.time.Duration.Companion.days
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
    private val matching = FakeMatchingTrigger()
    private val config = FixedAppConfig()

    private fun register(user: UserModel?, trigger: FakeMatchingTrigger = matching) = RegisterFoundItemUseCase(
        ValidateFoundItemUseCase(FixedClock, BoliviaTime),
        repository,
        trigger,
        GetCurrentUserUseCase(FakeSession(user)),
        FixedClock,
        GetAppConfigUseCase(config)
    )

    @Test
    fun savesAnAvailableItemWithCustodyAndRetention() = runTest {
        val draft = validFoundDraft.copy(
            title = "  Smartphone Negro ",
            brand = " ",
            custodyPlace = CustodyPlace.LOCKER,
            controlQuestions = listOf(" ¿Qué fondo de pantalla tiene? ", ""),
            expectedAnswers = listOf(" Un perro ", ""),
            photos = listOf("file:///a.jpg", "file:///b.jpg")
        )
        val item = register(demoUser(Role.STAFF))(draft).getOrThrow()

        assertEquals(listOf(item), repository.saved)
        assertEquals(listOf(item), matching.registered)
        assertEquals(ItemStatus.DISPONIBLE, item.status)
        assertEquals("Smartphone Negro", item.title)
        assertNull(item.brand)
        assertEquals(CustodyPlace.LOCKER, item.custodyPlace)
        assertEquals(listOf("¿Qué fondo de pantalla tiene?"), item.controlQuestions)
        assertEquals(listOf("Un perro"), item.expectedAnswers)
        assertEquals(listOf("file:///a.jpg", "file:///b.jpg"), item.photoUrls)
        assertEquals("u-ana-staff", item.registeredBy)
        assertEquals(Campus.LA_PAZ, item.campus)
        assertEquals(FixedClock.now() + 90.days, item.retentionUntil)
    }

    @Test
    fun matchingFailureDoesNotLoseTheItem() = runTest {
        val result = register(demoUser(Role.STAFF), FakeMatchingTrigger(fail = true))(validFoundDraft)
        assertTrue(result.isSuccess)
        assertEquals(1, repository.saved.size)
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

    @Test
    fun retentionComesFromRemoteConfig() = runTest {
        config.update { it.copy(retentionDays = 30) }
        val item = register(demoUser(Role.STAFF))(validFoundDraft).getOrThrow()
        assertEquals(FixedClock.now() + 30.days, item.retentionUntil)
    }
}
