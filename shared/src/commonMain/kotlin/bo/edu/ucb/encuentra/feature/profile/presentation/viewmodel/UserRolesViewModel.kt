package bo.edu.ucb.encuentra.feature.profile.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.edu.ucb.encuentra.core.domain.model.Role
import bo.edu.ucb.encuentra.core.domain.model.UserModel
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.feature.profile.domain.usecase.AssignStaffByEmailUseCase
import bo.edu.ucb.encuentra.feature.profile.domain.usecase.ChangeUserRoleUseCase
import bo.edu.ucb.encuentra.feature.profile.domain.usecase.ObserveUsersUseCase
import bo.edu.ucb.encuentra.feature.profile.domain.usecase.label
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Filtro de la lista de cuentas. */
enum class RoleFilter(val label: String, val role: Role?) {
    TODOS("Todos", null),
    ESTUDIANTES("Estudiantes", Role.ESTUDIANTE),
    STAFF("Staff", Role.STAFF),
    ADMIN("Admin", Role.ADMIN)
}

/** Cambio que espera confirmación (quitar staff). */
data class PendingRoleChange(val user: UserModel, val newRole: Role)

data class UserRolesState(
    val loading: Boolean = true,
    val error: String? = null,
    val users: List<UserModel> = emptyList(),
    val currentUserId: String? = null,
    val filter: RoleFilter = RoleFilter.TODOS,
    val email: String = "",
    val emailError: String? = null,
    val working: Boolean = false,
    val pending: PendingRoleChange? = null,
    val message: String? = null
) {
    val visibleUsers: List<UserModel>
        get() = users.filter { filter.role == null || it.role == filter.role }

    fun countOf(role: Role) = users.count { it.role == role }

    /** Se puede quitar staff a otra cuenta STAFF (nunca a uno mismo ni a un admin). */
    fun canRemoveStaff(user: UserModel) = user.role == Role.STAFF && user.id != currentUserId

    /** Se puede dar staff a un estudiante desde la lista (además de por correo). */
    fun canMakeStaff(user: UserModel) = user.role == Role.ESTUDIANTE && user.id != currentUserId
}

sealed class UserRolesEvent {
    data class OnEmailChanged(val value: String) : UserRolesEvent()
    data object OnAssignStaff : UserRolesEvent()
    data class OnFilterChanged(val filter: RoleFilter) : UserRolesEvent()
    data class OnRequestChange(val user: UserModel, val newRole: Role) : UserRolesEvent()
    data object OnConfirmChange : UserRolesEvent()
    data object OnDismissChange : UserRolesEvent()
    data object OnMessageShown : UserRolesEvent()
    data object OnBack : UserRolesEvent()
}

sealed class UserRolesEffect {
    data object NavigateBack : UserRolesEffect()
}

/** S03: panel del admin para asignar staff por correo y quitarlo (con confirmación). */
class UserRolesViewModel(
    private val observeUsers: ObserveUsersUseCase,
    private val assignStaff: AssignStaffByEmailUseCase,
    private val changeRole: ChangeUserRoleUseCase,
    getCurrentUser: GetCurrentUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(UserRolesState(currentUserId = getCurrentUser()?.id))
    val state: StateFlow<UserRolesState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<UserRolesEffect>()
    val effects: SharedFlow<UserRolesEffect> = _effects.asSharedFlow()

    init {
        viewModelScope.launch {
            observeUsers()
                .catch { e -> _state.update { it.copy(loading = false, error = e.message ?: "No se pudieron cargar las cuentas") } }
                .collect { users -> _state.update { it.copy(loading = false, error = null, users = users) } }
        }
    }

    fun handleEvent(event: UserRolesEvent) {
        when (event) {
            is UserRolesEvent.OnEmailChanged -> _state.update { it.copy(email = event.value, emailError = null) }
            UserRolesEvent.OnAssignStaff -> assignByEmail()
            is UserRolesEvent.OnFilterChanged -> _state.update { it.copy(filter = event.filter) }
            // Dar staff es directo; quitarlo pide confirmación (le cambia lo que ve esa persona).
            is UserRolesEvent.OnRequestChange ->
                if (event.newRole == Role.STAFF) apply(event.user, event.newRole)
                else _state.update { it.copy(pending = PendingRoleChange(event.user, event.newRole)) }
            UserRolesEvent.OnConfirmChange -> _state.value.pending?.let { apply(it.user, it.newRole) }
            UserRolesEvent.OnDismissChange -> _state.update { it.copy(pending = null) }
            UserRolesEvent.OnMessageShown -> _state.update { it.copy(message = null) }
            UserRolesEvent.OnBack -> viewModelScope.launch { _effects.emit(UserRolesEffect.NavigateBack) }
        }
    }

    private fun assignByEmail() {
        if (_state.value.working) return
        _state.update { it.copy(working = true, emailError = null) }
        viewModelScope.launch {
            assignStaff(_state.value.email)
                .onSuccess { user -> _state.update { it.copy(working = false, email = "", message = successFor(user)) } }
                .onFailure { e -> _state.update { it.copy(working = false, emailError = e.message) } }
        }
    }

    private fun apply(user: UserModel, role: Role) {
        if (_state.value.working) return
        _state.update { it.copy(working = true, pending = null) }
        viewModelScope.launch {
            changeRole(user.id, role)
                .onSuccess { updated -> _state.update { it.copy(working = false, message = successFor(updated)) } }
                .onFailure { e -> _state.update { it.copy(working = false, message = e.message) } }
        }
    }

    private fun successFor(user: UserModel) = "${user.fullName} ahora es ${user.role.label}."
}
