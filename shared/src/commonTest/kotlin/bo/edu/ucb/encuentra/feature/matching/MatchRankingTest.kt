package bo.edu.ucb.encuentra.feature.matching

import bo.edu.ucb.encuentra.core.domain.model.CampusZone
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.matching.domain.engine.MatchEngine
import bo.edu.ucb.encuentra.feature.matching.domain.model.MatchingConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

/** M02: score ponderado con texto, Top-N con umbral, descartados fuera y pesos configurables. */
class MatchRankingTest {

    private val report = lostReport(
        title = "Calculadora científica",
        description = "Casio fx-991 con mi nombre escrito en la tapa"
    )

    @Test
    fun textSimilarityUsesKeywordsWithoutAccentsOrStopwords() {
        assertEquals(setOf("calculadora", "cientifica"), MatchEngine.keywords("¡Calculadora CIENTÍFICA con la tapa!") - setOf("tapa"))
        assertEquals(1.0, MatchEngine.textSimilarity("Calculadora Casio", "Encontré una calculadora casio fx-991 en el aula"))
        assertEquals(0.0, MatchEngine.textSimilarity("Calculadora Casio", "Termo azul de un litro"))
        assertEquals(0.5, MatchEngine.textSimilarity("Calculadora", "y la"))
    }

    @Test
    fun defaultWeightsAddTheTextCriterion() {
        val same = foundItem(title = "Calculadora Casio fx-991", description = "Tiene un nombre escrito en la tapa")
        assertEquals(100, MatchEngine.score(report, same))
        // Igual en todo menos el texto: pierde los 20 puntos de texto.
        val otherText = foundItem(title = "Calculadora gráfica", description = "Pantalla grande")
        assertEquals(85, MatchEngine.score(report, otherText))
    }

    @Test
    fun rankingIsOrderedAndKeepsOnlyTheTopN() {
        val items = listOf(
            foundItem(id = "rojo", color = "Rojo"),
            foundItem(id = "igual"),
            foundItem(id = "otra-marca", brand = "HP"),
            foundItem(id = "otra-zona", zone = CampusZone.PARQUEO),
            foundItem(id = "tarde", foundAt = report.lostTo + 20.days)
        )
        val config = MatchingConfig(topN = 3)
        val ranked = MatchEngine.rank(report, items, config).map { it.first.id.value }
        assertEquals(3, ranked.size)
        assertEquals("igual", ranked.first())
        // Los que más pierden (color 30 puntos) quedan fuera del top 3.
        assertTrue("rojo" !in ranked)
    }

    @Test
    fun minimumScoreDropsWeakCandidates() {
        val weak = foundItem(id = "debil", color = "Rojo", brand = "HP", zone = CampusZone.PARQUEO)
        assertTrue(MatchEngine.rank(report, listOf(weak), MatchingConfig(minScore = 50)).isEmpty())
        assertEquals(1, MatchEngine.rank(report, listOf(weak), MatchingConfig(minScore = 0)).size)
    }

    @Test
    fun rejectedItemsNeverComeBack() {
        val items = listOf(foundItem(id = "no-es-mio"), foundItem(id = "otro", color = "Gris"))
        val ranked = MatchEngine.rank(report, items, excluded = setOf(ItemId("no-es-mio")))
        assertEquals(listOf("otro"), ranked.map { it.first.id.value })
    }

    @Test
    fun weightsFromRemoteConfigChangeTheRanking() {
        val wrongColor = foundItem(id = "otro-color", color = "Blanco")
        val wrongZone = foundItem(id = "otra-zona", zone = CampusZone.PARQUEO)
        // Con color pesando mucho, gana el que acierta el color (aunque esté en otra zona)...
        val colorHeavy = MatchingConfig(colorWeight = 80.0, zoneWeight = 5.0)
        assertEquals("otra-zona", MatchEngine.rank(report, listOf(wrongColor, wrongZone), colorHeavy).first().first.id.value)
        // ...y con zona pesando mucho, al revés.
        val zoneHeavy = MatchingConfig(colorWeight = 5.0, zoneWeight = 80.0)
        assertEquals("otro-color", MatchEngine.rank(report, listOf(wrongColor, wrongZone), zoneHeavy).first().first.id.value)
    }

    @Test
    fun invalidRemoteValuesFallBackToSafeOnes() {
        assertEquals(MatchingConfig.DEFAULT, MatchingConfig(colorWeight = -1.0).sanitized())
        assertEquals(MatchingConfig.DEFAULT, MatchingConfig(0.0, 0.0, 0.0, 0.0, 0.0).sanitized())
        val clamped = MatchingConfig(minScore = 150, topN = 0).sanitized()
        assertEquals(100, clamped.minScore)
        assertEquals(1, clamped.topN)
    }
}
