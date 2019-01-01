package jasition.matching.domain.order.event

import jasition.cqrs.Event
import jasition.cqrs.EventId
import jasition.cqrs.Transaction
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.client.Client
import jasition.matching.domain.client.ClientRequestId
import jasition.matching.domain.trade.matchAndPlaceEntry
import java.time.Instant

data class OrderPlacedEvent(
    val eventId: EventId,
    val requestId: ClientRequestId,
    val whoRequested: Client,
    val bookId: BookId,
    val entryType: EntryType,
    val side: Side,
    val sizes: EntrySizes,
    val price: Price?,
    val timeInForce: TimeInForce,
    val whenHappened: Instant,
    val status: EntryStatus = EntryStatus.NEW
) : Event<BookId, Books> {
    override fun aggregateId(): BookId = bookId
    override fun eventId(): EventId = eventId
    override fun isPrimary(): Boolean = true

    override fun play(aggregate: Books): Transaction<BookId, Books> {
        return matchAndPlaceEntry(
            bookEntry = toBookEntry(),
            books = aggregate.copy(lastEventId = aggregate.verifyEventId(eventId))
        )
    }

    fun toBookEntry(): BookEntry = BookEntry(
        price = price,
        whenSubmitted = whenHappened,
        eventId = eventId,
        requestId = requestId,
        whoRequested = whoRequested,
        entryType = entryType,
        side = side,
        timeInForce = timeInForce,
        sizes = sizes,
        status = status
    )
}

