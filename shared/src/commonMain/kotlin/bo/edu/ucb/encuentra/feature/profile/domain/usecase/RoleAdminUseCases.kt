package bo.edu.ucb.encuentra.feature.profile.domain.usecase

import bo.edu.ucb.encuentra.core.domain.model.AuditAction
import bo.edu.ucb.encuentra.core.domain.model.Role
import bo.edu.ucb.encuentra.core.domain.model.UserModel
import bo.edu.ucb.encuentra.core.domain.repository.AuditRecorder
import bo.edu.ucb.encuentra.core.domain.repository.TransactionRunner
import bo.edu.ucb.encuentra.core.domain.repository.UserRepository
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.core.domain.vo.Email
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Error de una regla de roles; el mensaje se muestra tal cual en la pantalla. */
class RoleChangeException(message: String) : Exception(message)

object RoleErrors {
    const val ONLY_ADMIN = "Solo un administrador puede cambiar roles."
    const val SELF = "No puedes cambiar tu propio rol."
    const val NOT_FOUND = "No hay una cuenta con ese correo. La persona debe ingresar una vez a la app."
    const val ADMIN_LOCKED = "Las cuentas de administrador no se cambian desde la app."
    const val NOT_ASSIGNABLE = "Desde la app solo se asigna Estudiante o Staff."
    const val EMAIL_EMPTY = "Ingresa el correo institucional."
    const val EMAIL_NOT_INSTITUTIONAL = "Usa un correo institucional @ucb.edu.bo."
    fun alreadyHas(user: UserModel) = "${user.fullName} ya tiene el rol ${user.role.label}."
}

/** Nombre del rol para la UI ("Estudiante", "Staff", "Admin"). */
val Role.label: String
    get() = when (this) {
        Role.ESTUDIANTE -> "Estudiante"
        Role.STAFF -> "Staff"
        Role.ADMIN -> "Admin"
    }

/** Roles que el admin puede dar desde la app. ADMIN solo se crea en el seed / consola de Firebase. */
val ASSIGNABLE_ROLES = listOf(Role.ESTUDIANTE, Role.STAFF)

/** S03: cuentas para el panel de roles. Si quien pregunta no es ADMIN, no ve nada. */
class ObserveUsersUseCase(
    private val users: UserRepository,
    private val getCurrentUser: GetCurrentUserUseCase
) {
    operator fun invoke(): Flow<List<UserModel>> =
        if (getCurrentUser()?.role == Role.ADMIN) users.observeAll() else flowOf(emptyList())
}

/**
 * S03: cambia el rol de una cuenta. Reglas:
 * - solo ADMIN (el campo `role` de `users/{uid}` solo lo cambia un admin; ver `firebase/firestore.rules`);
 * - nadie cambia su propio rol (nadie se asigna staff a sí mismo, y el admin no se quita el permiso por error);
 * - las cuentas ADMIN no se tocan y solo se asigna ESTUDIANTE o STAFF;
 * - queda en la auditoría (ROLE_CHANGED: "Estudiante → Staff"), en la misma transacción.
 */
class ChangeUserRoleUseCase(
    private val users: UserRepository,
    private val audit: AuditRecorder,
    private val tx: TransactionRunner,
    private val getCurrentUser: GetCurrentUserUseCase
) {
    suspend operator fun invoke(userId: String, role: Role): Result<UserModel> = runCatching {
        val actor = getCurrentUser()
        if (actor?.role != Role.ADMIN) throw RoleChangeException(RoleErrors.ONLY_ADMIN)
        val target = users.findById(userId) ?: throw RoleChangeException(RoleErrors.NOT_FOUND)
        when {
            target.id == actor.id -> throw RoleChangeException(RoleErrors.SELF)
            target.role == Role.ADMIN -> throw RoleChangeException(RoleErrors.ADMIN_LOCKED)
            role !in ASSIGNABLE_ROLES -> throw RoleChangeException(RoleErrors.NOT_ASSIGNABLE)
            target.role == role -> throw RoleChangeException(RoleErrors.alreadyHas(target))
        }
        tx.run {
            users.updateRole(target.id, role)
            audit.record(AuditAction.ROLE_CHANGED, actor.id, target.id, "${target.role.label} → ${role.label}")
        }
        target.copy(role = role)
    }
}

/** S03: "Asignar staff por correo" del panel de admin. */
class AssignStaffByEmailUseCase(
    private val users: UserRepository,
    private val changeRole: ChangeUserRoleUseCase
) {
    suspend operator fun invoke(rawEmail: String): Result<UserModel> {
        // Email no acepta texto vacío: se valida antes de construirlo.
        if (rawEmail.isBlank()) return Result.failure(RoleChangeException(RoleErrors.EMAIL_EMPTY))
        val email = Email(rawEmail.trim().lowercase())
        if (!email.isInstitutional()) return Result.failure(RoleChangeException(RoleErrors.EMAIL_NOT_INSTITUTIONAL))
        val user = users.findByEmail(email) ?: return Result.failure(RoleChangeException(RoleErrors.NOT_FOUND))
        return changeRole(user.id, Role.STAFF)
    }
}
