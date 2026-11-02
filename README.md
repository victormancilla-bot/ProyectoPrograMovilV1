# UCB Encuentra

App de objetos perdidos y encontrados de la UCB. Kotlin Multiplatform (Android + iOS) con Compose Multiplatform, Clean Architecture, MVI y Koin.

Paquete: `bo.edu.ucb.encuentra` · Flavors: `dev` (`bo.edu.ucb.encuentra.dev`) y `prod` (`bo.edu.ucb.encuentra.app`).

## Requisitos

- Android Studio (última estable) con el plugin Kotlin Multiplatform
- JDK 17 o superior
- Android SDK 37
- Para iOS: macOS con Xcode

## Primera vez

1. Clona y cámbiate a `develop`:
   ```bash
   git clone https://github.com/victormancilla-bot/ProyectoPrograMovilV1.git
   cd ProyectoPrograMovilV1
   git switch develop
   ```
2. Copia `local.properties.example` como `local.properties` y llena las claves (pídelas al Integrante A). **Nunca subas `local.properties`.**
   De ahí se genera `Secrets.kt` (`bo.edu.ucb.encuentra.core.config.Secrets`) al compilar.
3. Firebase: coloca `google-services.json` en `androidApp/`. Sin ese archivo la app compila igual, pero Firebase no se inicializa.
4. Abre el proyecto en Android Studio y haz Gradle Sync.

## Cómo correr

- **Android:** en *Build Variants* elige `devDebug` y ejecuta `androidApp`. O por consola:
  ```bash
  ./gradlew :androidApp:assembleDevDebug     # APK en androidApp/build/outputs/apk/dev/debug
  ./gradlew :androidApp:installDevDebug      # instala en el emulador/dispositivo conectado
  ```
- **iOS:** abre `iosApp/` en Xcode y ejecuta.

Usuarios de prueba (están en el seed de Room). El login pide primero el correo y, al tocar **Continuar**, la contraseña `password` (S02 la reemplazará por Google UCB):

| Correo | Rol | Entra a |
|---|---|---|
| `estudiante@ucb.edu.bo` | ESTUDIANTE | Inicio · barra: Inicio, Buscar, Reportar, Alertas, Perfil |
| `staff@ucb.edu.bo` | STAFF | Panel · barra: Panel, Inventario, Reclamos, Alertas, Perfil |
| `admin@ucb.edu.bo` | ADMIN | Igual que staff |
| `ana.gomez@ucb.edu.bo`, `luis.mendoza@ucb.edu.bo`, `carlos.mamani@ucb.edu.bo` | ESTUDIANTE | Inicio (reclamantes del seed) |

Las pantallas que no están en la barra se abren desde **Perfil → Menú de desarrollo** (solo mientras son stubs).

## Login y sesión (S01)

- Pantalla del Figma "Login Estudiante": header alto con el escudo, tarjeta blanca, **Continuar** y "Contacta a Sistemas UCB".
- Dos pasos en la misma tarjeta: (1) el correo debe ser **@ucb.edu.bo** (`ValidateInstitutionalEmailUseCase`); (2) contraseña. `AuthenticateUseCase` busca el usuario en Room (`UserRepository`) y responde "Correo o contraseña incorrectos" sin revelar qué correos existen.
- **Sesión persistente** con DataStore (`SessionRepositoryImpl`): guarda solo el id del usuario; al abrir la app se restaura desde Room y se entra directo al home del rol. **Perfil → Cerrar sesión** la borra.
- Para empezar de cero (borra sesión y base): *Ajustes → Apps → UCB Encuentra (dev) → Borrar datos*, o `adb shell pm clear bo.edu.ucb.encuentra.dev`.
- ⚠️ El enlace a Sistemas UCB abre `mailto:sistemas@ucb.edu.bo` (`LoginViewModel.SUPPORT_URI`): **es de prueba**, falta confirmar el contacto real (o leerlo de Remote Config, F08).

## Home del estudiante (L01)

- Pantalla del Figma "Home Estudiante": header con "¡Hola, {nombre}!" y "{carrera} • {sede}" tomados del usuario (no fijo), tarjeta "¿Perdiste algo?" → wizard, **Actividad** y **Accesos rápidos** (Mis reportes, Objetos encontrados, Cómo funciona).
- Actividad en vivo desde Room (`HomeActivityRepository`, `combine` de dos `Flow`): **reportes activos** = Buscando, En verificación o Match confirmado; **coincidencias nuevas** = notificaciones `NEW_MATCH` sin leer (punto rojo si hay). Con el seed: 4 y 1.
- MVI: `HomeState` / `HomeEvent` / `HomeEffect`; tests en `HomeViewModelTest`.
- Pendiente de otras tarjetas: el menú (☰) y la luna de modo oscuro del header (S07).

## Wizard de reporte (L02)

- Un solo `ReportWizardViewModel` para los 4 pasos con el borrador (`ReportDraftState` + `ReportDraft`); cancelar lo descarta (F05).
- **Paso 1 · Categoría** (Figma "Reportar - Categoría"): grilla de 6 categorías y, al elegir una, sus subcategorías en chips (obligatoria: la confirmación muestra "Electrónica / Laptops").
- **Paso 2 · Atributos** (Figma "Reportar - Atributos"): ¿qué es? (nombre, obligatorio), color principal (obligatorio; "+" muestra 13 colores), marca (opcional), características distintivas (opcional).
- Validación por paso en `ValidateReportStepUseCase`: **Siguiente** queda deshabilitado hasta que el paso es válido. La flecha atrás vuelve al paso anterior sin perder datos (en el paso 1 cancela).
- Pasos 3 y 4: L03 (dónde y cuándo, foto, contacto) y L04 (confirmación y envío).
- Tests: `ValidateReportStepUseCaseTest`, `ReportWizardViewModelTest`.

## Navegación (F05)

- Rutas `@Serializable` en `navigation/NavRoute.kt`; todas pasan por `RouteGuard`: sin sesión → Login, rol incorrecto → home de su rol.
  Si agregas una ruta, `RouteGuard` no compila hasta que digas qué rol puede entrar.
- Barra inferior por rol: pestañas en `navigation/RoleTabs.kt`, dibujadas con la `BottomNavBar` del Design System en `MainScaffold.kt` (estudiante con FAB central "+" para Reportar; staff con 5 pestañas).
- Wizard de reporte: el borrador vive en `ReportWizardViewModel`; *Cancelar* saca el wizard del back stack y lo descarta.
- Deep link para notificaciones: `DeepLinks.match(id)` → `ucbencuentra://match/{id}` abre `MatchDetail`. Probarlo:
  ```bash
  adb shell am start -a android.intent.action.VIEW -d "ucbencuentra://match/m42" bo.edu.ucb.encuentra.dev
  ```
  La sesión todavía vive en memoria: si la app estaba cerrada, el link lleva a Login (S01 la hará persistente).

## Base de datos (Room 3, F06)

`core/data/local/AppDatabase.kt` (versión **1**), dueño único: **Integrante A**. Se crea por plataforma (`Database.android.kt` / `Database.ios.kt`) y Koin la entrega con `platformModule()`.

| Tabla | Entidad | Modelo de dominio | Notas |
|---|---|---|---|
| `users` | `UserEntity` | `UserModel` (rol, sede, carrera) | email único |
| `lost_reports` | `LostReportEntity` | `LostReport` | estado `ReportStatus` |
| `found_items` | `FoundItemEntity` | `FoundItem` | estado `ItemStatus`, custodia, preguntas de control, `retention_until` |
| `custody_events` | `CustodyEventEntity` | `CustodyEvent` | historial, solo se agrega |
| `matches` | `MatchEntity` | `Match` | score 0–100, único por (reporte, objeto) |
| `claims` | `ClaimEntity` | `Claim` | número único (#1024); único por (objeto, reclamante) → bloquea doble reclamo |
| `audit_logs` | `AuditLogEntity` | `AuditLog` | **append-only**: el DAO no tiene update ni delete |
| `notifications` | `AppNotificationEntity` | `AppNotification` | deep link al tocarla |

- **Dominio** (`core/domain`): enums `Campus`, `CampusZone`, `Category`/`Subcategory`, `ItemStatus`, `ReportStatus`, `ClaimStatus`, `MatchStatus`, `CustodyPlace`, `AuditAction`, `NotificationType`; VOs `Email` (@ucb.edu.bo), `Password`, `ItemId`.
- **Convenciones**: ids de texto (los mismos en Firestore, L06) · fechas en epoch millis en Room y `kotlin.time.Instant` en dominio · enums guardados por nombre · listas como JSON (`ListConverters`, kotlinx.serialization) · sin foreign keys (con sincronización los datos pueden llegar en cualquier orden).
- **Mappers** entity ↔ dominio en `core/data/local/mapper/EntityMappers.kt`. Los repositorios de cada feature usan los DAOs y estos mappers; ningún ViewModel toca un DAO.
- **Seed**: `SeedData` (datos 2026 del mockup) se carga una vez al abrir la app si la base está vacía. Para recargarlo: desinstalar la app o *Ajustes → Apps → UCB Encuentra (dev) → Borrar datos*.
- **room3** cambió nombres: los conversores son `@ColumnTypeConverter` / `@ColumnTypeConverters` (no `@TypeConverter`).

### Agregar consultas o tablas

- **Nueva consulta**: agrégala al DAO existente en tu PR (no cambia el esquema).
- **Nueva tabla o columna**: avisa al Integrante A; cambia el esquema y necesita migración.

### Estrategia de migraciones

Room exporta el esquema de cada versión en `shared/schemas/bo.edu.ucb.encuentra.core.data.local.AppDatabase/<versión>.json` al compilar. **Esos JSON se commitean**: sirven para las migraciones automáticas y para revisar el cambio en el PR.

1. Cambia la entidad y sube `version` en `@Database` (1 → 2).
2. **Cambio simple** (tabla nueva, columna nueva con valor por defecto): agrega `autoMigrations = [AutoMigration(from = 1, to = 2)]` en `@Database`.
3. **Cambio complejo** (renombrar o borrar columna, cambiar tipo): `AutoMigration` con una spec (`@RenameColumn`, `@DeleteColumn`…) o una `Migration(1, 2)` con SQL, registrada en `getRoomDatabase()` con `.addMigrations(...)`.
4. Compila: se genera `2.json`. Commitea el JSON junto con el cambio.
5. **Nunca** `fallbackToDestructiveMigration()` en `prod`: borraría los reportes del usuario. En desarrollo, si la base local quedó inconsistente, desinstala la app.

## Tests y calidad

```bash
./gradlew :shared:testAndroidHostTest   # tests de commonTest + androidHostTest
./gradlew :shared:detekt                # análisis estático (reporte en shared/build/reports/detekt)
```

## Design System

Es un proyecto aparte: [UcbEncuentraDesignSystem](https://github.com/MaiteAraucoPorrez/UcbEncuentraDesignSystem) (clónalo al lado de esta carpeta como `design-system`). Se publica en `ds-repo/` de este repo y la app lo usa como `edu.ucb.ds:shared:1.0.0-SNAPSHOT`, así nadie necesita `publishToMavenLocal`.

Valores tomados del [Figma](https://www.figma.com/design/A9UfzePVdhqPurX4X7luU7/Sin-t%C3%ADtulo) (fuente: **Open Sans**):

```kotlin
UcbEncuentraTheme.colors.primary          // primary, secondary, tertiary, background, surface, surfaceContainer,
                                          // surfaceVariant, onSurface, onSurfaceVariant, outline, outlineVariant, error…
UcbEncuentraTheme.colors.scoreColor(92)   // success >= 80, warning 50-79, neutral < 50
UcbEncuentraTheme.typography.heading      // heading, title, bodyLarge, body, label, overline
UcbEncuentraTheme.spacing.screenPadding   // small 4 · medium 8 · itemGap 12 · large 16 · screenPadding 20
UcbEncuentraTheme.shapes.extraLarge       // small 8 · medium 12 · large 16 · extraLarge 24 · pill
UcbEncuentraTheme(darkTheme = isDark) { … }
```

Demo: Perfil → Menú de desarrollo → *Design System* (tokens, componentes F03 y componentes de dominio F04: CurvedHeader, BottomNavBar, ScoreBadge, StatusChip, CategoryTile, StatCard, ItemCard y estados). Para cambiar el DS ver `ds-repo/README.md`.
En Windows las rutas de `ds-repo/` son largas: `git config --global core.longpaths true`.

## Estructura

```
shared/src/commonMain/kotlin/bo/edu/ucb/encuentra/
├── App.kt
├── core/            # código común (config, data, presentation)
├── di/              # KoinInit + SharedModules (registra el módulo de cada feature)
├── navigation/      # NavRoute (@Serializable) + AppNavHost
├── core/data/local/ # AppDatabase, entidades, DAOs, mappers y seed (F06)
└── feature/<x>/     # data / domain / presentation/{screen,viewmodel} / di
ds-repo/             # Design System publicado (no editar a mano)
shared/schemas/      # esquemas de Room por versión (generados, se commitean)
```

Dependencias disponibles (todas en `gradle/libs.versions.toml`): Navigation Compose, kotlinx.serialization, Ktor, Coil 3, Room 3 + KSP, Firebase GitLive (Auth, Firestore, Storage, Messaging, Remote Config), WorkManager (Android), Wire/gRPC (los `.proto` van en `shared/src/commonMain/proto`) y detekt.

## Cómo contribuir

Ramas, commits, dueños por carpeta y Definition of Done: ver [CONTRIBUTING.md](CONTRIBUTING.md).
