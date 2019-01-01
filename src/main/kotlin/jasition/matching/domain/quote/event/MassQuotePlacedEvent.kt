package jasition.matching.domain.quote.event

import io.vavr.collection.List
import io.vavr.collection.Seq
import jasition.cqrs.Event
import jasition.cqrs.EventId
import jasition.cqrs.Transaction
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.book.event.EntriesRemovedFromBookEvent
import jasition.matching.domain.client.Client
import jasition.matching.domain.quote.command.QuoteEntry
import jasition.matching.domain.quote.command.QuoteModelType
import jasition.matching.domain.trade.matchAndPlaceEntries
import java.time.Instant
import java.util.function.Predicate

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
    override fun isPrimary(): Boolean = true

    override fun play(aggregate: Books): Transaction<BookId, Books> {
        val newTransaction = cancelQuotesIfRequired(aggregate.copy(lastEventId = aggregate.verifyEventId(eventId)))

        return matchAndPlaceEntries(
            bookEntries = toBookEntries(),
            books = newTransaction.aggregate,
            transaction = newTransaction
        )
    }

    private fun cancelQuotesIfRequired(aggregate: Books): Transaction<BookId, Books> {
        if (quoteModelType.shouldCancelPreviousQuotes()) {

            val toBeRemoved = aggregate.findBookEntries(Predicate { p -> p.whoRequested == whoRequested })

            if (toBeRemoved.isEmpty) {
                return Transaction(aggregate)
            }

            val entriesRemovedToBookEvent = EntriesRemovedFromBookEvent(
                eventId = eventId.next(),
                entries = toBeRemoved,
                bookId = bookId,
                whenHappened = whenHappened
            )

            return Transaction(aggregate)
                .append(entriesRemovedToBookEvent)
                .append(entriesRemovedToBookEvent.play(aggregate))
        }
        return Transaction(aggregate)
    }

    fun toBookEntries(
        offset: Int = 0,
        bookEntries: Seq<BookEntry> = List.empty()
    ): Seq<BookEntry> {
        if (offset >= entries.size()) {
            return bookEntries
        }

        val quoteEntry = entries.get(offset)
        var updatedEntries = bookEntries
        quoteEntry.bid?.let {
            updatedEntries = updatedEntries.append(
                toBookEntry(
                    quoteEntry = quoteEntry,
                    side = Side.BUY,
                    size = it.size,
                    price = it.price
                )
            )
        }
        quoteEntry.offer?.let {
            updatedEntries = updatedEntries.append(
                toBookEntry(
                    quoteEntry = quoteEntry,
                    side = Side.SELL,
                    size = it.size,
                    price = it.price
                )
            )
        }
        return toBookEntries(
            offset = offset + 1,
            bookEntries = updatedEntries
        )
    }

    fun toBookEntry(
        quoteEntry: QuoteEntry,
        side: Side,
        size: Int,
        price: Price?
    ): BookEntry {
        return BookEntry(
            whenSubmitted = whenHappened,
            eventId = eventId,
            requestId = quoteEntry.toClientRequestId(quoteId = quoteId),
            whoRequested = whoRequested,
            isQuote = true,
            entryType = quoteEntry.entryType,
            side = side,
            sizes = EntrySizes(size),
            price = price,
            timeInForce = timeInForce,
            status = EntryStatus.NEW
        )
    }
}