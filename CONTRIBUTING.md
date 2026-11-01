# Cómo trabajamos — UCB Encuentra

## Ramas (Gitflow)

| Rama | Para qué | Sale de | Se mezcla en | Quién |
|---|---|---|---|---|
| `main` | Lo que se presenta/publica. Cada merge lleva tag (`v0.1`, `v1.0`…) | — | — | Solo vía PR desde `release/*` o `hotfix/*` |
| `develop` | Integración diaria del equipo | `main` | `qa` / `release/*` | PR + 1 aprobación |
| `qa` | Pruebas del equipo antes de una presentación | `develop` | `release/*` | PR desde `develop` |
| `feature/<ID>-nombre` | Una tarjeta de Trello (ej. `feature/L02-wizard-paso1-2`) | `develop` | `develop` | Dueño de la tarjeta |
| `release/x.y` | Congelar una versión (ej. `release/0.3` antes del 12/11) | `develop` o `qa` | `main` **y** `develop` | Integrante A |
| `hotfix/<nombre>` | Bug urgente en lo que ya está en `main` | `main` | `main` **y** `develop` | Quien lo arregle |

Flujo normal:

```bash
git switch develop && git pull
git switch -c feature/L02-wizard-paso1-2
# ...commits...
git push -u origin feature/L02-wizard-paso1-2
# Abrir PR hacia develop en GitHub
```

Reglas:
- Nadie hace push directo a `main`, `develop` ni `qa`.
- Antes de abrir el PR: `git pull origin develop` en tu rama y resuelve conflictos tú.
- Borra la rama `feature/*` después del merge.

Hitos:
- **10/11 (noche):** se congela `develop` → `release/0.3` → `main` con tag `v0.3` para la Presentación 3 (12/11).
- **07/12:** se congela el código → `release/1.0` → `main` con tag `v1.0` para el entregable final (11/12).

## Commits

Formato `tipo(scope): Verbo en pasado ...` (en inglés), donde `scope` es la feature o la tarjeta. Commits pequeños: uno por paso que compile.

```
feat(reportwizard): Added step 1 with category grid
fix(login): Fixed validation to reject emails outside @ucb.edu.bo
refactor(matching): Moved score calculation to a use case
test(foundform): Added state machine test cases
docs(readme): Added Firebase setup steps
chore(deps): Updated ktor to 3.6.0
```

Tipos: `feat`, `fix`, `refactor`, `test`, `docs`, `style`, `chore`, `build`, `ci`.

## Estructura y dueños

Cada feature vive en `shared/src/commonMain/kotlin/bo/edu/ucb/encuentra/feature/<x>/`:

```
feature/<x>/
├── data/            # repositorios impl, DAOs, DTOs, mappers
├── domain/          # modelos, interfaces de repositorio, use cases, VOs
├── presentation/
│   ├── screen/      # @Composable XxxScreen
│   └── viewmodel/   # XxxViewModel + XxxState / XxxEvent / XxxEffect (MVI)
└── di/XxxModule.kt  # módulo de Koin de la feature
```

| Integrante | Features (carpetas) |
|---|---|
| A · Plataforma, datos y panel admin | `dashboard`, `claims`, `core/data` (AppDatabase, Firestore, WorkManager, gRPC) |
| B · Acceso, seguridad y verificación | `login`, `verify`, `delivery`, `audit`, `navigation` (F05) |
| C · Reporte de perdido y notificaciones | `home`, `reportwizard`, `myreports`, `foundcatalog`, `alerts` |
| D · Hallazgo con cámara e IA de visión | `foundcapture`, `foundform`, `inventory` |
| E · Design System, matching y mapas | `matching`, `profile`, design system |

Solo tocas tus carpetas. Estos archivos son compartidos y **se avisa en el grupo antes de cambiarlos** (y van en un PR aparte):

- `gradle/libs.versions.toml` y los `build.gradle.kts`
- `navigation/NavRoute.kt` (dueño: B)
- `core/data/local/AppDatabase.kt` (dueño: A, llega con F06)
- `di/SharedModules.kt` (ya registra todos los módulos; normalmente no hace falta tocarlo)

Para registrar algo nuevo en Koin, agrégalo al `XxxModule.kt` de tu feature, no a `SharedModules`.

## Definition of Done

Una tarjeta pasa a **Hecho** cuando:

1. Cumple todo el checklist de la tarjeta en Trello.
2. Compila en `devDebug` y `prodDebug`.
3. Los use cases nuevos tienen test en `commonTest` y `./gradlew :shared:testAndroidHostTest` pasa.
4. `./gradlew :shared:detekt` no agrega issues nuevos.
5. La UI sigue el PDF de pantallas y usa los componentes del Design System.
6. Sin textos de prueba, `println` ni claves en el código.
7. PR revisado y aprobado por 1 integrante y mezclado en `develop`.
