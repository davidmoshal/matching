package jasition.matching.domain.book.event

import io.vavr.collection.List
import jasition.cqrs.Event
import jasition.cqrs.EventId
import jasition.cqrs.Transaction
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.BookEntry
import java.time.Instant

data class EntriesRemovedFromBookEvent(
    val eventId: EventId,
    val bookId: BookId,
    val entries: List<BookEntry>,
    val whenHappened: Instant
) : Event<BookId, Books> {
    override fun aggregateId(): BookId = bookId
    override fun eventId(): EventId = eventId
    override fun isPrimary(): Boolean = false

    override fun play(aggregate: Books): Transaction<BookId, Books> =
        Transaction(aggregate.removeBookEntries(aggregate.verifyEventId(eventId),
            entries))
}
