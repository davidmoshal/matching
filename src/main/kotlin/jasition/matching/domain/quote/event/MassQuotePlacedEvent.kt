package jasition.matching.domain.quote.event

import io.vavr.collection.List
import io.vavr.collection.Seq
import jasition.cqrs.Event
import jasition.cqrs.EventId
import jasition.cqrs.Transaction
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.BookEntry
import jasition.matching.domain.book.entry.TimeInForce
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
                entries = toBeRemoved.map(BookEntry::cancelled),
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
    ): Seq<BookEntry> =
        if (offset >= entries.size())
            bookEntries
        else toBookEntries(
            offset = offset + 1,
            bookEntries = bookEntries.appendAll(
                entries.get(offset).toBookEntries(
                    whenHappened = whenHappened,
                    eventId = eventId,
                    whoRequested = whoRequested,
                    timeInForce = timeInForce,
                    quoteId = quoteId
                )
            )
        )
}