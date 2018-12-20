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
    val whenHappened: Instant
) : Event {

    fun toBookEntry(): BookEntry {
        return BookEntry(
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
            status = EntryStatus.NEW
        )
    }
}

fun playOrderPlacedEvent(event: OrderPlacedEvent, books: Books): Tuple2<List<Event>, Books> {
    return Tuple2(emptyList(), books.addBookEntry(event.toBookEntry()))
}