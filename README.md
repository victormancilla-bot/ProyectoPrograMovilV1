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
| `admin@ucb.edu.bo` | ADMIN | Igual que staff + **Perfil → Roles y permisos** (S03) |
| `ana.gomez@ucb.edu.bo`, `luis.mendoza@ucb.edu.bo`, `carlos.mamani@ucb.edu.bo` | ESTUDIANTE | Inicio (reclamantes del seed) |

Las pantallas que no están en la barra se abren desde **Perfil → Menú de desarrollo** (solo mientras son stubs).

## Login y sesión (S01)

- Pantalla del Figma "Login Estudiante": header alto con el escudo, tarjeta blanca, **Continuar** y "Contacta a Sistemas UCB".
- Dos pasos en la misma tarjeta: (1) el correo debe ser **@ucb.edu.bo** (`ValidateInstitutionalEmailUseCase`); (2) contraseña. `AuthenticateUseCase` busca el usuario en Room (`UserRepository`) y responde "Correo o contraseña incorrectos" sin revelar qué correos existen.
- **Sesión persistente** con DataStore (`SessionRepositoryImpl`): guarda solo el id del usuario; al abrir la app se restaura desde Room y se entra directo al home del rol. **Perfil → Cerrar sesión** la borra.
- Para empezar de cero (borra sesión y base): *Ajustes → Apps → UCB Encuentra (dev) → Borrar datos*, o `adb shell pm clear bo.edu.ucb.encuentra.dev`.
- El enlace a Sistemas UCB abre un `mailto:` al correo del parámetro `support_contact` de Remote Config (F08); por defecto `sistemas@ucb.edu.bo`, que es **de prueba** hasta confirmar el contacto real.

## Home del estudiante (L01)

- Pantalla del Figma "Home Estudiante": header con "¡Hola, {nombre}!" y "{carrera} • {sede}" tomados del usuario (no fijo), tarjeta "¿Perdiste algo?" → wizard, **Actividad** y **Accesos rápidos** (Mis reportes, Objetos encontrados, Cómo funciona).
- Actividad en vivo desde Room (`HomeActivityRepository`, `combine` de dos `Flow`): **reportes activos** = Buscando, En verificación o Match confirmado; **coincidencias nuevas** = notificaciones `NEW_MATCH` sin leer (punto rojo si hay). Con el seed: 4 y 1.
- MVI: `HomeState` / `HomeEvent` / `HomeEffect`; tests en `HomeViewModelTest`.
- Pendiente de otras tarjetas: el menú (☰) y la luna de modo oscuro del header (S07).

## Matching fase 1 (M01)

`feature/matching/domain/engine/MatchEngine.kt`: Kotlin puro en `commonMain`, sin UI ni Room.

1. **Filtro duro** (descarta el ruido antes de cualquier IA): misma sede, misma categoría y subcategoría, objeto **DISPONIBLE**, reporte **BUSCANDO** y fechas compatibles (encontrado desde el inicio del rango de pérdida hasta **30 días** después del final).
2. **Score 0–100**:

| Criterio | Puntos | Coincide | No se sabe | No coincide |
|---|---|---|---|---|
| Color | 40 | 40 | 20 | 0 |
| Marca (sin mayúsculas; "Casio fx-991" = "Casio") | 25 | 25 | 12 | 0 |
| Zona | 25 | 25 | 12 ("Otro lugar") | 0 |
| Fechas | 10 | 10 si se encontró dentro del rango | | −1 cada 3 días después |

- Se guardan los pares con score ≥ 30 como **PROPUESTO**, con id fijo por par (`m-<reporte>-<objeto>`): correrlo dos veces no duplica, y no se pisan los descartados.
- `RunMatchingUseCase` implementa el hook `MatchingTrigger`: corre **al enviar un reporte (L04)** contra los objetos DISPONIBLES y **al registrar un hallazgo (H03)** contra los reportes BUSCANDO. Por cada reporte con coincidencias nuevas crea una notificación **NEW_MATCH** al dueño (la cuenta el Home y abre la mejor por deep link). Si el matching falla, el reporte o el objeto igual quedan guardados.
- Tests: `MatchEngineTest` (8, casos borde de fechas, textos, datos desconocidos), `RunMatchingUseCaseTest` (4).

## Mis reportes (L05)

- Pantalla del PDF "Mis Reportes": chips **Todos / Buscando / Verificación** (Verificación = en verificación o con match confirmado) y tarjetas con categoría, nombre, "Reportado: …", ubicación, chip de estado ("Match confirmado", "En verificación", "Vencido"…) y **Ver detalles**. Los que ya no están en curso (cerrados, recuperados, vencidos) aparecen atenuados y al final.
- Lista en vivo desde Room con `Flow` (`MyReportsRepositoryImpl`): crear, cerrar o un match nuevo se ven sin recargar.
- **Detalle del reporte** en la misma pantalla (sin ruta nueva: `NavRoute.kt` está congelado): datos, rango de fechas, ubicación, foto, contacto y **sus coincidencias** con su % (tocar una abre el detalle del match). El botón atrás del teléfono vuelve a la lista (`NavigationBackHandler` de `androidx.navigationevent`, el reemplazo de `BackHandler`).
- **Cerrar reporte** con confirmación y motivo: "Ya lo encontré por mi cuenta" (RECUPERADO) o "Ya no lo estoy buscando" (CERRADO). Solo si está BUSCANDO y es del usuario; queda en la auditoría (`AuditAction.REPORT_CLOSED`). Un reporte cerrado deja de aparecer en Posibles coincidencias y el matching ya no lo toca.
- Estados: cargando, vacío ("Aún no reportaste nada" → Reportar objeto perdido), filtro sin resultados y error con Reintentar.
- Tests: `MyReportsUseCasesTest` (5, con el seed), `MyReportsViewModelTest` (6).

## Matching fase 2 (M02): score ponderado y ranking

- **Score ponderado 0–100** en `MatchEngine`: color, marca, zona, fecha y **texto** (palabras clave del título + descripción, sin tildes ni palabras vacías). Cada criterio da una similitud 0–1 que se multiplica por su peso; los pesos son relativos (no tienen que sumar 100).
- **Pesos y umbrales desde Remote Config** (`RemoteConfigMatchingConfigRepository`, con el mismo data source que F08). Parámetros para crear en la consola de Firebase:

| Parámetro | Por defecto | Qué hace |
|---|---|---|
| `matching_weight_color` | 30 | Peso del color |
| `matching_weight_brand` | 20 | Peso de la marca |
| `matching_weight_zone` | 20 | Peso de la zona |
| `matching_weight_date` | 10 | Peso de la fecha |
| `matching_weight_text` | 20 | Peso del texto |
| `match_threshold` | 30 | Umbral mínimo para proponer (parámetro de F08; antes `matching_min_score`) |
| `matching_top_n` | 5 | Máximo de candidatos por reporte |
| `matching_max_days_to_find` | 30 | Días después de la pérdida en que puede aparecer |

  Mientras no exista `google-services.json`, Firebase no está inicializado y se usan los valores por defecto (sin romper nada). Valores inválidos (pesos negativos o todos en 0) vuelven a los por defecto.
- **Top-N con umbral**: por reporte se proponen los N mejores sobre el mínimo.
- **"No es mío"**: botón en cada tarjeta de Posibles coincidencias (con confirmación). La coincidencia pasa a **DESCARTADO** y el matching nunca la vuelve a proponer; el ranking se re-calcula para que entre el siguiente candidato.
- **Re-matching** (`RunMatchingUseCase`): al crear un reporte, al llegar un hallazgo (para cada reporte afectado) y al editar un reporte (`MatchingTrigger.onReportUpdated`, lo llamará L05). Solo toca reportes **BUSCANDO**; respeta DESCARTADO y CONFIRMADO; actualiza el score de las propuestas que siguen, borra las que quedaron fuera del Top-N y avisa solo por las nuevas.
- Tests: `MatchRankingTest` (7), `RunMatchingUseCaseTest` (9), `MatchEngineTest` (8, con los pesos de la fase 1), `MatchListViewModelTest` (6).

## Posibles coincidencias (M04)

- Pantalla del Figma "Resultados de Matching" (se abre desde "N coincidencias nuevas" del Home): candidatos con `ItemCard` + `ScoreBadge` de color (verde ≥ 80, naranja 50-79, gris < 50) y ranking `#1, #2...` del más al menos parecido. **Nunca un "es / no es"**: siempre un porcentaje.
- Chips por reporte cuando el usuario tiene más de uno con candidatos (primero los que tienen coincidencias por revisar). Los matches descartados ("No es mío", M02) no aparecen; los confirmados dicen "Confirmado".
- **Modificar criterios** vuelve al reporte (Mis reportes, L05). Tocar una tarjeta abre el detalle (M02).
- Estados: cargando, vacío ("Aún no hay coincidencias" → Reportar objeto perdido) y error con **Reintentar**.
- Datos: `MatchRepositoryImpl` combina `lost_reports`, `matches` (nueva consulta `observeActiveByOwner`) y `found_items` de Room, así la lista se actualiza sola cuando M01 agregue matches. Con el seed 2026: Calculadora científica → 92 / 65 / 30 (igual que el mockup) y Cuaderno de Cálculo → 92 confirmado.
- Tests: `ObserveMyMatchesUseCaseTest` (con el seed), `MatchListViewModelTest`.

## Detalle de match (M05)

- Pantalla del PDF "Detalle de Match" (desde una tarjeta de Posibles coincidencias, del detalle de Mis reportes o por deep link `ucbencuentra://match/{id}`): foto del objeto (o el ícono de su categoría si no tiene) con el **% de parecido**, nombre y fecha en que se encontró.
- **Análisis de coincidencia**: "Lo que reportaste" vs "Lo que se encontró" fila por fila (tipo, marca, color con su muestra, lugar y fecha) con **✔ coincide** o **? por verificar**. Usa las mismas similitudes del score (`MatchEngine`, M02): ✔ solo si el dato coincide del todo; distinto o desconocido = ?.
- **Vista limitada del hallazgo** (`FoundItemPreview`): no incluye la descripción del staff, el lugar exacto, la custodia ni las preguntas de control, porque podrían darle las respuestas a quien no es el dueño. La pantalla lo explica en "Información protegida".
- Solo el dueño del reporte ve la coincidencia: con otra sesión, o si ya se descartó, aparece "Coincidencia no disponible".
- **Esto es mío — verificar** → `ClaimMatchUseCase` llama al hook `ClaimRequester` (core), que implementa V02 (`CreateClaimUseCase`, ver abajo). Si el reclamo se crea, los botones se reemplazan por "Reclamo #N enviado"; los errores de V02 ("Ya reclamaste este objeto (Reclamo #N).") se muestran tal cual.
- Si el dueño ya reclamó el objeto, el detalle lo sabe desde Room y muestra su reclamo con el estado en vez de los botones.
- **No es lo mío, seguir buscando** → confirmación y `RejectMatchUseCase` (M02): la coincidencia se descarta, se re-calcula el ranking y se vuelve a la lista.
- Los botones solo aparecen en coincidencias **propuestas** de un reporte abierto; si ya está confirmada o el reporte se cerró se muestra un aviso.
- Tests: `MatchDetailUseCasesTest` (8, con el seed), `MatchDetailViewModelTest` (7).

## Dashboard Administrador (D01)

Pantalla del Figma "Dashboard Admin" (pestaña **Panel** del staff): "Panel — {sede}", 4 contadores, "Registrar objeto encontrado" (abre H03) y "Últimos reclamos pendientes" (los 3 más recientes, con "Hace 2 horas") + **Ver todos** (lista de reclamos). Tocar un reclamo abre su detalle; tocar un contador lleva a Inventario o Reclamos.

| Contador | Qué cuenta |
|---|---|
| Objetos en custodia | objetos DISPONIBLE o RECLAMADO |
| Pendientes de entrega | reclamos APROBADO (el dueño todavía no lo recogió) |
| En verificación | reclamos EN_VERIFICACION (respondieron las preguntas; decide el staff) |
| Por vencer (en rojo) | objetos DISPONIBLE cuya retención vence en los próximos 7 días |

**En tiempo real, sin refrescar** (tema "Base de datos en tiempo real" del plan, como la feature `realtimedatabase` del docente):

- `RealtimeStatsDataSource` escucha el nodo `stats/` de **Firebase Realtime Database** con `reference("stats").valueEvents` (GitLive `firebase-database`).
- Mientras no llega (o si Firebase no está configurado: todavía no hay `google-services.json`, o falta algún contador), se usan los mismos contadores calculados con `COUNT` de **Room**, que también son `Flow` y se actualizan solos. La pantalla indica la fuente: "En vivo · Firebase Realtime Database" o "En vivo · datos del dispositivo".
- "Por vencer" depende de la hora: se recalcula cada 30 minutos aunque la base no cambie.

Nodo a crear en la consola (F08), con números enteros; lo puede mantener N03 desde el servidor:

```json
{ "stats": { "en_custodia": 142, "pendientes_entrega": 18, "en_verificacion": 5, "por_vencer": 3 } }
```

- Tests: `ObserveDashboardStatsUseCaseTest` (sin Firebase → Room, Firebase reemplaza a Room al llegar, nodo faltante, actualización en vivo), `DashboardViewModelTest` (sede, contadores, reclamo nuevo sin refrescar, navegación) y `FormatRelativeTest`.

## Roles ESTUDIANTE / STAFF / ADMIN (S03)

El rol decide qué ve y qué puede hacer cada quien. **Nadie se asigna staff a sí mismo.**

- **Campo `role`** de la cuenta (`users` en Room; en Firebase `users/{uid}.role`). Solo un **ADMIN** lo cambia: lo valida `ChangeUserRoleUseCase` y, para cuando exista el proyecto de Firebase (S02), las reglas en `firebase/firestore.rules` (al crear la cuenta siempre es ESTUDIANTE; el dueño no puede tocar su rol; el admin solo cambia el de otras cuentas, entre ESTUDIANTE y STAFF).
- **Reglas del dominio** (`feature/profile/domain/usecase/RoleAdminUseCases.kt`): solo ADMIN; nadie cambia su propio rol; las cuentas ADMIN no se tocan desde la app y solo se asigna ESTUDIANTE o STAFF; cada cambio queda en la **auditoría** (`ROLE_CHANGED`, p. ej. "Estudiante → Staff") en la misma transacción.
- **UI y rutas por rol** (F05): nueva ruta `NavRoute.UserRoles` con nivel de acceso **ADMIN** en `RouteGuard` (el staff vuelve a su Panel; el estudiante, a su Inicio). El botón **Roles y permisos** solo aparece en el Perfil del admin.
- **Pantalla Roles y permisos**: asignar staff por correo institucional (la persona debe haber ingresado una vez), lista de cuentas con filtros por rol y contador, **Hacer staff** / **Quitar staff** (quitar pide confirmación).
- **Cuentas de prueba**: ver la tabla de arriba (estudiante, staff, admin). El rol nuevo se aplica la próxima vez que la persona ingresa.
- Tests: `RoleAdminUseCasesTest`, `UserRolesViewModelTest` y `RouteGuardTest` (solo admin entra al panel).

## Notificaciones internas (N01)

Avisos de **coincidencia nueva** (matching) y de **cambios del reclamo** (aprobado, rechazado, bloqueado, entrega programada, entregado), como notificación del sistema.

- **expect/actual**: interfaz `LocalNotifier` (core) que cada plataforma registra en su `platformModule()`: `AndroidLocalNotifier` (`NotificationCompat`) e `IosLocalNotifier` (pendiente, no se compila desde Windows). Igual el permiso: `rememberNotificationPermission` expect/actual.
- **Permiso POST_NOTIFICATIONS** (Android 13+, en el manifest): se pide cuando entra un usuario (`LocalNotificationsEffect` en `AppNavHost`). Sin permiso no se muestra nada en el sistema, pero el aviso queda guardado para la bandeja (N04).
- **Canales** (`NotificationChannels`, como en ucbp1): **Coincidencias**, **Reclamos**, **Retención** y General; el usuario puede silenciar cada uno en Ajustes.
- **Se guardan en Room** (`notifications`): todo pasa por `NotificationCenter` (core), que usan el matching (`RoomMatchingStore`) y los reclamos (`RoomClaimNotifier`).
- **A quién se le muestra:** en la demo todos usan el mismo teléfono, así que el aviso se muestra al momento **solo si el destinatario es quien tiene la sesión abierta**. Si el staff aprueba el reclamo de Ana, a Ana le llega cuando entra (`deliverPending`, máximo 5, una sola vez: se recuerda en la DataStore hasta cuándo ya se le mostró). Entre dispositivos lo resuelve el push de N02.
- **Tocar la notificación** abre su deep link de F05 (`ucbencuentra://match/{id}` → Detalle de match); si no tiene, abre la app.
- Tests: `NotificationCenterTest` (se guarda todo y se notifica solo al usuario actual, pendientes al entrar sin repetir, leídas excluidas y máximo 5, sin sesión).

## Captura de foto (H01)

El registro de un objeto encontrado empieza ahora por las fotos: **Inventario o Dashboard → "Registrar objeto encontrado" → Captura (paso 1) → Formulario (pasos 2 y 3)**. Al registrar se vuelve a la pantalla de origen.

- **Spike: expect/actual propio** en lugar de FileKit/Peekaboo: mismo patrón que el `PhotoPicker` de L03, sin dependencias nuevas (`libs.versions.toml` congelado). `core/presentation/CameraCapture.kt` (`rememberCamera`, `CameraLauncher`, `CameraPermissionResult`); en Android usa `TakePicture` del sistema con un temporal compartido por `FileProvider` (`res/xml/file_paths.xml`); en iOS queda `isAvailable = false` (no se compila desde Windows).
- **Permiso CAMERA** (manifest, con `uses-feature camera.any` opcional): si ya está, abre la cámara; la primera vez lo pide; si el usuario lo negó, muestra "Sin acceso a la cámara" con **Permitir** (vuelve a pedirlo) o la galería; si lo negó con "No volver a preguntar", ofrece **Abrir ajustes**.
- **Pantalla "Registrar – Captura de Foto"** (título corregido de "Reportar" a "Registrar"): stepper **1. Captura · 2. Detalles · 3. Ubicación** (`RegisterStepper`), Tomar foto / Subir galería, "Fotos capturadas (N)" con miniaturas y **X para eliminar**, "+ Agregar", **mínimo 1 foto** para Continuar (máximo 6).
- **Compresión antes de guardar** (`PhotoCompression` + `compressToAppStorage`): lado mayor 1600 px, JPEG 80, girada según el EXIF, en `filesDir/found_photos` (las de los reportes en `report_photos`, también comprimidas). En el emulador una foto de 63 KB quedó en 23 KB; una de cámara real de 3-8 MB queda en ~300 KB.
- Las fotos pasan al formulario por `CapturedPhotosStore` (core, en memoria; `NavRoute.FoundForm` no lleva parámetros). El formulario las muestra, exige al menos una (`PHOTO_REQUIRED`) y el objeto se guarda con `photoUrls`. Volver atrás desde el formulario no pierde las fotos; cancelar la captura las descarta.
- Tests: `PhotoCompressionTest`, `FoundCaptureViewModelTest` y los de H03 actualizados (foto requerida, fotos guardadas, borrador limpio al registrar).

## Inventario de objetos (H07)

Pantalla del Figma "Inventario" (pestaña **Inventario** del staff), en vivo desde Room:

- **Búsqueda por texto o código**: ignora mayúsculas y tildes y busca en nombre, código, marca, color, descripción, categoría y lugar ("casio", "#CALCULAD", "bloque c"). Con varias palabras tienen que estar todas.
- **Código del objeto** (`FoundItem.code`): `#` + los primeros 8 caracteres del id, para buscar y para la etiqueta física.
- **Chips Todos / Disponible / Reclamado / Vencido**. "Disponible" = espera a su dueño y no venció.
- **Por vencer y Vencido** (`RetentionState`, `FoundItem.retentionState(now)`): no se guardan, se calculan con la fecha de retención (`retention_days` de F08) y solo para objetos disponibles. Por vencer = dentro de 7 días (igual que el Dashboard). Se resaltan con un chip naranja/rojo en la tarjeta y una franja en el detalle; en la lista van primero.
- **Detalle** (en la misma pantalla, el atrás del teléfono vuelve a la lista): fotos, datos completos (incluida la descripción privada), quién lo registró, retención, **custodia actual** e **historial de custodia** (lugar, fecha, quién y nota).
- **Editar custodia**: Oficina / Locker / Bodega + detalle + nota. Usa `MoveCustodyUseCase` de H05: solo staff, solo objetos en custodia, audita (`CUSTODY_CHANGED`) y agrega el movimiento al historial.
- "Registrar objeto encontrado" abre H03 (hasta que H01 agregue la captura de foto).
- Tests: `InventoryUseCasesTest` (retención, chips, orden, búsqueda, detalle) y `InventoryViewModelTest` (filtros, detalle, editar custodia de punta a punta, mismo lugar, navegación).

## Remote Config: parámetros y flags (F08)

Parámetros que la oficina puede cambiar en la consola de Firebase **sin publicar una versión** (mismo patrón que la feature remoteconfig de ucbp1: valores por defecto + `fetchAndActivate` + `safeCall`):

| Parámetro | Tipo | Por defecto (offline) | Para qué |
|---|---|---|---|
| `retention_days` | número | 90 | Días de custodia de un objeto nuevo (H03 → "Por vencer" de D01, D04). Fuera de 7–365 se usa 90 |
| `match_threshold` | número | 30 | Score mínimo para proponer una coincidencia (M01/M02). Se limita a 0–100 |
| `ai_suggestions_enabled` | booleano | false | Sugerencias de atributos con IA al registrar (H02, cuando exista) |
| `support_contact` | texto | sistemas@ucb.edu.bo | Correo de "Contacta a Sistemas UCB" en el login |
| `maintenance_mode` | booleano | false | Muestra "Estamos en mantenimiento" (con el contacto y **Reintentar**) en lugar de la app |

- **Fetch al abrir la app** (`App.kt`, en paralelo al seed y la sesión: no retrasa el arranque). La configuración es un `StateFlow` (`AppConfigRepository`): las pantallas reaccionan cuando llega.
- **Valores por defecto offline**: sin internet, sin `google-services.json` o si el fetch falla, la app sigue con los últimos valores guardados o los por defecto; el error vuelve como `Result` (no rompe nada). Valores fuera de rango se corrigen (`AppConfig.sanitized()`).
- **Un solo `RemoteConfigDataSource`** (core) para toda la app: Firebase `setDefaults` reemplaza el mapa completo, así que los defaults de F08 y del matching (M02) se registran juntos (`RemoteConfigKeys.DEFAULTS`) y se descarga una vez por sesión. "Reintentar" del mantenimiento fuerza una descarga nueva. `minimumFetchInterval = 0` para que cada apertura traiga los cambios.
- Tests: `RemoteConfigTest` (defaults offline = defaults de la app y del matching, valores de la consola aplicados, fetch fallido, valores fuera de rango, reintento forzado), más retención configurable en H03 y contacto de soporte en el login.

## Confirmación de entrega (V04)

Pantalla del Figma "Confirmación de Entrega" (staff: Reclamos → reclamo aprobado → **Confirmar entrega**; la lista del staff ahora también muestra los aprobados, después de los que hay que decidir).

- **Listo para entrega**: objeto, "Ref: Reclamo #N", **Reclamado por** (estudiante) y **Aprobado por** (staff), y **Fecha y hora programada** ("4 nov 2026, 10:00 - 10:30") o "Sin programar".
- **Programar entrega** (`ScheduleDeliveryUseCase`): fecha (solo desde hoy) y hora → "Guardar fecha y avisar al estudiante". Solo reclamos aprobados, solo staff, nunca en el pasado; se puede reprogramar. Queda en la auditoría (`DELIVERY_SCHEDULED`) y el estudiante recibe la notificación "Entrega programada" (hook `ClaimNotifier.onDeliveryScheduled`). Programar es opcional: el estudiante también puede pasar sin cita.
- **Verificación física obligatoria** (`PhysicalChecks`): las dos casillas del mockup (identidad con carnet universitario físico y objeto recibido conforme). Sin las dos, "Confirmar entrega" queda deshabilitado y el dominio también lo rechaza.
- **Confirmar** (`ConfirmDeliveryUseCase` → DELIVER de V02): reclamo y objeto **ENTREGADO** (con quién y cuándo), reporte "Recuperado", los demás reclamos abiertos del objeto se rechazan, **auditoría** (S05) con la verificación y **notificación** "Objeto entregado" al estudiante (N01). La pantalla pasa a "Entregado".
- Tests: `DeliveryUseCasesTest` (programar con auditoría y aviso, permisos y fechas, checklist obligatorio, entrega completa) y `DeliveryViewModelTest`.

## Panel de Verificación de Reclamos (V03)

- **Lista** (pestaña Reclamos del staff, mockup "Verificación de Reclamos"): tarjetas con miniatura (foto o ícono de la categoría), "RECLAMO #1024", objeto, "Reclamado por" y chip **"Hace 2 horas"**. La flecha **expande** un resumen (estado, sugerencia de las respuestas, intentos fallidos, % de parecido) con **Revisar reclamo**. Primero los que ya respondieron las preguntas.
- **Detalle** (no estaba en el PDF): quién reclama, estado, tiempo y % de parecido (`ScoreBadge`); **fotos** del hallazgo y del reporte; **Reporte del estudiante vs Hallazgo** fila por fila con ✔ / ✘ (el staff ve también el lugar exacto y la descripción privada que el estudiante no ve en M05); **custodia** (privado); **preguntas de control** con respuesta, esperada y sugerencia (V01). El reporte solo se muestra si es de quien reclama.
- **Aprobar / Rechazar con motivo obligatorio**: el diálogo no deja confirmar sin motivo y el dominio también lo exige (`ClaimErrors.REASON_REQUIRED`).
- **Hooks**: la auditoría usa `AuditRecorder` (hook de S05, ya en H05): "Reclamo #1024 · En verificación → Aprobado · {motivo}". Las notificaciones usan el hook nuevo **`ClaimNotifier`** (core) para N01: `RoomClaimNotifier` guarda una notificación interna (tipo Reclamos) para quien reclamó cuando su reclamo se aprueba, rechaza, bloquea por intentos o entrega (con deep link al Detalle de match). N01/N02 pueden sumarle notificación del sistema o push sin tocar los casos de uso.
- Tests: `ClaimDecisionHooksTest` (motivo obligatorio, auditoría y notificación al aprobar/rechazar, entrega que avisa al dueño y rechaza a los demás, textos), `ClaimComparisonTest` (comparación reporte vs hallazgo) y los de los ViewModels.

## Preguntas de control (V01)

"No se entrega por hacer match": quien reclama responde las preguntas que definió el staff y el staff decide.

- **H03** pide ahora, junto a cada pregunta, la **respuesta esperada (privada)**, obligatoria. Se guarda en `FoundItem.expectedAnswers` (Room v2).
- **Pantalla del estudiante** (no estaba en el PDF), dentro del Detalle de match: después de "Esto es mío — verificar" aparece "Verifica que es tuyo" con un campo por pregunta y los intentos que quedan. Después muestra en qué quedó el reclamo (en verificación, aprobado, rechazado, bloqueado o entregado).
- **Comparación asistida** (`AnswerComparator`): sin mayúsculas, tildes, signos ni palabras de relleno. Coincide si la respuesta contiene la esperada, o al revés, o si comparte al menos la mitad de sus palabras clave ("uno de la UCB" ≈ "Escudo de la UCB"). Un objeto sin respuesta esperada (registrado antes de V01) va directo al staff.
- **Intentos y bloqueo**: si alguna respuesta no coincide, el reclamo sigue PENDIENTE y se descuenta un intento, **sin decir cuál falló ni cuál era la respuesta**. Al 3er intento fallido (`Claim.MAX_ANSWER_ATTEMPTS`) el reclamo queda **bloqueado** (RECHAZADO), la coincidencia se descarta, el objeto vuelve a DISPONIBLE si nadie más lo reclama y todo se audita.
- **Respuestas privadas**: solo `ObserveClaimReviewUseCase` / `ObserveOpenClaimsUseCase` las devuelven, y solo al staff. El estudiante nunca ve las respuestas esperadas, ni siquiera las suyas después de enviarlas.
- **Decisión humana**: el detalle del reclamo para el staff (pestaña Reclamos → reclamo) muestra cada pregunta con lo que respondió el estudiante, la esperada y la sugerencia "Coincide / No coincide", más los intentos usados. Aprobar o rechazar pide confirmación y un motivo (obligatorio desde V03) que queda en la auditoría. La lista y el detalle completos están en V03 (abajo).
- Para que `matching` no dependa de `claims`, el hook de core `ClaimRequester` ahora también tiene `answerQuestions` (lo implementa `ClaimRequesterImpl`).
- Tests: `AnswerComparatorTest` (3), `ControlQuestionsTest` (7: coincide, intento fallido, bloqueo al 3er intento, preguntas incompletas, sin respuesta esperada, revisión del staff, privacidad), `ClaimDetailViewModelTest` (5), más los de H03 y M05 actualizados.

## Reclamos: flujo y estados (V02)

Estados del reclamo con su máquina de estados (`ClaimStateMachine`, igual que H05 para el objeto):

```
PENDIENTE ──responder (quien reclama, V01)──▶ EN_VERIFICACION ──aprobar (staff)──▶ APROBADO ──entregar (staff)──▶ ENTREGADO
    │                                              │
    └──────────────rechazar (staff)────────────────┴──▶ RECHAZADO
```

- **Crear** (`CreateClaimUseCase`, desde "Esto es mío — verificar" de M05), en una transacción: valida que la coincidencia sea del reporte de quien reclama y siga propuesta, y que el objeto siga en custodia; **bloquea el doble reclamo** del mismo objeto por la misma persona (además del índice único `found_item_id + claimant_id` de Room); crea el reclamo PENDIENTE con número visible (#1027…); el objeto pasa a **RECLAMADO con la máquina de H05**; el reporte pasa a "En verificación"; queda en la auditoría.
- **Varias personas pueden reclamar el mismo objeto** (decisión): si alguien reclamó primero, el dueño real no queda bloqueado; el objeto ya está RECLAMADO y el staff decide. Solo un reclamo por objeto puede quedar aprobado.
- **Transiciones** (`ChangeClaimStatusUseCase` + `SubmitClaimAnswersUseCase`, `ApproveClaimUseCase`, `RejectClaimUseCase`, `DeliverClaimUseCase`), cada una auditada:

| Transición | Quién | Coincidencia | Reporte del dueño | Objeto (H05) |
|---|---|---|---|---|
| Responder preguntas | quien reclama | — | — | — |
| Aprobar | staff | CONFIRMADO | Match confirmado | sigue RECLAMADO |
| Rechazar | staff | DESCARTADO | vuelve a Buscando si no tiene otro reclamo abierto | vuelve a DISPONIBLE si nadie más lo reclama |
| Entregar | staff | — | Recuperado | ENTREGADO; los demás reclamos abiertos se rechazan |

- Un reclamo solo cambia la coincidencia y el reporte si el reporte es de quien reclama (protege ante datos mal cargados).
- Las pantallas que usan estas transiciones son de otras tarjetas: responder preguntas (V01), panel del staff (V03) y entrega (V04).
- Tests: `ClaimStateMachineTest` (4) y `ClaimUseCasesTest` (9, con el seed: crear, doble reclamo, objeto ya reclamado por otro, permisos, flujo completo hasta la entrega, rechazo, entrega con otros reclamos abiertos).

## Estados y custodia del objeto (H05)

Máquina de estados en `core/domain/model/ItemStateMachine.kt`:

```
DISPONIBLE ──reclamar──▶ RECLAMADO ──entregar──▶ ENTREGADO
    ▲                       │
    └────────liberar────────┘   (reclamo rechazado, V01)
DISPONIBLE ──descartar──▶ DESCARTADO   (retención vencida, D04)
DISPONIBLE ──donar──────▶ DONADO       (retención vencida, D04)
```

- ENTREGADO, DESCARTADO y DONADO son finales. Cualquier otra transición lanza `InvalidTransitionException` ("No se puede descartar un objeto en estado \"Reclamado\"").
- Un use case por transición en `core/domain/usecase/item`: `ClaimItemUseCase`, `ReleaseItemUseCase`, `DeliverItemUseCase`, `DiscardItemUseCase`, `DonateItemUseCase` (todos sobre `ChangeItemStatusUseCase`). Reclamar lo puede hacer el estudiante; el resto, solo staff.
- Custodia: `MoveCustodyUseCase` (oficina, locker, bodega + detalle) agrega un `CustodyEvent` al historial y actualiza el lugar actual; solo para objetos DISPONIBLE o RECLAMADO. `ObserveCustodyHistoryUseCase` devuelve el historial (más reciente primero).
- Hook de auditoría para S05: `AuditRecorder` (hoy `RoomAuditRecorder`, tabla append-only). Cada cambio de estado o custodia se guarda junto con su auditoría en una sola transacción (`TransactionRunner` → `withWriteTransaction` de Room).
- Tests: `ItemStateMachineTest` (todas las combinaciones estado × transición), `ItemLifecycleUseCasesTest`.

## Registrar objeto encontrado (H03)

- Pantalla del mockup "Registrar - Formulario de Objeto" para el staff (por ahora se abre desde **Inventario → Registrar objeto encontrado**; H01 la abrirá después de la foto).
- **Datos detectados** (categoría/subcategoría y color) editables tocando cada fila. En R3 se eligen a mano (chip "Ingreso manual"); con H02 la IA los precarga y el chip pasa a "Sugerido por IA".
- Nombre (obligatorio), marca (opcional), características, **ubicación del hallazgo**, **ubicación de custodia actual (obligatoria)** + detalle opcional, **fecha y hora** (arrancan en "ahora", no pueden ser futuras) y **1-3 preguntas de control** para el dueño (las usa V01).
- `RegisterFoundItemUseCase` valida (`ValidateFoundItemUseCase`) y guarda en Room con estado **DISPONIBLE**, retención de 90 días, sede y staff de la sesión. `FoundItemRepositoryImpl` escribe en una sola transacción el objeto, su primer evento de custodia y el registro de auditoría `ITEM_REGISTERED`.
- Los errores se muestran al intentar registrar (formulario largo: el botón siempre indica qué falta).
- Tests: `ValidateFoundItemUseCaseTest`, `RegisterFoundItemUseCaseTest`, `FoundFormViewModelTest` (reloj fijo inyectado: `Clock` y `TimeZone` vienen de `coreModule`).

## Wizard de reporte (L02-L04)

- Un solo `ReportWizardViewModel` para los 4 pasos con el borrador (`ReportDraftState` + `ReportDraft`); cancelar lo descarta (F05).
- **Paso 1 · Categoría** (Figma "Reportar - Categoría"): grilla de 6 categorías y, al elegir una, sus subcategorías en chips (obligatoria: la confirmación muestra "Electrónica / Laptops").
- **Paso 2 · Atributos** (Figma "Reportar - Atributos"): ¿qué es? (nombre, obligatorio), color principal (obligatorio; "+" muestra 13 colores), marca (opcional), características distintivas (opcional).
- Validación por paso en `ValidateReportStepUseCase`: **Siguiente** queda deshabilitado hasta que el paso es válido. La flecha atrás vuelve al paso anterior sin perder datos (en el paso 1 cancela).
- **Paso 3 · Dónde y cuándo** (Figma "Reportar - Ubicación y Foto", con las correcciones de L03):
  - Zona del campus (obligatoria) + detalle aula/piso (opcional).
  - **Rango** desde/hasta en **dd/mm/aaaa** (no una sola fecha mm/dd/yyyy) con calendario que no deja elegir días futuros; máximo 30 días. Si "Desde" pasa a "Hasta", "Hasta" se mueve con él.
  - **Franja horaria** obligatoria: Mañana, Tarde, Noche o No sé. `ValidateReportStepUseCase.lostRange()` convierte rango + franja en `lostFrom`/`lostTo` (lo usa L04).
  - **Foto de referencia opcional** desde la galería con el Photo Picker del sistema (`core/presentation/PhotoPicker`): **no pide permisos** de almacenamiento; la foto se copia a `filesDir/report_photos`. En iOS el botón se oculta (pendiente). La cámara llega con H01.
  - **Contacto**: correo prellenado desde el perfil (no editable) + teléfono/WhatsApp opcional (Bolivia, 8 dígitos, `+591` opcional).
- **Paso 4 · Confirmación** (Figma "Reportar - Confirmación"): resumen con **Editar** por sección (Detalles → paso 1, Atributos → paso 2, Cuándo y dónde / Contacto → paso 3). Editando desde el resumen, el botón dice "Volver al resumen" y regresa directo a la confirmación si todo sigue válido.
- **Enviar reporte**: `SubmitLostReportUseCase` revisa los pasos 1-3 (`upTo`), guarda el `LostReport` en Room con estado **BUSCANDO** (sede y dueño de la sesión, rango + franja → `lostFrom`/`lostTo`) y llama al hook `MatchingTrigger` (M01). Si el matching falla, el reporte igual queda guardado. Firestore llega con L06.
- **Pantalla de éxito** (no existe en el PDF; hecha con el DS): "¡Reporte enviado!", resumen, "te avisaremos si hay coincidencias", **Ver mis reportes** (reemplaza al wizard en el back stack) y **Volver al inicio**.
- `MatchingTrigger` (`core/domain/repository`) hoy es `PendingMatchingTrigger` (no hace nada) en `matchingModule`; M01 cambia solo esa implementación.
- Tests: `ValidateReportStepUseCaseTest` (9), `ReportWizardViewModelTest` (11), `SubmitLostReportUseCaseTest` (4). Reloj fijo inyectado (`Clock`/`TimeZone` de `coreModule`).

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

`core/data/local/AppDatabase.kt` (versión **2**), dueño único: **Integrante A**. Se crea por plataforma (`Database.android.kt` / `Database.ios.kt`) y Koin la entrega con `platformModule()`.

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

| Versión | Cambio | Migración |
|---|---|---|
| 1 | Esquema inicial (F06) | — |
| 2 | `found_items.expected_answers` (respuestas esperadas, `'[]'` por defecto) y `claims.failed_attempts` (`0`) para V01 | `AutoMigration(1, 2)`, probada en el emulador sobre una base v1 con datos |

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
