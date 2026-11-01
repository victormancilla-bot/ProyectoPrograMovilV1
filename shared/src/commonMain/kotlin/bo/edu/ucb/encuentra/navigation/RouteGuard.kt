package bo.edu.ucb.encuentra.navigation

import bo.edu.ucb.encuentra.core.domain.model.Role
import bo.edu.ucb.encuentra.core.domain.model.UserModel

/**
 * Guard de navegación (F05): sin sesión → Login; rol incorrecto → home de su rol.
 * El `when` es exhaustivo: si agregas una ruta a NavRoute, el compilador te obliga a decir quién puede entrar.
 */
object RouteGuard {

    private enum class Access { PUBLIC, STUDENT, STAFF, ANY_USER }

    fun homeFor(role: Role): NavRoute = if (role.isStaff) NavRoute.Dashboard else NavRoute.Home

    /** null si puede entrar a [route]; si no, la ruta a la que hay que redirigir. */
    fun redirect(route: NavRoute, user: UserModel?): NavRoute? = when (accessOf(route)) {
        Access.PUBLIC -> user?.let { homeFor(it.role) }
        Access.ANY_USER -> if (user == null) NavRoute.Login else null
        Access.STUDENT -> when {
            user == null -> NavRoute.Login
            user.role.isStaff -> homeFor(user.role)
            else -> null
        }
        Access.STAFF -> when {
            user == null -> NavRoute.Login
            !user.role.isStaff -> homeFor(user.role)
            else -> null
        }
    }

    private fun accessOf(route: NavRoute): Access = when (route) {
        NavRoute.Login -> Access.PUBLIC

        NavRoute.Home,
        NavRoute.ReportWizard,
        NavRoute.MyReports,
        NavRoute.FoundCatalog,
        NavRoute.HowItWorks,
        NavRoute.MatchList,
        is NavRoute.MatchDetail,
        is NavRoute.Verify -> Access.STUDENT

        NavRoute.Dashboard,
        NavRoute.FoundCapture,
        NavRoute.FoundForm,
        NavRoute.Inventory,
        NavRoute.Claims,
        is NavRoute.ClaimDetail,
        is NavRoute.Delivery,
        NavRoute.Audit -> Access.STAFF

        NavRoute.Alerts,
        NavRoute.Profile,
        NavRoute.DevMenu,
        NavRoute.DesignSystemDemo -> Access.ANY_USER
    }
}
