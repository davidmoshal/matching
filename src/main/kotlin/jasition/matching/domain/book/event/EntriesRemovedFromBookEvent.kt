package jasition.matching.domain.book.event

import io.vavr.collection.Seq
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
    val entries: Seq<BookEntry>,
    val whenHappened: Instant
) : Event<BookId, Books> {
    init {
        val offending = entries.filter {eventId != it.key.eventId}
        if (offending.size() > 0)
            throw IllegalArgumentException("Event ID must match the Event ID in the Book Entry: eventId=$eventId, offendingEntries=$offending")
    }

    override fun aggregateId(): BookId = bookId
    override fun eventId(): EventId = eventId
    override fun isPrimary(): Boolean = false

    override fun play(aggregate: Books): Transaction<BookId, Books> =
        Transaction(aggregate.removeBookEntries(aggregate.verifyEventId(eventId),
            entries))
}
