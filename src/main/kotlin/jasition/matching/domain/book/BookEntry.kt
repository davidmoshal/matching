package jasition.matching.domain.book

import jasition.matching.domain.EventId
import jasition.matching.domain.order.*
import java.time.Instant

data class BookEntry(
    val key: BookEntryKey,
    val clientEntryId: ClientEntryId,
    val client: Client,
    val orderType: OrderType,
    val side: Side,
    val timeInForce: TimeInForce,
    val size: OrderQuantity,
    val status: OrderStatus
)

data class BookEntryKey(
    val price: Price?,
    val whenSubmitted: Instant,
    val eventId: EventId
)

data class ClientEntryId(
    val requestId: String,
    val originalRequestId: String? = null,
    val listId: String? = null
)