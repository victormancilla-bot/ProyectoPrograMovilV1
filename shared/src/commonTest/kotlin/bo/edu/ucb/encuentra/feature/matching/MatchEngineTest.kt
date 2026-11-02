package bo.edu.ucb.encuentra.feature.matching

import bo.edu.ucb.encuentra.core.domain.model.Campus
import bo.edu.ucb.encuentra.core.domain.model.CampusZone
import bo.edu.ucb.encuentra.core.domain.model.Category
import bo.edu.ucb.encuentra.core.domain.model.CustodyPlace
import bo.edu.ucb.encuentra.core.domain.model.FoundItem
import bo.edu.ucb.encuentra.core.domain.model.ItemStatus
import bo.edu.ucb.encuentra.core.domain.model.LostReport
import bo.edu.ucb.encuentra.core.domain.model.ReportStatus
import bo.edu.ucb.encuentra.core.domain.model.Subcategory
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.matching.domain.engine.MatchEngine
import bo.edu.ucb.encuentra.feature.matching.domain.model.MatchingConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

private val LOST_FROM = Instant.parse("2026-10-30T06:00:00-04:00")
private val LOST_TO = Instant.parse("2026-11-01T12:00:00-04:00")

internal fun lostReport(
    id: String = "lr-1",
    color: String? = "Negro",
    brand: String? = "Casio",
    zone: CampusZone = CampusZone.BLOQUE_C,
    status: ReportStatus = ReportStatus.BUSCANDO,
    campus: Campus = Campus.LA_PAZ,
    subcategory: Subcategory = Subcategory.CALCULADORA,
    title: String = "Calculadora",
    description: String = ""
) = LostReport(
    id = ItemId(id), ownerId = "u-mateo", title = title, category = subcategory.category, subcategory = subcategory,
    color = color, brand = brand, description = description, campus = campus, zone = zone, locationDetail = null,
    lostFrom = LOST_FROM, lostTo = LOST_TO, photoUrl = null, contactPhone = null, status = status, createdAt = LOST_TO
)

internal fun foundItem(
    id: String = "fi-1",
    color: String? = "Negro",
    brand: String? = "Casio",
    zone: CampusZone = CampusZone.BLOQUE_C,
    foundAt: Instant = LOST_TO - 1.hours,
    status: ItemStatus = ItemStatus.DISPONIBLE,
    campus: Campus = Campus.LA_PAZ,
    subcategory: Subcategory = Subcategory.CALCULADORA,
    title: String = "Calculadora Casio",
    description: String = ""
) = FoundItem(
    id = ItemId(id), title = title, category = subcategory.category, subcategory = subcategory, color = color,
    brand = brand, description = description, campus = campus, foundZone = zone, foundDetail = null, foundAt = foundAt,
    custodyPlace = CustodyPlace.OFICINA, custodyDetail = "", status = status, photoUrls = emptyList(),
    controlQuestions = listOf("¿Qué nombre tiene?"), registeredBy = "u-ana-staff", retentionUntil = foundAt + 90.days,
    createdAt = foundAt
)

/** Pesos de la fase 1 (M01): sin texto, para que estos casos sigan midiendo lo mismo. */
internal val PHASE1 = MatchingConfig(
    colorWeight = 40.0, brandWeight = 25.0, zoneWeight = 25.0, dateWeight = 10.0, textWeight = 0.0,
    minScore = 30, topN = 100, maxDaysToFind = 30
)

class MatchEngineTest {

    @Test
    fun identicalObjectScoresOneHundred() {
        assertTrue(MatchEngine.isCandidate(lostReport(), foundItem(), PHASE1))
        assertEquals(100, MatchEngine.score(lostReport(), foundItem(), PHASE1))
    }

    @Test
    fun hardFilterDropsOtherCategoryCampusOrStatus() {
        val report = lostReport()
        assertFalse(MatchEngine.isCandidate(report, foundItem(subcategory = Subcategory.LAPTOP), PHASE1))
        assertFalse(MatchEngine.isCandidate(report, foundItem(campus = Campus.COCHABAMBA), PHASE1))
        assertFalse(MatchEngine.isCandidate(report, foundItem(status = ItemStatus.RECLAMADO), PHASE1))
        assertFalse(MatchEngine.isCandidate(lostReport(status = ReportStatus.CERRADO), foundItem(), PHASE1))
    }

    @Test
    fun hardFilterChecksTheDateWindowAtItsEdges() {
        val report = lostReport()
        // Encontrado antes de perderse: imposible.
        assertFalse(MatchEngine.isCandidate(report, foundItem(foundAt = LOST_FROM - 1.hours), PHASE1))
        // Justo al inicio del rango y justo al límite de 30 días: entran.
        assertTrue(MatchEngine.isCandidate(report, foundItem(foundAt = LOST_FROM), PHASE1))
        assertTrue(MatchEngine.isCandidate(report, foundItem(foundAt = LOST_TO + 30.days), PHASE1))
        // Una hora después del límite: fuera.
        assertFalse(MatchEngine.isCandidate(report, foundItem(foundAt = LOST_TO + 30.days + 1.hours), PHASE1))
    }

    @Test
    fun colorBrandAndZoneWeighTheScore() {
        val report = lostReport()
        assertEquals(60, MatchEngine.score(report, foundItem(color = "Blanco"), PHASE1))
        assertEquals(75, MatchEngine.score(report, foundItem(brand = "Texas Instruments"), PHASE1))
        assertEquals(75, MatchEngine.score(report, foundItem(zone = CampusZone.CAFETERIA), PHASE1))
        // Sin dato o "Otro lugar": la mitad (ni confirma ni descarta).
        assertEquals(80, MatchEngine.score(lostReport(color = null), foundItem(), PHASE1))
        assertEquals(87, MatchEngine.score(report, foundItem(brand = null), PHASE1))
        assertEquals(87, MatchEngine.score(lostReport(zone = CampusZone.OTRO), foundItem(), PHASE1))
    }

    @Test
    fun textComparisonIgnoresCaseSpacesAndModelSuffix() {
        assertEquals(100, MatchEngine.score(lostReport(color = " negro ", brand = "CASIO fx-991"), foundItem(), PHASE1))
    }

    @Test
    fun foundLaterLosesDatePointsGradually() {
        val report = lostReport()
        assertEquals(97, MatchEngine.score(report, foundItem(foundAt = LOST_TO + 9.days), PHASE1))
        assertEquals(90, MatchEngine.score(report, foundItem(foundAt = LOST_TO + 30.days), PHASE1))
    }

    @Test
    fun rankKeepsOnlyCandidatesAboveTheMinimumBestFirst() {
        val items = listOf(
            foundItem(id = "otro-color", color = "Rojo"),
            foundItem(id = "igual"),
            foundItem(id = "nada-coincide", color = "Rojo", brand = "HP", zone = CampusZone.PARQUEO, foundAt = LOST_TO + 30.days),
            foundItem(id = "otra-cosa", subcategory = Subcategory.LAPTOP)
        )
        val ranked = MatchEngine.rank(lostReport(), items, PHASE1)
        assertEquals(listOf("igual" to 100, "otro-color" to 60), ranked.map { (item, score) -> item.id.value to score })
    }

    @Test
    fun rankFromAFoundItemFindsItsReports() {
        val reports = listOf(lostReport(id = "mio"), lostReport(id = "cerrado", status = ReportStatus.CERRADO))
        assertEquals(listOf("mio"), MatchEngine.candidateReports(foundItem(), reports, PHASE1).map { it.id.value })
    }
}
