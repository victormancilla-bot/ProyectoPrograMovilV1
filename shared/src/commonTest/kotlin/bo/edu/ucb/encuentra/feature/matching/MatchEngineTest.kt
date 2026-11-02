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
    subcategory: Subcategory = Subcategory.CALCULADORA
) = LostReport(
    id = ItemId(id), ownerId = "u-mateo", title = "Calculadora", category = subcategory.category, subcategory = subcategory,
    color = color, brand = brand, description = "", campus = campus, zone = zone, locationDetail = null,
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
    subcategory: Subcategory = Subcategory.CALCULADORA
) = FoundItem(
    id = ItemId(id), title = "Calculadora Casio", category = subcategory.category, subcategory = subcategory, color = color,
    brand = brand, description = "", campus = campus, foundZone = zone, foundDetail = null, foundAt = foundAt,
    custodyPlace = CustodyPlace.OFICINA, custodyDetail = "", status = status, photoUrls = emptyList(),
    controlQuestions = listOf("¿Qué nombre tiene?"), registeredBy = "u-ana-staff", retentionUntil = foundAt + 90.days,
    createdAt = foundAt
)

class MatchEngineTest {

    @Test
    fun identicalObjectScoresOneHundred() {
        assertTrue(MatchEngine.isCandidate(lostReport(), foundItem()))
        assertEquals(100, MatchEngine.score(lostReport(), foundItem()))
    }

    @Test
    fun hardFilterDropsOtherCategoryCampusOrStatus() {
        val report = lostReport()
        assertFalse(MatchEngine.isCandidate(report, foundItem(subcategory = Subcategory.LAPTOP)))
        assertFalse(MatchEngine.isCandidate(report, foundItem(campus = Campus.COCHABAMBA)))
        assertFalse(MatchEngine.isCandidate(report, foundItem(status = ItemStatus.RECLAMADO)))
        assertFalse(MatchEngine.isCandidate(lostReport(status = ReportStatus.CERRADO), foundItem()))
    }

    @Test
    fun hardFilterChecksTheDateWindowAtItsEdges() {
        val report = lostReport()
        // Encontrado antes de perderse: imposible.
        assertFalse(MatchEngine.isCandidate(report, foundItem(foundAt = LOST_FROM - 1.hours)))
        // Justo al inicio del rango y justo al límite de 30 días: entran.
        assertTrue(MatchEngine.isCandidate(report, foundItem(foundAt = LOST_FROM)))
        assertTrue(MatchEngine.isCandidate(report, foundItem(foundAt = LOST_TO + MatchEngine.MAX_DAYS_TO_FIND)))
        // Una hora después del límite: fuera.
        assertFalse(MatchEngine.isCandidate(report, foundItem(foundAt = LOST_TO + MatchEngine.MAX_DAYS_TO_FIND + 1.hours)))
    }

    @Test
    fun colorBrandAndZoneWeighTheScore() {
        val report = lostReport()
        assertEquals(60, MatchEngine.score(report, foundItem(color = "Blanco")))
        assertEquals(75, MatchEngine.score(report, foundItem(brand = "Texas Instruments")))
        assertEquals(75, MatchEngine.score(report, foundItem(zone = CampusZone.CAFETERIA)))
        // Sin dato o "Otro lugar": la mitad (ni confirma ni descarta).
        assertEquals(80, MatchEngine.score(lostReport(color = null), foundItem()))
        assertEquals(87, MatchEngine.score(report, foundItem(brand = null)))
        assertEquals(87, MatchEngine.score(lostReport(zone = CampusZone.OTRO), foundItem()))
    }

    @Test
    fun textComparisonIgnoresCaseSpacesAndModelSuffix() {
        assertEquals(100, MatchEngine.score(lostReport(color = " negro ", brand = "CASIO fx-991"), foundItem()))
    }

    @Test
    fun foundLaterLosesDatePointsGradually() {
        val report = lostReport()
        assertEquals(97, MatchEngine.score(report, foundItem(foundAt = LOST_TO + 9.days)))
        assertEquals(90, MatchEngine.score(report, foundItem(foundAt = LOST_TO + 30.days)))
    }

    @Test
    fun rankKeepsOnlyCandidatesAboveTheMinimumBestFirst() {
        val items = listOf(
            foundItem(id = "otro-color", color = "Rojo"),
            foundItem(id = "igual"),
            foundItem(id = "nada-coincide", color = "Rojo", brand = "HP", zone = CampusZone.PARQUEO, foundAt = LOST_TO + 30.days),
            foundItem(id = "otra-cosa", subcategory = Subcategory.LAPTOP)
        )
        val ranked = MatchEngine.rank(lostReport(), items)
        assertEquals(listOf("igual" to 100, "otro-color" to 60), ranked.map { (item, score) -> item.id.value to score })
    }

    @Test
    fun rankFromAFoundItemFindsItsReports() {
        val reports = listOf(lostReport(id = "mio"), lostReport(id = "cerrado", status = ReportStatus.CERRADO))
        assertEquals(listOf("mio"), MatchEngine.rank(foundItem(), reports).map { it.first.id.value })
    }
}
