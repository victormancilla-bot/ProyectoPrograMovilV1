# Guía Completa: Desarrollo de Apps Móviles con Kotlin/Compose y KMP
### De lo más fácil a lo más difícil — basada en tus diapositivas y notas de pizarrón

---

## Índice
1. Estructura de un proyecto (KMP)
2. Flavors y configuración de builds
3. Pantalla de Sign In (Compose básico)
4. Componentes personalizados: Button y TextField
5. FlowRow y FlowColumn
6. Card (tarjetas)
7. Scroll
8. Toast
9. Snackbar
10. Intent y `putExtra` (navegación entre Activities)
11. Arquitectura limpia: Data / Domain / Presentation
12. MVVM vs MVI (arquitectura de estado)
13. Inyección de dependencias con Koin (KMP)
14. Koin + ViewModel

---

## 1. Estructura de un proyecto (Project Structure)

Cuando abres un proyecto **KMP (Kotlin Multiplatform)** en Android Studio verás esta estructura (tal como en tu diapositiva de Flavors):

```
📁 .gradle
📁 .idea
📁 .kotlin
📦 androidApp          → código específico de Android
📁 build
📁 gradle
📁 iosApp              → código específico de iOS
📦 shared              → código COMPARTIDO entre Android e iOS
   .gitignore
   build.gradle.kts
   gradle.properties
   gradlew / gradlew.bat
   local.properties
   README.md
   settings.gradle.kts
📁 External Libraries
📁 Scratches and Consoles
```

**Idea clave:** todo lo que se pueda reutilizar entre Android e iOS (lógica de negocio, modelos, repositorios, ViewModels) va en `shared`. Lo que es exclusivo de la plataforma (por ejemplo `MainActivity.kt`) va en `androidApp` o `iosApp`.

---

## 2. Flavors y configuración de builds (Build Variants)

Los **Flavors** te permiten tener distintas versiones de tu app (por ejemplo `dev` y `prod`) desde el mismo código.

### Paso 1: Declarar la dimensión y los flavors
En `build.gradle.kts` del módulo `androidApp`:

```kotlin
flavorDimensions += "environment"
productFlavors {
    create("dev") {
        dimension = "environment"
        applicationIdSuffix = ".dev"
        resValue(type = "string", name = "app_name", value = "app-dev")
    }
    create("prod") {
        dimension = "environment"
        applicationIdSuffix = ".app"
        resValue(type = "string", name = "app_name", value = "app-prod")
    }
}
```

### Paso 2: Elegir el Build Variant
En Android Studio → panel **Build Variants** → seleccionas el módulo `:androidApp` y eliges, por ejemplo, `devDebug` o `prodDebug`.

### Paso 3: Variables por flavor con `buildConfigField`
Sirve para tener, por ejemplo, distintas URLs de API según el ambiente:

```kotlin
buildFeatures {
    buildConfig = true   // obligatorio para poder usar BuildConfig
}

flavorDimensions += "environment"
productFlavors {
    create("dev") {
        dimension = "environment"
        val appName = "\"app-dev\""
        val baseUrl = "\"https://dev.api.calyrsoft.com\""
        applicationIdSuffix = ".dev"
        buildConfigField(type = "String", name = "APP_NAME", value = appName)
        buildConfigField(type = "String", name = "BASE_URL", value = baseUrl)
    }
    create("prod") {
        dimension = "environment"
        applicationIdSuffix = ".app"
        buildConfigField(type = "String", name = "APP_NAME", value = "\"app-prod\"")
        buildConfigField(type = "String", name = "BASE_URL", value = "\"http://prod.api.calyrsoft.com\"")
    }
}
```

### Paso 4: Usar las variables generadas
```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Log.d(tag = "CONFIG_TEST", msg = BuildConfig.GLOBAL_PROVIDER)
        Log.d(tag = "CONFIG_TEST", msg = BuildConfig.BASE_URL)
        Log.d(tag = "CONFIG_TEST", msg = BuildConfig.APP_NAME)
        setContent { App() }
    }
}
```

**Verificación:** corres la app en modo **Debug** y revisas el **Logcat** filtrando por `package:mine CONFIG_TEST`. Ahí verás los valores impresos (ej. `app-dev`, la URL, etc.) según el flavor activo.

> ⚠️ Cuidado con copiar y pegar: en tu diapositiva se ve un error típico donde el flavor `prod` quedó con `val appName = "\"app-dev\""` (mal copiado). Siempre revisa que cada flavor tenga **sus propios valores**.

---

## 3. Pantalla de Sign In (Compose básico)

Este es el punto de partida para aprender Jetpack Compose: una pantalla simple con `Column`, `TextField` y `Button`.

```kotlin
@Composable
fun SigninPage() {
    var userSignIn by remember { mutableStateOf("") }
    var passwordSignIn by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Aquí van los TextField de email y password
        // y el botón "Sign In"
    }
}
```

**Conceptos clave que introduce:**
- `remember { mutableStateOf(...) }` → estado local que sobrevive a recomposiciones.
- `Modifier.fillMaxSize()`, `.padding()` → cómo dimensionar y espaciar composables.
- `Arrangement` y `Alignment` → cómo se acomodan los elementos dentro del `Column`.

---

## 4. Componentes personalizados: Button y TextField

### 4.1 Strings en `strings.xml` (buena práctica)
Nunca escribas texto "a fuego" en el composable; usa recursos de string:

```xml
<resources>
    <string name="app_name">ucbapp</string>
    <string name="signin_button">Sign In</string>
    <string name="signin_title">Sign In</string>
</resources>
```

### 4.2 Botón personalizado
```kotlin
Button(modifier = Modifier.fillMaxWidth(), onClick = { /*TODO*/ }) {
    Text(text = stringResource(R.string.signin_button))
}
```

### 4.3 TextField personalizado
```kotlin
TextField(
    label = { Text(text = "User") },
    singleLine = true,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
    placeholder = { Text(text = "placeholder") }
)
```

**Puntos importantes:** `singleLine = true` evita saltos de línea, `keyboardOptions` controla el tipo de teclado (texto, email, número, etc.), y `label`/`placeholder` mejoran la UX.

---

## 5. FlowRow y FlowColumn

`FlowRow` y `FlowColumn` son como `Row` y `Column`, pero **cuando se acaba el espacio, el contenido salta a la siguiente línea/columna** automáticamente. Muy útil para chips, etiquetas, galerías responsivas, etc.

### 5.1 Ejemplo básico de FlowRow
```kotlin
@Composable
private fun FlowRowSimpleUsageExample() {
    FlowRow(modifier = Modifier.padding(8.dp)) {
        ChipItem("Price: High to Low")
        ChipItem("Avg rating: 4+")
        ChipItem("Free breakfast")
        ChipItem("Free cancellation")
        ChipItem("£50 pn")
    }
}
```

### 5.2 Controlando la disposición
- **Eje principal (horizontal en FlowRow):** `Arrangement.Start`, `SpaceBetween`, `Center`, `End`, `SpaceAround`, `spacedBy(8.dp)`.
- **Eje cruzado (vertical en FlowRow):** `Alignment.Top`, `Bottom`, `CenterVertically`.
- **Máximo de elementos por fila/columna:** `maxItemsInEachRow` o `maxItemsInEachColumn`.

```kotlin
FlowRow(
    modifier = Modifier.padding(4.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    maxItemsInEachRow = rows
) {
    val itemModifier = Modifier
        .padding(4.dp)
        .height(80.dp)
        .weight(1f)
        .clip(RoundedCornerShape(8.dp))
        .background(Color.Blue)

    repeat(rows * columns) {
        Spacer(modifier = itemModifier)
    }
}
```

### 5.3 Caso especial: un elemento distinto en la fila (ej. el 3ro ocupa todo el ancho)
```kotlin
repeat(6) { item ->
    // si es el tercer elemento, usar fillMaxWidth en vez de weight
    if ((item + 1) % 3 == 0) {
        Spacer(modifier = itemModifier.fillMaxWidth())
    } else {
        Spacer(modifier = itemModifier.weight(0.5f))
    }
}
```

### 5.4 FlowColumn
```kotlin
val listDesserts = listOf(
    "Apple", "Banana", "Cupcake", "Donut", "Eclair", "Froyo", "Gingerbread",
    "Honeycomb", "Ice Cream Sandwich", "Jellybean", "KitKat", "Lollipop",
    "Marshmallow", "Nougat"
)

FlowColumn(
    Modifier
        .padding(20.dp)
        .fillMaxHeight()
        .fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
    maxItemsInEachColumn = 7,
) {
    repeat(listDesserts.size) {
        Box(
            modifier = Modifier
                .border(1.dp, Color.DarkGray, RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            Text(
                text = listDesserts[it],
                fontSize = 12.sp,
                modifier = Modifier.padding(3.dp)
            )
        }
    }
}
```

---

## 6. Card (tarjetas)

Card muestra información dentro de "tarjetas" con estilo consistente. Existen 3 tipos: **Elevated**, **Filled** (Card normal) y **Outlined**.

Estructura recomendada (según tu diapositiva): crea una función `CardMain()` dentro de un `Column` y ahí agrupas los ejemplos.

```kotlin
@Composable
@Preview
fun App() {
    MaterialTheme {
        CardMain()
    }
}

@Composable
fun CardMain() {
    Column { }
}
```

### 6.1 Card simple
```kotlin
Card {
    Text(text = "Hello Android Compose")
}
```

### 6.2 Card con color, ancho y alto personalizados
```kotlin
Card(
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ),
    modifier = Modifier.size(width = 240.dp, height = 100.dp)
) {
    Text(text = "Card with Width and Height")
}
```

### 6.3 Card ocupando todo el ancho
```kotlin
Card(modifier = Modifier.fillMaxWidth()) {
    Text(
        text = "Card fill max width",
        modifier = Modifier.padding(16.dp),
        textAlign = TextAlign.Center
    )
}
```

### 6.4 ElevatedCard
```kotlin
ElevatedCard {
    Text(text = "Elevated", modifier = Modifier.padding(16.dp), textAlign = TextAlign.Center)
}
```

### 6.5 OutlinedCard
```kotlin
OutlinedCard(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, Color.Black),
    modifier = Modifier.size(width = 240.dp, height = 100.dp)
) {
    Text(text = "Outlined", modifier = Modifier.padding(16.dp), textAlign = TextAlign.Center)
}
```

---

## 7. Scroll

Permite hacer scroll vertical cuando el contenido no cabe en la pantalla (aparece la típica barra lateral).

```kotlin
@Composable
fun ScrollExample(modifier: Modifier) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.LightGray)
            .verticalScroll(rememberScrollState())
    ) {
        repeat(100) {
            Text(text = "Item $it", modifier = Modifier.padding(2.dp))
        }
    }
}
```

> Nota de tus diapositivas: para probarlo, comenta la llamada a `CardExample(...)` en el `Scaffold` de `MainActivity` y llama en su lugar a `ScrollExample(modifier = Modifier.padding(innerPadding))`.

---

## 8. Toast

Mensaje breve emergente. Necesita **contexto**, **texto** y **duración**.

```kotlin
val text = "Hello toast!"
val duration = Toast.LENGTH_SHORT
val toast = Toast.makeText(this, text, duration) // dentro de una Activity
toast.show()
```

### 8.1 Cómo obtener el contexto (opción 1): pasarlo como parámetro
```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val context = applicationContext
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FrankyAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CardExample(modifier = Modifier.padding(innerPadding), context = context)
                }
            }
        }
    }
}
```
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardExample(modifier: Modifier, context: Context) {
    // ...dentro de un OutlinedCard...
    OutlinedCard(
        onClick = {
            Toast.makeText(context, text = "Texto", Toast.LENGTH_LONG).show()
        },
        // ...
    ) { /* contenido */ }
}
```

### 8.2 Cómo obtener el contexto (opción 2, más simple): `LocalContext.current`
Solo se puede usar **dentro de una función `@Composable`**.

```kotlin
@Composable
fun CardExample(modifier: Modifier) {
    val contextLocal = LocalContext.current
    // ...
    OutlinedCard(
        onClick = {
            Toast.makeText(contextLocal, text = "Texto", Toast.LENGTH_SHORT).show()
        }
    ) { /* contenido */ }
}
```

---

## 9. Snackbar

Es una notificación breve en la parte inferior de la pantalla que **no interrumpe** al usuario y puede tener una acción (ej. "Deshacer"). Usos típicos: confirmar una acción, avisar pérdida de conexión, confirmar guardado de datos.

### Paso 1: Crear el `SnackbarHostState` y el `CoroutineScope`
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnackbarExample() {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            ElevatedCard(
                onClick = {
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = "Snackbar",
                            actionLabel = "Action",
                            duration = SnackbarDuration.Indefinite
                        )
                        when (result) {
                            SnackbarResult.ActionPerformed -> {
                                // el usuario tocó el botón "Action"
                            }
                            SnackbarResult.Dismissed -> {
                                // se cerró sin acción
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { /* contenido de la tarjeta */ }
        }
    }
}
```

**Conceptos clave:** `showSnackbar()` es una función `suspend`, por eso necesita un `CoroutineScope` (`rememberCoroutineScope()`) y se llama dentro de `scope.launch { }`. El `when(result)` te permite reaccionar según si el usuario tocó la acción o simplemente se cerró.

---

## 10. Intent y `putExtra` (navegar entre Activities y pasar datos)

### Activity origen: crear el Intent y enviar datos
```kotlin
fun openSecondActivity(context: Context) {
    val intent = Intent(context, GitUiActivity::class.java)
    intent.putExtra("name", "calyr")
    context.startActivity(intent)
}
```

### Activity destino: recuperar el dato enviado
```kotlin
class GitUiActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val context = applicationContext
        val name = intent.extras?.getString("name")
        Toast.makeText(context, name, Toast.LENGTH_LONG).show()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { /* ... */ }
    }
}
```

**Ejemplo combinado (de tu diapositiva de ejercicio):** dentro del `ActionPerformed` del Snackbar, se llama a `openSecondActivity(context)`, es decir, se puede usar el resultado de una interacción (como un Snackbar) para disparar la navegación.

```kotlin
SnackbarResult.ActionPerformed -> {
    openSecondActivity(context)
}
```

---

## 11. Arquitectura limpia: Data / Domain / Presentation

Esto es lo que se ve en las fotos del pizarrón: la organización de un **feature** completo (ejemplo: `Login`) siguiendo **Clean Architecture**. Es más difícil que lo anterior porque implica pensar en capas y responsabilidades, no solo en UI.

### 11.1 Las "5 pantallas" / pasos para nombrar un feature (pizarrón, foto 1)
1. **Nombre al feature** (ej. `Login`, `Search`, `Signin`).
2. **Capa de presentación**
   - `Screen` (pausa)
   - `state`
   - `Composable` (pausa)
   - → dentro de esta capa: **events, effects, state, viewmodel**
3. **Capa de dominio**
   - `VO` (Value Object)
   - `model`
   - `repository` (interfaz)
   - `usecase`

### 11.2 Estructura completa de un feature (pizarrón, foto 2 y 3 — ejemplo `Login`)

```
Login
├── data
│   └── datasource
│       ├── local
│       └── remota (remote)
├── domain
│   ├── usecase
│   ├── repository (interfaz)
│   ├── model
│   └── vo
│       → data class User(val name: String, val lastname: String)
└── presentation
    ├── composable → Revibable (botón, card...)  [reutilizables/reusable]
    ├── screen  → LoginScreen.kt        (View)
    └── viewmodel
        ├── state  → LoginState.kt
        ├── viewmodel → LoginViewModel.kt   (ViewModel)
        ├── effect
        └── event
```

**Explicación de cada capa:**

- **`data`**: se encarga de **obtener** los datos, desde una fuente **local** (base de datos, cache) o **remota** (API/red). Aquí se implementan los repositorios definidos en `domain`.
- **`domain`**: es el corazón del negocio, **no depende de Android ni de Compose**.
  - `usecase`: una acción concreta del negocio (ej. `AuthenticateUseCase`).
  - `repository`: **interfaz** (contrato) que `data` implementa.
  - `model` / `vo`: los objetos de negocio, ej. `data class User(val name: String, val lastname: String)`.
- **`presentation`**: todo lo relacionado a la UI.
  - `screen`: el `@Composable` que arma la pantalla completa (la "View"), ej. `LoginScreen.kt`.
  - `composable`: componentes reutilizables (botones, cards) — se marcan `@Composable` y deben ser `Revibable` (revisables/"stateless" reutilizables).
  - `viewmodel`: contiene el `ViewModel`, el `State`, los `Event` y los `Effect`.

### 11.3 Ejemplo de pantalla adicional del pizarrón: `Search`
```
presentacion
├── viewmodel → SearchViewModel.kt
└── screen → SearchScreen.kt
```
Esto confirma el mismo patrón: cada feature nuevo (Search, Login, etc.) repite la misma estructura de carpetas.

### 11.4 Boceto de UI (pizarrón foto 3)
El pizarrón muestra un boceto de pantalla con:
- Campo **"nickname"**
- Campo **"final"**
- Un contenedor **"Avatar"**
- Etiqueta **"calyr"**

Esto es simplemente el wireframe/mockup que se traduce después a un `@Composable Screen`.

---

## 12. MVVM vs MVI (arquitectura de manejo de estado)

Este es el nivel más conceptual/difícil: cómo estructurar el **flujo de datos** dentro de la capa `presentation`.

### 12.1 El problema en KMP
En Kotlin Multiplatform, Android e iOS normalmente duplican la lógica de UI. La solución es tener una **lógica de UI compartida** en el módulo común (KMP), usando **MVVM** o **MVI**.

### 12.2 MVVM — Flujo de Estado Reactivo
```
VIEW (Compose/SwiftUI)
   --Click/Evento-->
VIEWMODEL (KMP Common)
   [Lógica de Negocio / StateFlow]
   --> MODEL (Repository/Local/Network)
   <-- STATE (UIState) -- vuelve a la VIEW
```
- **EVENTS**: Button Clicks → StateFlow
- **EFFECTS**: Navigate to Screen (navegación, efectos únicos)

### 12.3 MVI — Un Flujo Unidireccional (más estricto que MVVM)
```
VIEW (Compose/SwiftUI)
   --INTENT-->
VIEWMODEL/REDUCER (KMP Common)
   [Procesar Intent] --> [Nuevo Estado]
   --> MODEL/SIDE EFFECTS --RESULTADO-->
   --> ESTADO (Single State) -- vuelve a la VIEW
```
- **INTENTS**: User Action → Reducer
- **EFFECTS**: Show Toast, Network Call → Side Effects
- Todo el estado de la pantalla vive en **un solo objeto** ("Single State"), a diferencia de MVVM que puede tener varios `StateFlow`.

### 12.4 Comparación rápida
| | MVVM | MVI |
|---|---|---|
| Entrada de usuario | Events (Button Clicks) | Intents (User Action) |
| Procesamiento | StateFlow | Reducer |
| Salida "silenciosa" | Effects → Navigate to Screen | Effects → Show Toast, Network Call (Side Effects) |
| Estado | Puede ser varios estados | **Single State** (un solo estado) |

### 12.5 Arquitectura KMP unificada (compartiendo UI y lógica)
```
COMMON KMP MODULE
├── VIEWMODELS (Shared Flow / StateFlow) → interfaz KMP → Android (Compose) / iOS (SwiftUI)
├── REPOSITORIES
└── NETWORK/DB

SHARED STATE  ←→  COMMON KMP MODULE  ←→  BUSINESS LOGIC
```
La idea es que **ViewModels, Repositories y Network/DB** viven en el módulo común, y tanto Android como iOS solo implementan la "PLATFORM UI" (Compose o SwiftUI) que consume esos ViewModels a través de una interfaz KMP.

### 12.6 Implementación real de MVI en Login (código de tus diapositivas)

**El estado (Single State):**
```kotlin
class LoginState(
    val email: String,
    val password: String,
    val loading: Boolean = false,
    val error: String? = null
)
```

**Los efectos (side effects, no forman parte del estado):**
```kotlin
interface LoginEffect
```

**Los eventos/intents (lo que el usuario puede hacer):**
```kotlin
interface LoginEvent {
    object OnSubmit : LoginEvent
    data class OnChangePassword(val value: String) : LoginEvent
    data class OnError(val message: String) : LoginEvent
}
```

**El ViewModel completo (versión MVVM con Flow, tal como en tu diapositiva de Koin+ViewModel):**
```kotlin
class LoginViewModel : ViewModel() {
    private val _state = MutableStateFlow(LoginState())
    val state = _state.asStateFlow()

    private val _effects = MutableSharedFlow<LoginEffects>()
    val effects = _effects.asSharedFlow()

    private fun emitEffect(effect: LoginEffects) {
        viewModelScope.launch { _effects.emit(effect) }
    }

    fun emitEvent(event: LoginEvents) {
        when (event) {
            is LoginEvents.OnEmailChanged -> { /* actualizar state */ }
            is LoginEvents.OnPasswordChanged -> { /* actualizar state */ }
            LoginEvents.OnSubmit -> {
                var isValid = true
                if (state.value.userName.isBlank()) {
                    emitEffect(LoginEffects.ShowToast("The email field is required"))
                    isValid = false
                } else if (state.value.password.isBlank()) {
                    emitEffect(LoginEffects.ShowToast("The password field is required"))
                }
                if (isValid) {
                    emitEffect(LoginEffects.NavigateToHome)
                }
            }
        }
    }
}
```

**Consumiendo el ViewModel desde la Screen:**
```kotlin
@Composable
fun LoginScreen(viewModel: LoginViewModel = koinViewModel()) {
    val state = viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                LoginEffects.NavigateToHome -> TODO()
                is LoginEffects.ShowToast -> println("ERROR ${effect.message}")
                LoginEffects.SignUp -> TODO()
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(value = state.value.email, onValueChange = {
            viewModel.emitEvent(LoginEvents.OnEmailChanged(it))
        })
        Button(onClick = { viewModel.emitEvent(LoginEvents.OnSubmit) }) {
            Text("Login")
        }
    }
}
```

> **Por qué es importante:** `state` se **observa** (`collectAsState()`), mientras que `effects` se **escucha una sola vez por evento** con `LaunchedEffect` + `collect` — esto evita, por ejemplo, que un Toast se repita al rotar la pantalla.

---

## 13. Inyección de dependencias con Koin (KMP)

Este paso es más avanzado porque conecta **todas las capas anteriores** (data, domain, presentation) entre sí, sin que una clase tenga que crear manualmente sus dependencias.

### Paso 1 — Declarar versiones en `libs.versions.toml`
```toml
[versions]
koinBom = "4.2.2"

[libraries]
koin-bom = { module = "io.insert-koin:koin-bom", version.ref = "koinBom" }
koin-compose = { module = "io.insert-koin:koin-compose", version.ref = "koinBom" }
koin-compose-viewmodel = { module = "io.insert-koin:koin-compose-viewmodel", version.ref = "koinBom" }
koin-core = { module = "io.insert-koin:koin-core", version.ref = "koinBom" }
```
Sincroniza el proyecto.

### Paso 2 — Agregar dependencias en `build.gradle.kts` (módulo `shared`)
```kotlin
commonMain.dependencies {
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.components.resources)
    implementation(libs.compose.uiToolingPreview)
    implementation(libs.androidx.lifecycle.viewmodelCompose)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.koin.core)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)
}
```
Sincroniza de nuevo.

### Paso 3 — Crear el paquete `di` en `shared/src/commonMain/kotlin/[tupaquete]/di`

**`KoinInit.kt`**
```kotlin
// NO DEBES ELIMINAR EL PAQUETE DE TU APLICACION
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        appDeclaration()
        modules(sharedModules())
    }
}
```

**`SharedModules.kt`**
```kotlin
import org.koin.core.module.Module

fun sharedModules(): List<Module> = listOf(
    dataModule,
    presentationModule,
    domainModule
)
```

**`PresentationModule.kt`**
```kotlin
val presentationModule = module { }
```

**`DomainModule.kt`**
```kotlin
val domainModule = module { }
```

**`DataModule.kt`**
```kotlin
val dataModule = module { }
```

### Paso 4 — Inicializar Koin en Android
**`KoinModules.android.kt`** en `shared/src/androidMain/kotlin/[tupaquete]/di`
```kotlin
import android.content.Context
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

fun initKoinAndroid(context: Context) {
    startKoin {
        androidContext(context)
        androidLogger()
        modules(sharedModules())
    }
}
```

### Paso 5 — Inicializar Koin en iOS
**`KoinModules.ios.kt`** en `shared/src/iosMain/kotlin/[tupaquete]/di`
```kotlin
fun initKoinIos() {
    initKoin()
}
```

### Paso 6 — Crear `MainApplication.kt` en el módulo `androidApp`
```kotlin
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoinAndroid(this)
    }
}
```

### Paso 7 — Registrar `MainApplication` en `AndroidManifest.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:name=".MainApplication"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">
        <activity
            android:exported="true"
            android:name=".MainActivity">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

### Paso 8 — Compilar el proyecto

---

## 14. Koin + ViewModel (el paso final: unir todo)

Una vez que Koin está inicializado, puedes inyectar automáticamente tus `ViewModel`, `UseCase` y `Repository`.

### 14.1 Registrar un ViewModel en el módulo de presentación
```kotlin
val presentationModule = module {
    viewModelOf(::ProductDetailViewModel)
    // o, para Login:
    // viewModelOf(::LoginViewModel)
}
```

### 14.2 Registrar un UseCase en el módulo de dominio (ejemplo real)
```kotlin
// AuthenticateUseCase.kt
class AuthenticateUseCase {
    suspend fun invoke(email: Email, password: Password): Result<Boolean> {
        return if (email.value == "calyr.software@gmail.com" && password.value == "123456") {
            Result.success(true)
        } else {
            Result.failure(Exception("Invalid credential"))
        }
    }
}

// DomainModule.kt
val domainModule = module {
    singleOf(::AuthenticateUseCase)
}
```

### 14.3 Usar el ViewModel inyectado en el Composable
```kotlin
@Composable
fun ProductDetailScreen(
    modifier: Modifier,
    viewModel: ProductDetailViewModel = koinViewModel()
) {
    // usas viewModel.state, viewModel.emitEvent(...), etc.
}
```

**Por qué esto cierra el círculo:** `koinViewModel()` le pide a Koin que le entregue una instancia del ViewModel ya armada (con sus dependencias, como el `UseCase`, inyectadas automáticamente), sin que tú tengas que escribir `LoginViewModel(AuthenticateUseCase())` a mano.

---

## Resumen visual del recorrido completo

```
1. Estructura de proyecto (KMP: androidApp / iosApp / shared)
        ↓
2. Flavors (dev/prod) + BuildConfig
        ↓
3-4. UI básica: Column, TextField, Button + strings.xml
        ↓
5-9. Componentes de UI: FlowRow/FlowColumn, Card, Scroll, Toast, Snackbar
        ↓
10. Navegación entre pantallas: Intent + putExtra
        ↓
11. Arquitectura limpia: data / domain / presentation
        ↓
12. Patrón de estado: MVVM o MVI (State, Event, Effect, ViewModel)
        ↓
13. Koin: inyección de dependencias (di: dataModule, domainModule, presentationModule)
        ↓
14. Koin + ViewModel: todo conectado y funcionando end-to-end
```

### Consejo final para alguien que recién empieza
Sigue este orden al construir tu primer feature real (por ejemplo, un Login):
1. Crea la pantalla visual primero, sin lógica (solo `Column` + `TextField` + `Button`).
2. Agrega el `LoginState`, `LoginEvent` y `LoginEffect`.
3. Crea el `LoginViewModel` que procese esos eventos y emita esos efectos.
4. Crea las carpetas `data`, `domain`, `presentation` dentro de tu feature.
5. Define la interfaz `LoginRepository` en `domain` y su implementación en `data`.
6. Crea el `di` con los tres módulos (`dataModule`, `domainModule`, `presentationModule`) y registra ahí tu ViewModel y tus UseCases con Koin.
7. Conecta la `LoginScreen` con `viewModel: LoginViewModel = koinViewModel()`.

Con esto tienes exactamente el flujo que se ve reflejado en todas tus diapositivas y en el pizarrón. ¡Éxitos!
