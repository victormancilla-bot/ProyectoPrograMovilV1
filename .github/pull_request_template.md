## Tarjeta
<!-- Ej: F01 · Preparar el repo del equipo (link a Trello) -->

## Qué cambia
-

## Cómo probarlo
1. Build variant `devDebug`
2.

## Capturas (si cambia UI)

## Definition of Done
- [ ] La rama sale de `develop` y se llama `feature/<ID>-nombre`
- [ ] Compila: `./gradlew :androidApp:assembleDevDebug`
- [ ] Tests pasan: `./gradlew :shared:testAndroidHostTest`
- [ ] `./gradlew :shared:detekt` sin issues nuevos
- [ ] Solo toqué archivos de mi feature (o avisé al equipo si toqué `libs.versions.toml`, `NavRoute`, `AppDatabase` o `SharedModules`)
- [ ] Sin claves ni `local.properties` / `google-services.json` con secretos en el diff
- [ ] Commits con formato `tipo(scope): mensaje`
- [ ] La tarjeta de Trello está en "En revisión (PR)"
