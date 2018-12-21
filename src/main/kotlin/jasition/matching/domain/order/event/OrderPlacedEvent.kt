package jasition.matching.domain.order.event

import arrow.core.Tuple2
import io.vavr.collection.List
import jasition.matching.domain.Event
import jasition.matching.domain.EventId
import jasition.matching.domain.Transaction
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.client.Client
import jasition.matching.domain.client.ClientRequestId
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
    books.verifyEventId(event.eventId)

    val result = books.match(event.toBookEntry())
    val entry = result.a
    val transaction = result.b

    return if (entry.timeInForce.canStayOnBook(entry.size)) transaction.append(books.addBookEntry(entry)) else transaction
}