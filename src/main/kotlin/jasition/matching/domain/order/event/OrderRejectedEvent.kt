package jasition.matching.domain.order.event

import jasition.matching.domain.Event
import jasition.matching.domain.EventId
import jasition.matching.domain.EventType
import jasition.matching.domain.Transaction
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
) : Event {
    override fun eventId(): EventId = eventId
    override fun type(): EventType = EventType.PRIMARY
}

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

fun orderRejected(event: OrderRejectedEvent, books: Books): Transaction<Books> =
    Transaction(books.withEventId(books.verifyEventId(event.eventId)))
