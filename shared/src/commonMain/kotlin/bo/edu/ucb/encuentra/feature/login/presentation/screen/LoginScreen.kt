package bo.edu.ucb.encuentra.feature.login.presentation.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import bo.edu.ucb.encuentra.feature.login.presentation.viewmodel.LoginEffect
import bo.edu.ucb.encuentra.feature.login.presentation.viewmodel.LoginEvent
import bo.edu.ucb.encuentra.feature.login.presentation.viewmodel.LoginState
import bo.edu.ucb.encuentra.feature.login.presentation.viewmodel.LoginStep
import bo.edu.ucb.encuentra.feature.login.presentation.viewmodel.LoginViewModel
import bo.edu.ucb.encuentra.resources.Res
import bo.edu.ucb.encuentra.resources.ucb_escudo
import edu.ucb.ds.components.buttons.PrimaryButton
import edu.ucb.ds.components.cards.AppCard
import edu.ucb.ds.components.header.CurvedHeaderContainer
import edu.ucb.ds.components.label.AppBody
import edu.ucb.ds.components.label.AppHeading
import edu.ucb.ds.components.surfaces.AppSurface
import edu.ucb.ds.components.textfields.AppTextField
import edu.ucb.ds.icons.DsIcons
import edu.ucb.ds.theme.UcbEncuentraTheme
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

/** Login institucional (Figma "Login Estudiante"): correo → Continuar → contraseña → Continuar. */
@Composable
fun LoginScreen(
    viewModel: LoginViewModel = koinViewModel(),
    onNavigateToHome: () -> Unit,
    onOpenSupport: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is LoginEffect.NavigateToHome -> onNavigateToHome()
                is LoginEffect.OpenSupport -> onOpenSupport(effect.uri)
            }
        }
    }

    AppSurface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            InstitutionalHeader()
            LoginCard(
                state = state,
                onEvent = viewModel::handleEvent,
                modifier = Modifier
                    .offset(y = (-32).dp)
                    .padding(horizontal = UcbEncuentraTheme.spacing.screenPadding)
                    .widthIn(max = 448.dp)
            )
            SupportFooter(onContactSupport = { viewModel.handleEvent(LoginEvent.OnContactSupport) })
        }
    }
}

/** Header alto con la onda, el escudo y "UCB Encuentra". */
@Composable
private fun InstitutionalHeader() {
    CurvedHeaderContainer(minHeight = 280.dp, roundedBottom = false) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, bottom = UcbEncuentraTheme.spacing.screenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.large)
        ) {
            Image(
                painter = painterResource(Res.drawable.ucb_escudo),
                contentDescription = "Escudo de la Universidad Católica Boliviana",
                modifier = Modifier.size(width = 82.dp, height = 110.dp)
            )
            AppHeading(
                text = "UCB Encuentra",
                color = UcbEncuentraTheme.colors.onPrimary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LoginCard(
    state: LoginState,
    onEvent: (LoginEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = UcbEncuentraTheme.spacing
    AppCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            AppHeading("Iniciar Sesión", textAlign = TextAlign.Center)
            AppBody("Ingresa con tu correo institucional\n@ucb.edu.bo", textAlign = TextAlign.Center)
        }

        when (state.step) {
            LoginStep.EMAIL -> AppTextField(
                value = state.email,
                onValueChange = { onEvent(LoginEvent.OnEmailChanged(it)) },
                placeholder = "correo@ucb.edu.bo",
                leadingIcon = Icons.Outlined.MailOutline,
                errorMessage = state.emailError,
                containerColor = UcbEncuentraTheme.colors.surface,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
            )
            LoginStep.PASSWORD -> PasswordStep(state = state, onEvent = onEvent)
        }

        PrimaryButton(
            text = if (state.loading) "Ingresando…" else "Continuar",
            onClick = { onEvent(LoginEvent.OnContinue) },
            enabled = !state.loading,
            trailingIcon = if (state.loading) null else DsIcons.ArrowForward,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Paso 2: el correo elegido (con "Cambiar") y el campo de contraseña. S02 lo reemplaza por Google UCB. */
@Composable
private fun PasswordStep(state: LoginState, onEvent: (LoginEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.small)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppBody(
                text = state.email,
                color = UcbEncuentraTheme.colors.onSurface,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { onEvent(LoginEvent.OnChangeEmail) }, enabled = !state.loading) {
                Text("Cambiar", style = UcbEncuentraTheme.typography.label, color = UcbEncuentraTheme.colors.tertiary)
            }
        }
        AppTextField(
            value = state.password,
            onValueChange = { onEvent(LoginEvent.OnPasswordChanged(it)) },
            placeholder = "Contraseña",
            leadingIcon = Icons.Outlined.Lock,
            errorMessage = state.passwordError,
            enabled = !state.loading,
            containerColor = UcbEncuentraTheme.colors.surface,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            visualTransformation = PasswordVisualTransformation()
        )
    }
}

@Composable
private fun SupportFooter(onContactSupport: () -> Unit) {
    Column(
        modifier = Modifier.padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(UcbEncuentraTheme.spacing.large))
        AppBody("¿Problemas para ingresar?", textAlign = TextAlign.Center)
        Text(
            text = "Contacta a Sistemas UCB",
            style = UcbEncuentraTheme.typography.body.copy(fontWeight = UcbEncuentraTheme.typography.label.fontWeight),
            color = UcbEncuentraTheme.colors.tertiary,
            modifier = Modifier
                .clickable(onClick = onContactSupport)
                .padding(UcbEncuentraTheme.spacing.small)
        )
    }
}
