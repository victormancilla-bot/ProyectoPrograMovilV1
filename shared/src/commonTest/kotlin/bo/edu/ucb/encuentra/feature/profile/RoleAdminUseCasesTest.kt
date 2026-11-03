package bo.edu.ucb.encuentra.feature.profile

import bo.edu.ucb.encuentra.core.InMemoryUsers
import bo.edu.ucb.encuentra.core.data.local.seed.SeedData
import bo.edu.ucb.encuentra.core.domain.model.AuditAction
import bo.edu.ucb.encuentra.core.domain.model.Role
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.feature.claims.Direct
import bo.edu.ucb.encuentra.feature.foundform.FakeSession
import bo.edu.ucb.encuentra.feature.myreports.RecordingAudit
import bo.edu.ucb.encuentra.feature.profile.domain.usecase.AssignStaffByEmailUseCase
import bo.edu.ucb.encuentra.feature.profile.domain.usecase.ChangeUserRoleUseCase
import bo.edu.ucb.encuentra.feature.profile.domain.usecase.ObserveUsersUseCase
import bo.edu.ucb.encuentra.feature.profile.domain.usecase.RoleErrors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal fun seedUser(id: String) = SeedData.users.first { it.id == id }

class RoleAdminUseCasesTest {

    private val admin = seedUser("u-admin")
    private val staff = seedUser("u-ana-staff")
    private val student = seedUser("u-mateo")

    private val users = InMemoryUsers()
    private val audit = RecordingAudit()
    private val session = FakeSession(admin)
    private val getCurrentUser = GetCurrentUserUseCase(session)
    private val changeRole = ChangeUserRoleUseCase(users, audit, Direct, getCurrentUser)
    private val assignStaff = AssignStaffByEmailUseCase(users, changeRole)

    private fun roleOf(id: String) = users.users.value.first { it.id == id }.role
    private fun errorOf(result: Result<*>) = result.exceptionOrNull()?.message

    @Test
    fun adminAssignsStaffByEmailAndItIsAudited() = runTest {
        val result = assignStaff("  Ana.Gomez@UCB.edu.bo ")

        assertEquals(Role.STAFF, result.getOrThrow().role)
        assertEquals(Role.STAFF, roleOf("u-ana-gomez"))
        assertEquals(listOf(AuditAction.ROLE_CHANGED), audit.actions)
    }

    @Test
    fun adminCanRemoveStaff() = runTest {
        changeRole(staff.id, Role.ESTUDIANTE).getOrThrow()
        assertEquals(Role.ESTUDIANTE, roleOf(staff.id))
    }

    @Test
    fun onlyAdminChangesRolesAndNobodyMakesThemselvesStaff() = runTest {
        session.setCurrentUser(student)
        assertEquals(RoleErrors.ONLY_ADMIN, errorOf(changeRole(student.id, Role.STAFF)))

        session.setCurrentUser(staff)
        assertEquals(RoleErrors.ONLY_ADMIN, errorOf(assignStaff("ana.gomez@ucb.edu.bo")))

        session.setCurrentUser(admin)
        assertEquals(RoleErrors.SELF, errorOf(changeRole(admin.id, Role.ESTUDIANTE)))

        assertEquals(Role.ESTUDIANTE, roleOf(student.id))
        assertTrue(audit.actions.isEmpty())
    }

    @Test
    fun adminAccountsAndAdminRoleAreNotChangedFromTheApp() = runTest {
        users.users.value += admin.copy(id = "u-admin-2", email = bo.edu.ucb.encuentra.core.domain.vo.Email("otro@ucb.edu.bo"))
        assertEquals(RoleErrors.ADMIN_LOCKED, errorOf(changeRole("u-admin-2", Role.ESTUDIANTE)))
        assertEquals(RoleErrors.NOT_ASSIGNABLE, errorOf(changeRole(student.id, Role.ADMIN)))
    }

    @Test
    fun emailIsValidatedAndMustBelongToAnAccount() = runTest {
        assertEquals(RoleErrors.EMAIL_EMPTY, errorOf(assignStaff(" ")))
        assertEquals(RoleErrors.EMAIL_NOT_INSTITUTIONAL, errorOf(assignStaff("ana@gmail.com")))
        assertEquals(RoleErrors.NOT_FOUND, errorOf(assignStaff("nadie@ucb.edu.bo")))
        assertEquals(RoleErrors.alreadyHas(staff), errorOf(assignStaff("staff@ucb.edu.bo")))
        assertTrue(audit.actions.isEmpty())
    }

    @Test
    fun onlyAdminSeesTheAccounts() = runTest {
        val observe = ObserveUsersUseCase(users, getCurrentUser)
        assertEquals(SeedData.users.size, observe().first().size)

        session.setCurrentUser(staff)
        assertTrue(observe().first().isEmpty())
    }
}
