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
import jasition.matching.domain.trade.match
import java.time.Instant

data class OrderPlacedEvent(
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
    val entryStatus: EntryStatus = EntryStatus.NEW
) : Event<BookId, Books> {
    override fun aggregateId(): BookId = bookId
    override fun eventId(): EventId = eventId
    override fun eventType(): EventType = EventType.PRIMARY

    override fun play(aggregate: Books): Transaction<BookId, Books> {
        val (aggressor, transaction) = match(
            aggressor = toBookEntry(),
            books = aggregate.copy(lastEventId = aggregate.verifyEventId(eventId))
        )

        if (aggressor.timeInForce.canStayOnBook(aggressor.size)) {
            val newBooks = transaction.aggregate
            val nextEventId = newBooks.lastEventId.next()
            val entryAddedToBookEvent = aggressor.toEntryAddedToBookEvent(
                eventId = nextEventId,
                bookId = aggregate.bookId
            )

            return transaction
                .append(entryAddedToBookEvent)
                .append(entryAddedToBookEvent.play(newBooks))
        }
        return transaction
    }

    fun toBookEntry(): BookEntry = BookEntry(
        price = price,
        whenSubmitted = whenHappened,
        eventId = eventId,
        clientRequestId = requestId,
        client = whoRequested,
        entryType = entryType,
        side = side,
        timeInForce = timeInForce,
        size = size,
        status = entryStatus
    )
}

