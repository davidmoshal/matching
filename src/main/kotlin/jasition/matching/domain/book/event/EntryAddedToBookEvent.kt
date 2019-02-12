package jasition.matching.domain.book.event

import jasition.cqrs.Event
import jasition.cqrs.EventId
import jasition.cqrs.Transaction
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.BookEntry

data class EntryAddedToBookEvent(val bookId: BookId, val eventId: EventId, val entry: BookEntry) : Event<BookId, Books> {
    override fun aggregateId(): BookId = bookId

    override fun eventId(): EventId = eventId

    override fun isPrimary(): Boolean = false

    override fun play_2_(aggregate: Books): Books = aggregate.addBookEntry(entry)

    override fun play(aggregate: Books): Transaction<BookId, Books> =
        Transaction(aggregate = aggregate.addBookEntry(entry))

}