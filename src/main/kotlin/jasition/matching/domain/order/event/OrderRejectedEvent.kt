package jasition.matching.domain.order.event

import jasition.matching.domain.order.*
import java.time.Instant

data class OrderRejectedEvent(
        val requestId: String,
        val whoRequested: Client,
        val bookId: String,
        val orderType : OrderType,
        val side : Side,
        val size : Int,
        val price : Price?,
        val timeInForce: TimeInForce,
        val whenHappened : Instant
)