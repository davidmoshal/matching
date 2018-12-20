package jasition.matching.domain.order.event

import arrow.core.Tuple2
import jasition.matching.domain.Event
import jasition.matching.domain.EventId
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.client.Client
import jasition.matching.domain.client.ClientRequestId
import java.time.Instant

data class OrderRejectedEvent(
    val eventId: EventId,
    val requestId: ClientRequestId,
    val whoRequested: Client,
    val bookId: BookId,
    val entryType: EntryType,
    val side: Side,
    val size: EntryQuantity,
    val price: Price?,
    val timeInForce: TimeInForce,
    val whenHappened: Instant,
    val rejectReason: OrderRejectReason,
    val rejectText: String?
) : Event

enum class OrderRejectReason {
    BROKER_EXCHANGE_OPTION,
    UNKNOWN_SYMBOL,
    EXCHANGE_CLOSED,
    UNKNOWN_ORDER,
    DUPLICATE_ORDER,
    UNSUPPORTED_ORDER_CHARACTERISTIC,
    INCORRECT_QUANTITY,
    UNKNOWN_ACCOUNTS,
    OTHER
}

fun playOrderRejectedEvent(event: OrderRejectedEvent, books: Books): Tuple2<List<Event>, Books> {
    return Tuple2(emptyList(), books + event.eventId)
}