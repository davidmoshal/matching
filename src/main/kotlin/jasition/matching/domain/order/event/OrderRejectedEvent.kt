package jasition.matching.domain.order.event

import jasition.matching.domain.Event
import jasition.matching.domain.book.entry.OrderType
import jasition.matching.domain.book.entry.Side
import jasition.matching.domain.book.entry.TimeInForce
import jasition.matching.domain.client.Client
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