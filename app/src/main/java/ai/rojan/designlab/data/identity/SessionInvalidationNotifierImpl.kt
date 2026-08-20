package ai.rojan.designlab.data.identity

import ai.rojan.designlab.domain.identity.SessionInvalidationNotifier
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** A small replay-free buffer (capacity 1) so a `notifyInvalidated()` call that races a subscriber's collection start isn't silently lost. */
class SessionInvalidationNotifierImpl : SessionInvalidationNotifier {

    private val _invalidations = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val invalidations: SharedFlow<Unit> = _invalidations.asSharedFlow()

    override fun notifyInvalidated() {
        _invalidations.tryEmit(Unit)
    }
}
