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

Usuario de prueba del login actual: `user@example.com` / `password`. Después del login aparece un *Menú de desarrollo* con todas las pantallas (stubs) hasta que F05 agregue la navegación por rol.

## Tests y calidad

```bash
./gradlew :shared:testAndroidHostTest   # tests de commonTest + androidHostTest
./gradlew :shared:detekt                # análisis estático (reporte en shared/build/reports/detekt)
```

## Design System

Es un proyecto aparte: [UcbEncuentraDesignSystem](https://github.com/MaiteAraucoPorrez/UcbEncuentraDesignSystem) (clónalo al lado de esta carpeta como `design-system`). Se publica en `ds-repo/` de este repo y la app lo usa como `edu.ucb.ds:shared:1.0.0-SNAPSHOT`, así nadie necesita `publishToMavenLocal`.

```kotlin
UcbEncuentraTheme.colors.primary          // primary, secondary, background, surface, error (+ onX)
UcbEncuentraTheme.colors.scoreColor(92)   // success >= 80, warning 50-79, neutral < 50
UcbEncuentraTheme.typography.heading      // heading, body, label (Inter)
UcbEncuentraTheme.spacing.large           // small 4 · medium 8 · large 16
UcbEncuentraTheme.shapes.pill             // small, medium, large, pill
UcbEncuentraTheme(darkTheme = isDark) { … }
```

Demo: menú de desarrollo → *Design System*. Para cambiar el DS ver `ds-repo/README.md`.
En Windows las rutas de `ds-repo/` son largas: `git config --global core.longpaths true`.

## Estructura

```
shared/src/commonMain/kotlin/bo/edu/ucb/encuentra/
├── App.kt
├── core/            # código común (config, data, presentation)
├── di/              # KoinInit + SharedModules (registra el módulo de cada feature)
├── navigation/      # NavRoute (@Serializable) + AppNavHost
└── feature/<x>/     # data / domain / presentation/{screen,viewmodel} / di
ds-repo/             # Design System publicado (no editar a mano)
```

Dependencias disponibles (todas en `gradle/libs.versions.toml`): Navigation Compose, kotlinx.serialization, Ktor, Coil 3, Room 3 + KSP, Firebase GitLive (Auth, Firestore, Storage, Messaging, Remote Config), WorkManager (Android), Wire/gRPC (los `.proto` van en `shared/src/commonMain/proto`) y detekt.

## Cómo contribuir

Ramas, commits, dueños por carpeta y Definition of Done: ver [CONTRIBUTING.md](CONTRIBUTING.md).
