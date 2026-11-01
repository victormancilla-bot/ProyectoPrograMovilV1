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

## Estructura

```
shared/src/commonMain/kotlin/bo/edu/ucb/encuentra/
├── App.kt
├── core/            # código común (config, data, presentation)
├── di/              # KoinInit + SharedModules (registra el módulo de cada feature)
├── navigation/      # NavRoute (@Serializable) + AppNavHost
└── feature/<x>/     # data / domain / presentation/{screen,viewmodel} / di
```

Dependencias disponibles (todas en `gradle/libs.versions.toml`): Navigation Compose, kotlinx.serialization, Ktor, Coil 3, Room 3 + KSP, Firebase GitLive (Auth, Firestore, Storage, Messaging, Remote Config), WorkManager (Android), Wire/gRPC (los `.proto` van en `shared/src/commonMain/proto`) y detekt.

## Cómo contribuir

Ramas, commits, dueños por carpeta y Definition of Done: ver [CONTRIBUTING.md](CONTRIBUTING.md).
