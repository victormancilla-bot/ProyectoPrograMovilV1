package bo.edu.ucb.encuentra.core.data.local.seed

import bo.edu.ucb.encuentra.core.domain.model.AppNotification
import bo.edu.ucb.encuentra.core.domain.model.AuditAction
import bo.edu.ucb.encuentra.core.domain.model.AuditLog
import bo.edu.ucb.encuentra.core.domain.model.Campus
import bo.edu.ucb.encuentra.core.domain.model.CampusZone
import bo.edu.ucb.encuentra.core.domain.model.Category
import bo.edu.ucb.encuentra.core.domain.model.Claim
import bo.edu.ucb.encuentra.core.domain.model.ClaimStatus
import bo.edu.ucb.encuentra.core.domain.model.CustodyEvent
import bo.edu.ucb.encuentra.core.domain.model.CustodyPlace
import bo.edu.ucb.encuentra.core.domain.model.FoundItem
import bo.edu.ucb.encuentra.core.domain.model.ItemStatus
import bo.edu.ucb.encuentra.core.domain.model.LostReport
import bo.edu.ucb.encuentra.core.domain.model.Match
import bo.edu.ucb.encuentra.core.domain.model.MatchStatus
import bo.edu.ucb.encuentra.core.domain.model.NotificationType
import bo.edu.ucb.encuentra.core.domain.model.ReportStatus
import bo.edu.ucb.encuentra.core.domain.model.Role
import bo.edu.ucb.encuentra.core.domain.model.Subcategory
import bo.edu.ucb.encuentra.core.domain.model.UserModel
import bo.edu.ucb.encuentra.core.domain.vo.Email
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/**
 * Datos de prueba de 2026 coherentes con el mockup (Mis Reportes, Resultados de Matching, Dashboard,
 * Verificación de Reclamos). Los ids de los usuarios coinciden con las cuentas demo del login.
 * Se cargan una sola vez, cuando la base está vacía (DatabaseSeeder).
 */
object SeedData {

    private fun at(isoWithOffset: String): Instant = Instant.parse(isoWithOffset)

    private const val STAFF_ID = "u-ana-staff"
    private const val MATEO_ID = "u-mateo"
    private const val OFICINA = "Oficina de Objetos Perdidos · Bloque A"
    private val RETENTION = 90.days

    val users = listOf(
        UserModel(MATEO_ID, "Mateo Quispe", Email("estudiante@ucb.edu.bo"), Role.ESTUDIANTE, Campus.LA_PAZ, "Ingeniería"),
        UserModel(STAFF_ID, "Ana Staff", Email("staff@ucb.edu.bo"), Role.STAFF, Campus.LA_PAZ, "Objetos Perdidos"),
        UserModel("u-admin", "Admin UCB", Email("admin@ucb.edu.bo"), Role.ADMIN, Campus.LA_PAZ, "Administración"),
        UserModel("u-ana-gomez", "Ana Gómez", Email("ana.gomez@ucb.edu.bo"), Role.ESTUDIANTE, Campus.LA_PAZ, "Derecho"),
        UserModel("u-luis-mendoza", "Luis Mendoza", Email("luis.mendoza@ucb.edu.bo"), Role.ESTUDIANTE, Campus.LA_PAZ, "Ingeniería"),
        UserModel("u-carlos-mamani", "Carlos Mamani", Email("carlos.mamani@ucb.edu.bo"), Role.ESTUDIANTE, Campus.LA_PAZ, "Economía")
    )

    // ----- Reportes de objetos perdidos (Mis Reportes de Mateo) -----
    val lostReports = listOf(
        lostReport(
            "lr-laptop", "Laptop Dell XPS", Category.ELECTRONICA, Subcategory.LAPTOP, "Plateado", "Dell",
            "Laptop Dell XPS 13 plateada con sticker de la UCB en la tapa.", CampusZone.BIBLIOTECA_CENTRAL, "Piso 2",
            "2026-10-12T09:00:00-04:00", ReportStatus.BUSCANDO
        ),
        lostReport(
            "lr-lentes", "Lentes Ray-Ban", Category.ROPA_ACCESORIOS, Subcategory.LENTES, "Negro", "Ray-Ban",
            "Lentes de sol negros en estuche café.", CampusZone.CAFETERIA, null,
            "2026-10-10T13:00:00-04:00", ReportStatus.EN_VERIFICACION
        ),
        lostReport(
            "lr-cuaderno", "Cuaderno de Cálculo", Category.LIBROS_CUADERNOS, Subcategory.CUADERNO, "Gris", "Moleskine",
            "Cuaderno de tapa dura gris con apuntes de Cálculo II.", CampusZone.BLOQUE_D, "Aula 104",
            "2026-10-05T11:00:00-04:00", ReportStatus.MATCH_CONFIRMADO
        ),
        lostReport(
            "lr-botella", "Botella de Agua Azul", Category.OTROS, Subcategory.OTRO, "Azul", null,
            "Botella metálica azul con abolladura en la base.", CampusZone.CANCHAS_DEPORTIVAS, null,
            "2026-09-01T17:00:00-04:00", ReportStatus.VENCIDO
        ),
        lostReport(
            "lr-calculadora", "Calculadora científica", Category.ELECTRONICA, Subcategory.CALCULADORA, "Negro", "Casio",
            "Calculadora Casio fx-991 con mi nombre en la tapa.", CampusZone.BLOQUE_C, "Aula 201",
            "2026-10-11T15:00:00-04:00", ReportStatus.EN_VERIFICACION
        )
    )

    // ----- Objetos encontrados (Inventario / Dashboard) -----
    val foundItems = listOf(
        foundItem(
            "fi-calculadora", "Calculadora Científica Casio", Category.ELECTRONICA, Subcategory.CALCULADORA, "Negro", "Casio",
            "Calculadora Casio fx-991 con un nombre escrito en la tapa.", CampusZone.BLOQUE_C, "Aula 201",
            "2026-10-12T10:30:00-04:00", ItemStatus.RECLAMADO,
            listOf("¿Qué nombre está escrito en la tapa?" to "Mateo Quispe", "¿Qué modelo exacto es?" to "fx-991")
        ),
        foundItem(
            "fi-termo", "Termo Azul Oscuro", Category.ROPA_ACCESORIOS, Subcategory.TERMO, "Azul", "Stanley",
            "Termo azul oscuro de 1 litro.", CampusZone.CAFETERIA, null,
            "2026-10-10T12:15:00-04:00", ItemStatus.DISPONIBLE,
            listOf("¿Tiene algún sticker o marca?" to "Sticker de la UCB")
        ),
        foundItem(
            "fi-llavero", "Llavero con 3 llaves", Category.LLAVES, Subcategory.LLAVERO, "Plateado", null,
            "Llavero metálico con 3 llaves y un colgante de la UCB.", CampusZone.PARQUEO, null,
            "2026-08-08T18:00:00-04:00", ItemStatus.DISPONIBLE,
            listOf("¿Qué colgante tiene el llavero?" to "Escudo de la UCB")
        ),
        foundItem(
            "fi-billetera", "Billetera Negra de Cuero", Category.ROPA_ACCESORIOS, Subcategory.BILLETERA, "Negro", null,
            "Billetera de cuero negro con documentos.", CampusZone.BIBLIOTECA_CENTRAL, "Piso 1",
            "2026-10-30T16:00:00-04:00", ItemStatus.RECLAMADO,
            listOf("¿Qué documentos había dentro?" to "Carnet universitario y tarjeta de débito", "¿De qué color es el interior?" to "Café")
        ),
        foundItem(
            "fi-cuaderno-tapa-dura", "Cuaderno de Tapa Dura", Category.LIBROS_CUADERNOS, Subcategory.CUADERNO, "Gris", "Moleskine",
            "Se encontró en una mesa cerca de la entrada principal. Tiene algunas hojas arrancadas al principio.",
            CampusZone.CAFETERIA, "Cafetería Central", "2026-11-01T10:45:00-04:00", ItemStatus.DISPONIBLE,
            listOf("¿De qué materia son los apuntes?" to "Cálculo II")
        ),
        foundItem(
            "fi-apuntes-fisica", "Cuaderno de Apuntes - Física II", Category.LIBROS_CUADERNOS, Subcategory.APUNTES, "Azul", null,
            "Cuaderno anillado con apuntes de Física II.", CampusZone.BLOQUE_B, "Aula 305",
            "2026-10-28T09:20:00-04:00", ItemStatus.RECLAMADO,
            listOf("¿Cuál es el nombre del docente en la primera hoja?" to "Roberto Pérez")
        ),
        // D04: retención vencida (90 días) para la demo de Donar / Descartar.
        foundItem(
            "fi-paraguas", "Paraguas Negro Plegable", Category.OTROS, Subcategory.PARAGUAS, "Negro", null,
            "Paraguas plegable negro con mango de madera.", CampusZone.AUDITORIO, null,
            "2026-07-10T17:30:00-04:00", ItemStatus.DISPONIBLE,
            listOf("¿De qué material es el mango?" to "Madera")
        ),
        foundItem(
            "fi-chamarra", "Chamarra Deportiva Azul", Category.ROPA_ACCESORIOS, Subcategory.CHAMARRA, "Azul", "Adidas",
            "Chamarra deportiva azul talla M.", CampusZone.CANCHAS_DEPORTIVAS, null,
            "2026-07-25T19:00:00-04:00", ItemStatus.DISPONIBLE,
            listOf("¿Qué talla es?" to "M")
        )
    )

    /** Registro inicial de cada objeto + un movimiento (el llavero pasó a locker). */
    val custodyEvents: List<CustodyEvent> = foundItems.map { item ->
        CustodyEvent(
            id = ItemId("ce-${item.id.value}-1"),
            foundItemId = item.id,
            place = CustodyPlace.OFICINA,
            detail = OFICINA,
            changedBy = STAFF_ID,
            at = item.createdAt,
            note = "Registro inicial"
        )
    } + CustodyEvent(
        id = ItemId("ce-fi-llavero-2"),
        foundItemId = ItemId("fi-llavero"),
        place = CustodyPlace.LOCKER,
        detail = "Locker 12 · Bloque A",
        changedBy = STAFF_ID,
        at = at("2026-08-20T09:00:00-04:00"),
        note = "Movido a locker por espacio"
    )

    // ----- Coincidencias (Resultados de Matching: 92 / 65 / 30, y Detalle de Match 92) -----
    val matches = listOf(
        match("m-calculadora", "lr-calculadora", "fi-calculadora", 92, MatchStatus.PROPUESTO, "2026-10-12T11:00:00-04:00"),
        match("m-termo", "lr-calculadora", "fi-termo", 65, MatchStatus.PROPUESTO, "2026-10-12T11:00:00-04:00"),
        match("m-llavero", "lr-calculadora", "fi-llavero", 30, MatchStatus.PROPUESTO, "2026-10-12T11:00:00-04:00"),
        match("m-cuaderno", "lr-cuaderno", "fi-cuaderno-tapa-dura", 92, MatchStatus.CONFIRMADO, "2026-11-01T11:00:00-04:00")
    )

    // ----- Reclamos (Verificación de Reclamos #1024 y #1025, Dashboard "Últimos reclamos") -----
    val claims = listOf(
        claim(
            "c-1024", 1024, "fi-billetera", null, "u-ana-gomez", ClaimStatus.EN_VERIFICACION,
            listOf("Carnet universitario y una tarjeta de débito", "Café"), "2026-11-01T08:00:00-04:00"
        ),
        claim(
            "c-1025", 1025, "fi-calculadora", "m-calculadora", "u-luis-mendoza", ClaimStatus.PENDIENTE,
            emptyList(), "2026-11-01T05:00:00-04:00"
        ),
        claim(
            "c-1026", 1026, "fi-apuntes-fisica", null, "u-carlos-mamani", ClaimStatus.PENDIENTE,
            emptyList(), "2026-11-01T05:00:00-04:00"
        )
    )

    val auditLogs: List<AuditLog> = foundItems.map { item ->
        AuditLog(0, AuditAction.ITEM_REGISTERED, STAFF_ID, item.id.value, item.title, item.createdAt)
    } + listOf(
        AuditLog(0, AuditAction.CUSTODY_CHANGED, STAFF_ID, "fi-llavero", "Oficina → Locker 12", at("2026-08-20T09:00:00-04:00")),
        AuditLog(0, AuditAction.CLAIM_CREATED, "u-ana-gomez", "c-1024", "Reclamo #1024", at("2026-11-01T08:00:00-04:00")),
        AuditLog(0, AuditAction.CLAIM_ANSWERED, "u-ana-gomez", "c-1024", "Respondió 2 preguntas", at("2026-11-01T08:05:00-04:00")),
        AuditLog(0, AuditAction.CLAIM_CREATED, "u-luis-mendoza", "c-1025", "Reclamo #1025", at("2026-11-01T05:00:00-04:00")),
        AuditLog(0, AuditAction.CLAIM_CREATED, "u-carlos-mamani", "c-1026", "Reclamo #1026", at("2026-11-01T05:00:00-04:00"))
    )

    /** Home de Mateo: "1 coincidencia nueva". El deep link abre el Detalle de Match (F05). */
    val notifications = listOf(
        AppNotification(
            id = ItemId("n-match-calculadora"),
            userId = MATEO_ID,
            type = NotificationType.NEW_MATCH,
            title = "1 coincidencia nueva",
            body = "Encontramos una Calculadora Científica Casio que coincide en 92 %.",
            deepLink = "ucbencuentra://match/m-calculadora",
            read = false,
            createdAt = at("2026-10-12T11:00:00-04:00")
        ),
        AppNotification(
            id = ItemId("n-match-cuaderno"),
            userId = MATEO_ID,
            type = NotificationType.NEW_MATCH,
            title = "Match confirmado",
            body = "Tu Cuaderno de Cálculo coincide con un objeto en custodia.",
            deepLink = "ucbencuentra://match/m-cuaderno",
            read = true,
            createdAt = at("2026-11-01T11:00:00-04:00")
        )
    )

    private fun lostReport(
        id: String,
        title: String,
        category: Category,
        subcategory: Subcategory,
        color: String?,
        brand: String?,
        description: String,
        zone: CampusZone,
        detail: String?,
        reportedAt: String,
        status: ReportStatus
    ): LostReport {
        val reported = at(reportedAt)
        return LostReport(
            id = ItemId(id),
            ownerId = MATEO_ID,
            title = title,
            category = category,
            subcategory = subcategory,
            color = color,
            brand = brand,
            description = description,
            campus = Campus.LA_PAZ,
            zone = zone,
            locationDetail = detail,
            lostFrom = reported - 1.days,
            lostTo = reported,
            photoUrl = null,
            contactPhone = "+591 71234567",
            status = status,
            createdAt = reported
        )
    }

    private fun foundItem(
        id: String,
        title: String,
        category: Category,
        subcategory: Subcategory,
        color: String?,
        brand: String?,
        description: String,
        zone: CampusZone,
        detail: String?,
        foundAt: String,
        status: ItemStatus,
        /** Pregunta de control → respuesta esperada (privada, V01). */
        questions: List<Pair<String, String>>
    ): FoundItem {
        val found = at(foundAt)
        return FoundItem(
            id = ItemId(id),
            title = title,
            category = category,
            subcategory = subcategory,
            color = color,
            brand = brand,
            description = description,
            campus = Campus.LA_PAZ,
            foundZone = zone,
            foundDetail = detail,
            foundAt = found,
            custodyPlace = CustodyPlace.OFICINA,
            custodyDetail = OFICINA,
            status = status,
            photoUrls = emptyList(),
            controlQuestions = questions.map { it.first },
            expectedAnswers = questions.map { it.second },
            registeredBy = STAFF_ID,
            retentionUntil = found + RETENTION,
            createdAt = found
        )
    }

    private fun match(id: String, report: String, item: String, score: Int, status: MatchStatus, createdAt: String) = Match(
        id = ItemId(id),
        lostReportId = ItemId(report),
        foundItemId = ItemId(item),
        score = score,
        status = status,
        createdAt = at(createdAt)
    )

    @Suppress("LongParameterList")
    private fun claim(
        id: String,
        number: Int,
        item: String,
        matchId: String?,
        claimant: String,
        status: ClaimStatus,
        answers: List<String>,
        createdAt: String
    ) = Claim(
        id = ItemId(id),
        number = number,
        foundItemId = ItemId(item),
        matchId = matchId?.let(::ItemId),
        claimantId = claimant,
        status = status,
        controlAnswers = answers,
        createdAt = at(createdAt),
        reviewedBy = if (status == ClaimStatus.EN_VERIFICACION) STAFF_ID else null,
        reviewedAt = null,
        scheduledDeliveryAt = null,
        deliveredBy = null,
        deliveredAt = null
    )
}
