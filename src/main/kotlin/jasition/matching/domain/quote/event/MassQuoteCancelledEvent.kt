package jasition.matching.domain.quote.event

import io.vavr.collection.List
import jasition.cqrs.Event
import jasition.cqrs.EventId
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.BookEntry
import jasition.matching.domain.book.verifyEventId
import jasition.matching.domain.client.Client
import java.time.Instant

data class MassQuoteCancelledEvent(
    val eventId: EventId,
    val bookId: BookId,
    val entries: List<BookEntry>,
    val whoRequested: Client,
    val whenHappened: Instant
) : Event<BookId, Books> {
    override fun aggregateId(): BookId = bookId
    override fun eventId(): EventId = eventId
    override fun play(aggregate: Books): Books = aggregate.removeBookEntries(
        entries = entries,
        eventId = aggregate verifyEventId eventId
    )
}
