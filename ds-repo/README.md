# ds-repo

Repositorio Maven con el Design System (`edu.ucb.ds:shared:1.0.0-SNAPSHOT`).
Está dentro del repo para que la app compile en cualquier computadora sin ejecutar
`publishToMavenLocal` antes. `settings.gradle.kts` lo agrega en `dependencyResolutionManagement`.

No se edita a mano. Si cambias el proyecto `design-system` (carpeta vecina, repo https://github.com/MaiteAraucoPorrez/UcbEncuentraDesignSystem), vuelve a publicar:

```
cd ../design-system
./gradlew :shared:publishAllPublicationsToAppRepoRepository
```

y haz commit de los archivos nuevos de esta carpeta.
