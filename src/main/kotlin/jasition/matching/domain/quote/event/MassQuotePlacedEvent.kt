package jasition.matching.domain.quote.event

import io.vavr.collection.List
import io.vavr.collection.Seq
import jasition.cqrs.Event
import jasition.cqrs.EventId
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.BookEntry
import jasition.matching.domain.book.entry.Side
import jasition.matching.domain.book.entry.TimeInForce
import jasition.matching.domain.client.Client
import jasition.matching.domain.quote.QuoteEntry
import jasition.matching.domain.quote.QuoteModelType
import java.time.Instant

data class MassQuotePlacedEvent(
    val eventId: EventId,
    val quoteId: String,
    val whoRequested: Client,
    val bookId: BookId,
    val quoteModelType: QuoteModelType,
    val timeInForce: TimeInForce,
    val entries: Seq<QuoteEntry>,
    val whenHappened: Instant
) : Event<BookId, Books> {
    override fun aggregateId(): BookId = bookId
    override fun eventId(): EventId = eventId
    override fun play(aggregate: Books): Books = aggregate.ofEventId(eventId)

    fun toBookEntries(
        entryOffset: Int = 0,
        bookEntries: Seq<BookEntry> = List.empty()
    ): Seq<BookEntry> =
        if (entryOffset >= entries.size())
            bookEntries
        else {
            val quoteEntry = entries.get(entryOffset)
            var newBookEntries = List.empty<BookEntry>()

            quoteEntry.bid?.let {
                newBookEntries = newBookEntries.append(
                    quoteEntry.toBookEntry(
                        side = Side.BUY,
                        size = it.size,
                        price = it.price,
                        whenHappened = whenHappened,
                        eventId = eventId,
                        quoteId = quoteId,
                        whoRequested = whoRequested,
                        timeInForce = timeInForce
                    )
                )
            }
            quoteEntry.offer?.let {
                newBookEntries = newBookEntries.append(
                    quoteEntry.toBookEntry(
                        side = Side.SELL,
                        size = it.size,
                        price = it.price,
                        whenHappened = whenHappened,
                        eventId = eventId,
                        quoteId = quoteId,
                        whoRequested = whoRequested,
                        timeInForce = timeInForce
                    )
                )
            }
            toBookEntries(
                entryOffset = entryOffset + 1,
                bookEntries = bookEntries.appendAll(
                    newBookEntries
                )
            )
        }
}