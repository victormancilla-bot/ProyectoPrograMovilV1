package bo.edu.ucb.encuentra.feature.profile

import bo.edu.ucb.encuentra.core.InMemoryUsers
import bo.edu.ucb.encuentra.core.domain.model.Role
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.feature.claims.Direct
import bo.edu.ucb.encuentra.feature.foundform.FakeSession
import bo.edu.ucb.encuentra.feature.myreports.RecordingAudit
import bo.edu.ucb.encuentra.feature.profile.domain.usecase.AssignStaffByEmailUseCase
import bo.edu.ucb.encuentra.feature.profile.domain.usecase.ChangeUserRoleUseCase
import bo.edu.ucb.encuentra.feature.profile.domain.usecase.ObserveUsersUseCase
import bo.edu.ucb.encuentra.feature.profile.domain.usecase.RoleErrors
import bo.edu.ucb.encuentra.feature.profile.presentation.viewmodel.RoleFilter
import bo.edu.ucb.encuentra.feature.profile.presentation.viewmodel.UserRolesEvent
import bo.edu.ucb.encuentra.feature.profile.presentation.viewmodel.UserRolesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class UserRolesViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val users = InMemoryUsers()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(): UserRolesViewModel {
        val getCurrentUser = GetCurrentUserUseCase(FakeSession(seedUser("u-admin")))
        val change = ChangeUserRoleUseCase(users, RecordingAudit(), Direct, getCurrentUser)
        return UserRolesViewModel(
            ObserveUsersUseCase(users, getCurrentUser),
            AssignStaffByEmailUseCase(users, change),
            change,
            getCurrentUser
        )
    }

    private fun roleOf(id: String) = users.users.value.first { it.id == id }.role

    @Test
    fun assignStaffByEmailUpdatesTheListAndClearsTheField() = runTest(dispatcher) {
        val vm = viewModel()
        vm.handleEvent(UserRolesEvent.OnEmailChanged("ana.gomez@ucb.edu.bo"))
        vm.handleEvent(UserRolesEvent.OnAssignStaff)
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals("", state.email)
        assertEquals("Ana Gómez ahora es Staff.", state.message)
        assertEquals(Role.STAFF, state.users.first { it.id == "u-ana-gomez" }.role)

        vm.handleEvent(UserRolesEvent.OnFilterChanged(RoleFilter.STAFF))
        assertEquals(setOf("u-ana-staff", "u-ana-gomez"), vm.state.value.visibleUsers.map { it.id }.toSet())
    }

    @Test
    fun invalidEmailShowsTheErrorUnderTheField() = runTest(dispatcher) {
        val vm = viewModel()
        vm.handleEvent(UserRolesEvent.OnEmailChanged("ana@gmail.com"))
        vm.handleEvent(UserRolesEvent.OnAssignStaff)
        advanceUntilIdle()
        assertEquals(RoleErrors.EMAIL_NOT_INSTITUTIONAL, vm.state.value.emailError)
    }

    @Test
    fun removingStaffAsksForConfirmation() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        val staff = vm.state.value.users.first { it.id == "u-ana-staff" }
        assertTrue(vm.state.value.canRemoveStaff(staff))

        vm.handleEvent(UserRolesEvent.OnRequestChange(staff, Role.ESTUDIANTE))
        advanceUntilIdle()
        assertEquals(Role.STAFF, roleOf(staff.id))

        vm.handleEvent(UserRolesEvent.OnConfirmChange)
        advanceUntilIdle()
        assertEquals(Role.ESTUDIANTE, roleOf(staff.id))
        assertNull(vm.state.value.pending)
    }

    @Test
    fun theAdminCannotChangeTheirOwnRoleFromTheList() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        val me = vm.state.value.users.first { it.id == "u-admin" }
        assertFalse(vm.state.value.canRemoveStaff(me))
        assertFalse(vm.state.value.canMakeStaff(me))
    }
}
