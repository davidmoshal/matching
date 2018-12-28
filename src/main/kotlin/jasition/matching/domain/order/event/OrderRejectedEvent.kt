package jasition.matching.domain.order.event

import jasition.matching.domain.Event
import jasition.matching.domain.EventId
import jasition.matching.domain.EventType
import jasition.matching.domain.Transaction
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.EntryType
import jasition.matching.domain.book.entry.Price
import jasition.matching.domain.book.entry.Side
import jasition.matching.domain.book.entry.TimeInForce
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
    val size: Int,
    val price: Price?,
    val timeInForce: TimeInForce,
    val whenHappened: Instant,
    val rejectReason: OrderRejectReason,
    val rejectText: String?
) : Event<BookId, Books> {
    override fun aggregateId(): BookId = bookId
    override fun eventId(): EventId = eventId
    override fun eventType(): EventType = EventType.PRIMARY

    override fun play(aggregate: Books): Transaction<BookId, Books> =
        Transaction(aggregate.copy(lastEventId = aggregate.verifyEventId(eventId)))
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
