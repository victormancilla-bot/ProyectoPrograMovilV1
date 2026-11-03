package bo.edu.ucb.encuentra.core

import bo.edu.ucb.encuentra.core.domain.model.AppNotification
import bo.edu.ucb.encuentra.core.domain.model.NotificationType
import bo.edu.ucb.encuentra.core.domain.repository.LocalNotifier
import bo.edu.ucb.encuentra.core.domain.repository.NotificationRepository
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.NotificationCenter
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.foundform.FakeSession
import bo.edu.ucb.encuentra.feature.foundform.FixedClock
import bo.edu.ucb.encuentra.feature.myreports.mateo
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

/** Room + DataStore en memoria. */
internal class InMemoryNotifications : NotificationRepository {
    val saved = mutableMapOf<ItemId, AppNotification>()
    val shown = mutableMapOf<String, Long>()
    override suspend fun save(notifications: List<AppNotification>) = notifications.forEach { saved[it.id] = it }
    override suspend fun unreadSince(userId: String, sinceMillis: Long) = saved.values
        .filter { it.userId == userId && !it.read && it.createdAt.toEpochMilliseconds() > sinceMillis }
        .sortedBy { it.createdAt }
    override suspend fun shownUntil(userId: String) = shown[userId] ?: 0L
    override suspend fun setShownUntil(userId: String, millis: Long) {
        shown[userId] = millis
    }
}

/** Lo que se mostró en la barra del sistema. */
internal class RecordingNotifier : LocalNotifier {
    val shown = mutableListOf<AppNotification>()
    override fun show(notification: AppNotification) {
        shown += notification
    }
}

private fun notification(id: String, userId: String, minutesAgo: Int = 0, read: Boolean = false) = AppNotification(
    id = ItemId(id),
    userId = userId,
    type = NotificationType.CLAIM_UPDATE,
    title = "Aviso $id",
    body = "Cuerpo",
    deepLink = "ucbencuentra://match/m-$id",
    read = read,
    createdAt = FixedClock.now() - minutesAgo.minutes
)

class NotificationCenterTest {

    private val repository = InMemoryNotifications()
    private val notifier = RecordingNotifier()
    private val session = FakeSession(mateo)
    private val center = NotificationCenter(repository, notifier, GetCurrentUserUseCase(session))

    @Test
    fun everythingIsSavedButOnlyTheCurrentUserIsNotifiedNow() = runTest {
        center.publish(listOf(notification("a", "u-mateo"), notification("b", "u-ana-gomez")))

        assertEquals(setOf("a", "b"), repository.saved.keys.map { it.value }.toSet())
        assertEquals(listOf("a"), notifier.shown.map { it.id.value })
        // Lo ya mostrado a Mateo no se repite al volver a entrar.
        assertEquals(0, center.deliverPending())
    }

    @Test
    fun pendingNotificationsAreShownWhenTheirOwnerEnters() = runTest {
        // El staff aprueba el reclamo de Ana en este teléfono: no se le muestra al staff.
        session.setCurrentUser(mateo.copy(id = "u-ana-staff"))
        center.publish(notification("aprobado", "u-ana-gomez"))
        assertTrue(notifier.shown.isEmpty())

        // Entra Ana: le llega el aviso, una sola vez.
        session.setCurrentUser(mateo.copy(id = "u-ana-gomez"))
        assertEquals(1, center.deliverPending())
        assertEquals(listOf("aprobado"), notifier.shown.map { it.id.value })
        assertEquals(0, center.deliverPending())
    }

    @Test
    fun readNotificationsAreNotShownAndPendingAreCapped() = runTest {
        session.setCurrentUser(mateo.copy(id = "otro"))
        repository.save(listOf(notification("leida", "u-mateo", minutesAgo = 30, read = true)))
        repository.save((1..8).map { notification("n$it", "u-mateo", minutesAgo = 10 - it) })

        session.setCurrentUser(mateo)
        assertEquals(NotificationCenter.MAX_PENDING, center.deliverPending())

        // Las más recientes, sin la ya leída.
        assertEquals(listOf("n4", "n5", "n6", "n7", "n8"), notifier.shown.map { it.id.value })
    }

    @Test
    fun withoutSessionNothingIsShown() = runTest {
        session.clearSession()
        center.publish(notification("x", "u-mateo"))
        assertEquals(0, center.deliverPending())
        assertTrue(notifier.shown.isEmpty())
        assertEquals(1, repository.saved.size)
    }
}
