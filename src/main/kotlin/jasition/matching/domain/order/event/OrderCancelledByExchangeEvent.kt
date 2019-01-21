package jasition.matching.domain.order.event

import jasition.cqrs.Event
import jasition.cqrs.EventId
import jasition.cqrs.Transaction
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.client.Client
import jasition.matching.domain.client.ClientRequestId
import java.time.Instant
import java.util.function.Predicate

data class OrderCancelledByExchangeEvent(
    val eventId: EventId,
    val requestId: ClientRequestId,
    val whoRequested: Client,
    val bookId: BookId,
    val entryType: EntryType,
    val side: Side,
    val sizes: EntrySizes,
    val price: Price?,
    val timeInForce: TimeInForce,
    val status : EntryStatus,
    val whenHappened: Instant
) : Event<BookId, Books> {
    override fun aggregateId(): BookId = bookId
    override fun eventId(): EventId = eventId
    override fun isPrimary(): Boolean = false

    override fun play(aggregate: Books): Transaction<BookId, Books> = Transaction(
        aggregate.removeBookEntries(eventId = aggregate.verifyEventId(eventId),
            side = side,
            predicate = Predicate {
                it.whoRequested == whoRequested
                        && (it.requestId.current == requestId.current)
            })
    )
}
