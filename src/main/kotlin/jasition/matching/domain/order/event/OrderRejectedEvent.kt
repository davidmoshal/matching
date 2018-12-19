package jasition.matching.domain.order.event

import jasition.matching.domain.Event
import jasition.matching.domain.order.Client
import jasition.matching.domain.order.OrderType
import jasition.matching.domain.order.Side
import jasition.matching.domain.order.TimeInForce
import java.time.Instant

data class OrderRejectedEvent(
    val requestId: String,
    val whoRequested: Client,
    val bookId: String,
    val orderType: OrderType,
    val side: Side,
    val size: Int,
    val price: Long?,
    val timeInForce: TimeInForce,
    val whenHappened: Instant
) : Event