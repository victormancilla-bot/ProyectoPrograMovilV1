package bo.edu.ucb.encuentra.core.data.local

import bo.edu.ucb.encuentra.core.data.local.mapper.toEntity
import bo.edu.ucb.encuentra.core.data.local.mapper.toModel
import bo.edu.ucb.encuentra.core.data.local.seed.SeedData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SeedAndMappersTest {

    @Test
    fun everyModelSurvivesTheRoundTripToEntity() {
        assertEquals(SeedData.users, SeedData.users.map { it.toEntity().toModel() })
        assertEquals(SeedData.lostReports, SeedData.lostReports.map { it.toEntity().toModel() })
        assertEquals(SeedData.foundItems, SeedData.foundItems.map { it.toEntity().toModel() })
        assertEquals(SeedData.custodyEvents, SeedData.custodyEvents.map { it.toEntity().toModel() })
        assertEquals(SeedData.matches, SeedData.matches.map { it.toEntity().toModel() })
        assertEquals(SeedData.claims, SeedData.claims.map { it.toEntity().toModel() })
        assertEquals(SeedData.auditLogs, SeedData.auditLogs.map { it.toEntity().toModel() })
        assertEquals(SeedData.notifications, SeedData.notifications.map { it.toEntity().toModel() })
    }

    @Test
    fun listsAreStoredAsJsonAndReadBack() {
        val questions = listOf("¿Qué nombre tiene?", "Con \"comillas\", y comas")
        assertEquals(questions, ListConverters.toStringList(ListConverters.fromStringList(questions)))
        assertEquals(emptyList(), ListConverters.toStringList(""))
    }

    @Test
    fun seedReferencesAreConsistent() {
        val userIds = SeedData.users.map { it.id }.toSet()
        val reportIds = SeedData.lostReports.map { it.id }.toSet()
        val itemIds = SeedData.foundItems.map { it.id }.toSet()
        val matchIds = SeedData.matches.map { it.id }.toSet()

        assertTrue(SeedData.lostReports.all { it.ownerId in userIds })
        assertTrue(SeedData.matches.all { it.lostReportId in reportIds && it.foundItemId in itemIds })
        assertTrue(SeedData.claims.all { it.foundItemId in itemIds && it.claimantId in userIds })
        assertTrue(SeedData.claims.all { claim -> claim.matchId == null || claim.matchId in matchIds })
        assertTrue(SeedData.custodyEvents.all { it.foundItemId in itemIds })
        assertTrue(SeedData.notifications.all { it.userId in userIds })
    }

    @Test
    fun seedRespectsUniqueConstraints() {
        assertEquals(SeedData.claims.size, SeedData.claims.map { it.number }.toSet().size)
        assertEquals(SeedData.claims.size, SeedData.claims.map { it.foundItemId to it.claimantId }.toSet().size)
        assertEquals(SeedData.matches.size, SeedData.matches.map { it.lostReportId to it.foundItemId }.toSet().size)
        assertEquals(SeedData.users.size, SeedData.users.map { it.email }.toSet().size)
        assertTrue(SeedData.users.all { it.email.isInstitutional() })
    }

    @Test
    fun seedDatesAreFrom2026() {
        assertTrue(SeedData.lostReports.all { it.createdAt.toString().startsWith("2026") })
        assertTrue(SeedData.foundItems.all { it.foundAt.toString().startsWith("2026") })
    }
}
