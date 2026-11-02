package bo.edu.ucb.encuentra.feature.claims

import bo.edu.ucb.encuentra.core.data.local.seed.SeedData
import bo.edu.ucb.encuentra.core.domain.model.AuditAction
import bo.edu.ucb.encuentra.core.domain.model.Claim
import bo.edu.ucb.encuentra.core.domain.model.ClaimStatus
import bo.edu.ucb.encuentra.core.domain.model.CustodyEvent
import bo.edu.ucb.encuentra.core.domain.model.ItemStatus
import bo.edu.ucb.encuentra.core.domain.model.MatchStatus
import bo.edu.ucb.encuentra.core.domain.model.ReportStatus
import bo.edu.ucb.encuentra.core.domain.model.Role
import bo.edu.ucb.encuentra.core.domain.model.UserModel
import bo.edu.ucb.encuentra.core.domain.repository.AuditRecorder
import bo.edu.ucb.encuentra.core.domain.repository.ClaimRequestException
import bo.edu.ucb.encuentra.core.domain.repository.DuplicateClaimException
import bo.edu.ucb.encuentra.core.domain.repository.FoundItemLifecycleRepository
import bo.edu.ucb.encuentra.core.domain.repository.TransactionRunner
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.item.ItemErrors
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.claims.domain.model.ClaimErrors
import bo.edu.ucb.encuentra.feature.claims.domain.model.ClaimWithItem
import bo.edu.ucb.encuentra.feature.claims.domain.repository.ClaimRepository
import bo.edu.ucb.encuentra.feature.claims.domain.usecase.ApproveClaimUseCase
import bo.edu.ucb.encuentra.feature.claims.domain.usecase.ChangeClaimStatusUseCase
import bo.edu.ucb.encuentra.feature.claims.domain.usecase.CreateClaimUseCase
import bo.edu.ucb.encuentra.feature.claims.domain.usecase.DeliverClaimUseCase
import bo.edu.ucb.encuentra.feature.claims.domain.usecase.RejectClaimUseCase
import bo.edu.ucb.encuentra.feature.claims.domain.usecase.SubmitClaimAnswersUseCase
import bo.edu.ucb.encuentra.feature.foundform.FakeSession
import bo.edu.ucb.encuentra.feature.foundform.FixedClock
import bo.edu.ucb.encuentra.feature.foundform.demoUser
import bo.edu.ucb.encuentra.feature.myreports.mateo
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Reclamos, coincidencias, reportes y objetos del seed 2026 en memoria (como Room, con el índice único). */
internal class SeedClaimStore : ClaimRepository {
    val claims = SeedData.claims.associateBy { it.id }.toMutableMap()
    val matches = SeedData.matches.associateBy { it.id }.toMutableMap()
    val reports = SeedData.lostReports.associateBy { it.id }.toMutableMap()
    val items = SeedData.foundItems.associateBy { it.id }.toMutableMap()

    override suspend fun getById(id: ItemId) = claims[id]
    override suspend fun getByItem(itemId: ItemId) = claims.values.filter { it.foundItemId == itemId }
    override suspend fun getByClaimant(claimantId: String) = claims.values.filter { it.claimantId == claimantId }
    override suspend fun nextNumber() = (claims.values.maxOfOrNull { it.number } ?: 1000) + 1
    override suspend fun insert(claim: Claim) {
        check(claims.values.none { it.foundItemId == claim.foundItemId && it.claimantId == claim.claimantId }) { "UNIQUE constraint failed" }
        claims[claim.id] = claim
    }
    override suspend fun update(claim: Claim) {
        claims[claim.id] = claim
    }
    override suspend fun getMatch(id: ItemId) = matches[id]
    override suspend fun updateMatchStatus(id: ItemId, status: MatchStatus) {
        matches[id] = matches.getValue(id).copy(status = status)
    }
    override suspend fun getReport(id: ItemId) = reports[id]
    override suspend fun updateReportStatus(id: ItemId, status: ReportStatus) {
        reports[id] = reports.getValue(id).copy(status = status)
    }

    fun setItemStatus(id: ItemId, status: ItemStatus) {
        items[id] = items.getValue(id).copy(status = status)
    }

    override fun observeWithItem(claimId: ItemId) = flow {
        emit(claims[claimId]?.let { ClaimWithItem(it, items.getValue(it.foundItemId), NAMES[it.claimantId]) })
    }

    override fun observeOpen() = flow {
        emit(
            claims.values.filter { it.status == ClaimStatus.PENDIENTE || it.status == ClaimStatus.EN_VERIFICACION }
                .sortedWith(compareBy({ it.status != ClaimStatus.EN_VERIFICACION }, { it.createdAt }))
                .map { ClaimWithItem(it, items.getValue(it.foundItemId), NAMES[it.claimantId]) }
        )
    }

    fun item(id: String) = items.getValue(ItemId(id)).status
    fun report(id: String) = reports.getValue(ItemId(id)).status
    fun match(id: String) = matches.getValue(ItemId(id)).status
    fun claim(number: Int) = claims.values.first { it.number == number }
}

/** Los objetos del mismo store vistos como el repositorio de H05 (getById choca de tipo con ClaimRepository). */
internal class ItemsOf(private val store: SeedClaimStore) : FoundItemLifecycleRepository {
    override suspend fun getById(id: ItemId) = store.items[id]
    override suspend fun updateStatus(id: ItemId, status: ItemStatus) = store.setItemStatus(id, status)
    override suspend fun moveCustody(event: CustodyEvent) = Unit
    override fun observeCustodyHistory(id: ItemId) = flowOf(emptyList<CustodyEvent>())
}

internal class Audit : AuditRecorder {
    val entries = mutableListOf<Pair<AuditAction, String>>()
    override suspend fun record(action: AuditAction, actorId: String, targetId: String, detail: String) {
        entries += action to detail
    }
}

internal object Direct : TransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T = block()
}

private val NAMES = SeedData.users.associate { it.id to it.fullName }

internal val staff = demoUser(Role.STAFF)
internal val luis = demoUser(Role.ESTUDIANTE).copy(id = "u-luis-mendoza")

class ClaimUseCasesTest {

    private val store = SeedClaimStore()
    private val audit = Audit()
    private val session = FakeSession(mateo)
    private val create = CreateClaimUseCase(store, ItemsOf(store), audit, Direct, FixedClock)
    private val change = ChangeClaimStatusUseCase(store, ItemsOf(store), audit, Direct, GetCurrentUserUseCase(session), FixedClock)
    private val answer = SubmitClaimAnswersUseCase(change)
    private val approve = ApproveClaimUseCase(change)
    private val reject = RejectClaimUseCase(change)
    private val deliver = DeliverClaimUseCase(change)

    private suspend fun <T> asUser(user: UserModel, block: suspend () -> T): T {
        session.setCurrentUser(user)
        return block()
    }

    private suspend fun mateoClaims(matchId: String): Claim = store.claim(create.requestClaim(ItemId(matchId), mateo.id))

    @Test
    fun claimingAnAvailableItemCreatesAPendingClaimAndMarksTheItemClaimed() = runTest {
        store.updateReportStatus(ItemId("lr-calculadora"), ReportStatus.BUSCANDO)

        val number = create.requestClaim(ItemId("m-llavero"), mateo.id)

        assertEquals(1027, number)
        val claim = store.claim(1027)
        assertEquals(ClaimStatus.PENDIENTE, claim.status)
        assertEquals(ItemId("fi-llavero"), claim.foundItemId)
        assertEquals(ItemId("m-llavero"), claim.matchId)
        assertEquals(ItemStatus.RECLAMADO, store.item("fi-llavero"))
        assertEquals(ReportStatus.EN_VERIFICACION, store.report("lr-calculadora"))
        assertEquals(AuditAction.CLAIM_CREATED to "Reclamo #1027 · Llavero con 3 llaves · objeto Disponible → Reclamado", audit.entries.single())
    }

    @Test
    fun theSamePersonCannotClaimTheSameItemTwice() = runTest {
        create.requestClaim(ItemId("m-llavero"), mateo.id)

        val error = assertFailsWith<DuplicateClaimException> { create.requestClaim(ItemId("m-llavero"), mateo.id) }

        assertEquals("Ya reclamaste este objeto (Reclamo #1027).", error.message)
        assertEquals(1, store.claims.values.count { it.foundItemId == ItemId("fi-llavero") })
    }

    @Test
    fun anItemAlreadyClaimedBySomeoneElseCanStillBeClaimed() = runTest {
        // La calculadora ya tiene el reclamo #1025 de Luis: el dueño real no queda bloqueado.
        val claim = mateoClaims("m-calculadora")

        assertEquals(ClaimStatus.PENDIENTE, claim.status)
        assertEquals(ItemStatus.RECLAMADO, store.item("fi-calculadora"))
        assertEquals(2, store.claims.values.count { it.foundItemId == ItemId("fi-calculadora") })
    }

    @Test
    fun onlyTheReportOwnerCanClaimAnOpenMatchOfAnItemInCustody() = runTest {
        suspend fun refused(matchId: String, claimant: String) =
            assertFailsWith<ClaimRequestException> { create.requestClaim(ItemId(matchId), claimant) }.message

        assertEquals(ClaimErrors.NOT_YOUR_REPORT, refused("m-llavero", luis.id))
        assertEquals(ClaimErrors.MATCH_CLOSED, refused("m-cuaderno", mateo.id))
        assertEquals(ClaimErrors.MATCH_NOT_FOUND, refused("m-no-existe", mateo.id))
        store.setItemStatus(ItemId("fi-llavero"), ItemStatus.DONADO)
        assertEquals(ClaimErrors.ITEM_UNAVAILABLE, refused("m-llavero", mateo.id))
        assertTrue(audit.entries.isEmpty())
    }

    @Test
    fun fullFlowFromClaimToDelivery() = runTest {
        val claim = mateoClaims("m-llavero")

        val answered = answer(claim.id, listOf(" Un colgante de la UCB ")).getOrThrow()
        assertEquals(ClaimStatus.EN_VERIFICACION, answered.status)
        assertEquals(listOf("Un colgante de la UCB"), answered.controlAnswers)

        val approved = asUser(staff) { approve(claim.id).getOrThrow() }
        assertEquals(ClaimStatus.APROBADO, approved.status)
        assertEquals(staff.id, approved.reviewedBy)
        assertEquals(MatchStatus.CONFIRMADO, store.match("m-llavero"))
        assertEquals(ReportStatus.MATCH_CONFIRMADO, store.report("lr-calculadora"))
        assertEquals(ItemStatus.RECLAMADO, store.item("fi-llavero"))

        val delivered = asUser(staff) { deliver(claim.id, note = "Presentó su carnet").getOrThrow() }
        assertEquals(ClaimStatus.ENTREGADO, delivered.status)
        assertEquals(staff.id, delivered.deliveredBy)
        assertEquals(ItemStatus.ENTREGADO, store.item("fi-llavero"))
        assertEquals(ReportStatus.RECUPERADO, store.report("lr-calculadora"))

        assertEquals(
            listOf(AuditAction.CLAIM_CREATED, AuditAction.CLAIM_ANSWERED, AuditAction.CLAIM_APPROVED, AuditAction.ITEM_DELIVERED),
            audit.entries.map { it.first }
        )
        assertEquals("Reclamo #1027 · Aprobado → Entregado · Presentó su carnet", audit.entries.last().second)
    }

    @Test
    fun onlyStaffReviewsAndOnlyTheClaimantAnswers() = runTest {
        val claim = mateoClaims("m-llavero")

        assertEquals(ClaimErrors.ANSWERS_REQUIRED, answer(claim.id, listOf("  ")).exceptionOrNull()?.message)
        assertEquals(ItemErrors.NOT_STAFF, reject(claim.id).exceptionOrNull()?.message)
        assertEquals(ClaimErrors.NOT_YOUR_CLAIM, asUser(luis) { answer(claim.id, listOf("x")).exceptionOrNull()?.message })
        assertEquals(
            "No se puede aprobar un reclamo en estado \"Pendiente\".",
            asUser(staff) { approve(claim.id).exceptionOrNull()?.message }
        )
        assertEquals(ClaimStatus.PENDIENTE, store.claim(claim.number).status)
    }

    @Test
    fun rejectingReleasesTheItemAndTheReportSearchesAgain() = runTest {
        val claim = mateoClaims("m-llavero")

        val rejected = asUser(staff) { reject(claim.id, note = "No supo describir el colgante").getOrThrow() }

        assertEquals(ClaimStatus.RECHAZADO, rejected.status)
        assertEquals(MatchStatus.DESCARTADO, store.match("m-llavero"))
        assertEquals(ItemStatus.DISPONIBLE, store.item("fi-llavero"))
        assertEquals(ReportStatus.BUSCANDO, store.report("lr-calculadora"))
    }

    @Test
    fun rejectingOneClaimKeepsTheItemClaimedWhileAnotherIsOpen() = runTest {
        val mateoClaim = mateoClaims("m-calculadora")

        asUser(staff) { reject(ItemId("c-1025")).getOrThrow() }

        // El #1025 de Luis apunta (por error del seed) a la coincidencia de Mateo: no se la toca.
        assertEquals(MatchStatus.PROPUESTO, store.match("m-calculadora"))
        assertEquals(ReportStatus.EN_VERIFICACION, store.report("lr-calculadora"))
        assertEquals(ItemStatus.RECLAMADO, store.item("fi-calculadora"))
        assertEquals(ClaimStatus.PENDIENTE, store.claim(mateoClaim.number).status)
    }

    @Test
    fun deliveringRejectsTheOtherOpenClaimsAndOnlyOneCanBeApproved() = runTest {
        val mateoClaim = mateoClaims("m-calculadora")
        answer(mateoClaim.id, listOf("Mateo Quispe", "fx-991")).getOrThrow()
        // Luis también acierta (p. ej. vio la tapa): las dos respuestas coinciden y decide el staff.
        asUser(luis) { answer(ItemId("c-1025"), listOf("Mateo Quispe", "Casio fx-991")).getOrThrow() }

        asUser(staff) { approve(mateoClaim.id).getOrThrow() }
        val second = asUser(staff) { approve(ItemId("c-1025")).exceptionOrNull() }
        assertIs<ClaimRequestException>(second)
        assertEquals(ClaimErrors.ALREADY_APPROVED, second.message)

        asUser(staff) { deliver(mateoClaim.id).getOrThrow() }

        assertEquals(ClaimStatus.RECHAZADO, store.claim(1025).status)
        assertEquals(ItemStatus.ENTREGADO, store.item("fi-calculadora"))
        assertTrue(audit.entries.any { it.second == "Reclamo #1025 · el objeto se entregó con el reclamo #1027" })
    }
}
