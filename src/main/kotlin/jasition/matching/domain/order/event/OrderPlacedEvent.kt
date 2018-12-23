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
) : Event {
    override fun eventId(): EventId = eventId
    override fun type(): EventType = EventType.PRIMARY

    fun toBookEntry(): BookEntry = BookEntry(
        key = BookEntryKey(
            price = price,
            whenSubmitted = whenHappened,
            eventId = eventId
        ),
        clientRequestId = requestId,
        client = whoRequested,
        entryType = entryType,
        side = side,
        timeInForce = timeInForce,
        size = size,
        status = entryStatus
    )
}

fun play(event: OrderPlacedEvent, books: Books): Transaction<Books> {
    val result = match(aggressor = event.toBookEntry(), books = books.withEventId(books.verifyEventId(event.eventId)))
    val entry = result.a
    val matchTransaction = result.b

    if (entry.timeInForce.canStayOnBook(entry.size)) {
        val maxEventId = matchTransaction.maxEventId(event.eventId)
        val newAggregate = matchTransaction.aggregate.addBookEntry(entry.withEventId(maxEventId))
        return matchTransaction.withAggregate(newAggregate)
    }
    return matchTransaction
}