package jasition.matching.domain.book.event

import jasition.matching.domain.Event
import jasition.matching.domain.EventId
import jasition.matching.domain.EventType
import jasition.matching.domain.Transaction
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.BookEntry
import java.time.Instant

data class EntryAddedToBookEvent(
    val eventId: EventId,
    val bookId: BookId,
    val entry: BookEntry,
    val whenHappened: Instant
) : Event<Books> {
    init {
        if (eventId != entry.key.eventId) throw IllegalArgumentException("Event ID must match the Event ID in the Book Entry: eventId=$eventId, entry.eventId=${entry.key.eventId}")
    }

    override fun eventId(): EventId = eventId
    override fun type(): EventType = EventType.SIDE_EFFECT

    override fun play(aggregate: Books): Transaction<Books> {
        aggregate.verifyEventId(eventId)
        aggregate.verifyEventId(entry.key.eventId)

        return Transaction(aggregate.addBookEntry(entry))
    }
}
