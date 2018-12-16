package jasition.matching.domain.order.event

import jasition.matching.domain.order.*
import java.time.Instant

data class OrderPlacedEvent(
        val requestId: String,
        val whoRequested: Client,
        val bookId: String,
        val orderType : OrderType,
        val side : Side,
        val availableSize : Int,
        val tradedSize : Int,
        val cancelledSize : Int,
        val price : Price?,
        val timeInForce: TimeInForce,
        val whenHappened : Instant
)