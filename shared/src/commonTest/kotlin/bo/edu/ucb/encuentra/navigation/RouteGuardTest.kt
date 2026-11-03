package bo.edu.ucb.encuentra.navigation

import bo.edu.ucb.encuentra.core.domain.model.Campus
import bo.edu.ucb.encuentra.core.domain.model.Role
import bo.edu.ucb.encuentra.core.domain.model.UserModel
import bo.edu.ucb.encuentra.core.domain.vo.Email
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RouteGuardTest {

    private val student = UserModel("1", "Mateo", Email("estudiante@ucb.edu.bo"), Role.ESTUDIANTE, Campus.LA_PAZ)
    private val staff = UserModel("2", "Ana", Email("staff@ucb.edu.bo"), Role.STAFF, Campus.LA_PAZ)
    private val admin = UserModel("3", "Admin", Email("admin@ucb.edu.bo"), Role.ADMIN, Campus.LA_PAZ)

    @Test
    fun withoutSessionGoesToLogin() {
        assertEquals(NavRoute.Login, RouteGuard.redirect(NavRoute.Home, null))
        assertEquals(NavRoute.Login, RouteGuard.redirect(NavRoute.Dashboard, null))
        assertEquals(NavRoute.Login, RouteGuard.redirect(NavRoute.Profile, null))
        assertNull(RouteGuard.redirect(NavRoute.Login, null))
    }

    @Test
    fun wrongRoleGoesToHomeOfItsRole() {
        assertEquals(NavRoute.Home, RouteGuard.redirect(NavRoute.Inventory, student))
        assertEquals(NavRoute.Home, RouteGuard.redirect(NavRoute.ClaimDetail("c1"), student))
        assertEquals(NavRoute.Dashboard, RouteGuard.redirect(NavRoute.ReportWizard, staff))
        assertEquals(NavRoute.Dashboard, RouteGuard.redirect(NavRoute.MatchDetail("m1"), admin))
    }

    @Test
    fun rightRoleCanEnter() {
        assertNull(RouteGuard.redirect(NavRoute.MatchDetail("m1"), student))
        assertNull(RouteGuard.redirect(NavRoute.Claims, staff))
        assertNull(RouteGuard.redirect(NavRoute.Audit, admin))
        assertNull(RouteGuard.redirect(NavRoute.Alerts, student))
        assertNull(RouteGuard.redirect(NavRoute.Alerts, staff))
    }

    @Test
    fun onlyAdminEntersTheRolesPanel() {
        assertNull(RouteGuard.redirect(NavRoute.UserRoles, admin))
        assertEquals(NavRoute.Dashboard, RouteGuard.redirect(NavRoute.UserRoles, staff))
        assertEquals(NavRoute.Home, RouteGuard.redirect(NavRoute.UserRoles, student))
        assertEquals(NavRoute.Login, RouteGuard.redirect(NavRoute.UserRoles, null))
    }

    @Test
    fun loggedUserOnLoginGoesToItsHome() {
        assertEquals(NavRoute.Home, RouteGuard.redirect(NavRoute.Login, student))
        assertEquals(NavRoute.Dashboard, RouteGuard.redirect(NavRoute.Login, admin))
    }
}
