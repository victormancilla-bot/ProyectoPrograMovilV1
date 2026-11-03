package bo.edu.ucb.encuentra.core

import bo.edu.ucb.encuentra.core.presentation.MessageKind
import bo.edu.ucb.encuentra.core.presentation.UserMessage
import bo.edu.ucb.encuentra.core.presentation.UserMessages
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** Snackbar de prueba: guarda lo que se mostró. */
internal class RecordingMessages : UserMessages {
    val shown = mutableListOf<UserMessage>()
    override val messages: Flow<UserMessage> = emptyFlow()
    override fun show(text: String, kind: MessageKind) {
        shown += UserMessage(text, kind)
    }
    val last: UserMessage? get() = shown.lastOrNull()
}
