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
) : Event {
    override fun eventId(): EventId = eventId
    override fun type(): EventType = EventType.SIDE_EFFECT
}

fun play(event: EntryAddedToBookEvent, books: Books): Transaction<Books> {
    TODO("to implement")
}