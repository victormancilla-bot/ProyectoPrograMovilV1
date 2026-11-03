package bo.edu.ucb.encuentra.feature.foundcatalog.domain.usecase

import bo.edu.ucb.encuentra.core.domain.model.AppConfig
import bo.edu.ucb.encuentra.core.domain.usecase.GetAppConfigUseCase
import bo.edu.ucb.encuentra.feature.foundcatalog.domain.model.CatalogItem
import bo.edu.ucb.encuentra.feature.foundcatalog.domain.repository.FoundCatalogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Catálogo de objetos encontrados, del más reciente al más antiguo. */
class ObserveFoundCatalogUseCase(private val repository: FoundCatalogRepository) {
    operator fun invoke(): Flow<List<CatalogItem>> =
        repository.observeAvailable().map { items -> items.sortedByDescending { it.foundAt } }
}

/** Un paso de "Cómo funciona". */
data class HowItWorksStep(val title: String, val body: String)

/**
 * "Cómo funciona" en 4 pasos. Los días de retención y el contacto salen de Remote Config (F08), así el texto
 * no queda desactualizado si Objetos Perdidos cambia la política.
 */
class GetHowItWorksUseCase(private val getAppConfig: GetAppConfigUseCase) {
    operator fun invoke(): List<HowItWorksStep> = stepsFor(getAppConfig())

    companion object {
        fun stepsFor(config: AppConfig) = listOf(
            HowItWorksStep(
                "Reporta lo que perdiste",
                "Elige la categoría y cuenta cómo es, dónde y cuándo lo perdiste. Mientras más detalles, mejor."
            ),
            HowItWorksStep(
                "Te avisamos si aparece",
                "Comparamos tu reporte con lo que registra Objetos Perdidos y te notificamos cuando hay una coincidencia."
            ),
            HowItWorksStep(
                "Demuestra que es tuyo",
                "Responde las preguntas de control sobre detalles que solo el dueño conoce. El staff revisa tu reclamo."
            ),
            HowItWorksStep(
                "Recógelo con tu carnet",
                "Cuando te aprueben, pasa por Objetos Perdidos con tu carnet universitario. Los objetos se guardan " +
                    "${config.retentionDays} días; después se donan o se descartan."
            )
        )
    }
}
