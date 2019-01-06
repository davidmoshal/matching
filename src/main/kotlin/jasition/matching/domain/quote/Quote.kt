package jasition.matching.domain.quote

import io.vavr.collection.List
import io.vavr.collection.Seq
import jasition.cqrs.EventId
import jasition.cqrs.Transaction
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.client.Client
import jasition.matching.domain.client.ClientRequestId
import jasition.matching.domain.quote.event.MassQuoteCancelledEvent
import java.time.Instant
import java.util.function.Predicate

data class QuoteEntry(
    val quoteEntryId: String,
    val quoteSetId: String,
    val entryType: EntryType,
    val bid: PriceWithSize?,
    val offer: PriceWithSize?
) {

    fun toClientRequestId(quoteId: String): ClientRequestId = ClientRequestId(
        current = quoteEntryId, collectionId = quoteSetId, parentId = quoteId
    )

    fun toBookEntries(
        whenHappened: Instant,
        eventId: EventId,
        quoteId: String,
        whoRequested: Client,
        timeInForce: TimeInForce
    ): Seq<BookEntry> {
        var entries = List.empty<BookEntry>()
        bid?.let {
            entries = entries.append(
                toBookEntry(
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
        offer?.let {
            entries = entries.append(
                toBookEntry(
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
        return entries
    }

    private fun toBookEntry(
        side: Side,
        size: Int,
        price: Price?,
        whenHappened: Instant,
        eventId: EventId,
        quoteId: String,
        whoRequested: Client,
        timeInForce: TimeInForce
    ): BookEntry = BookEntry(
        whenSubmitted = whenHappened,
        eventId = eventId,
        requestId = toClientRequestId(quoteId = quoteId),
        whoRequested = whoRequested,
        isQuote = true,
        entryType = entryType,
        side = side,
        sizes = EntrySizes(size),
        price = price,
        timeInForce = timeInForce,
        status = EntryStatus.NEW
    )
}

enum class QuoteModelType {
    QUOTE_ENTRY {
        override fun shouldCancelPreviousQuotes(): Boolean = true
    };

    abstract fun shouldCancelPreviousQuotes(): Boolean
}

fun cancelExistingQuotes(
    books: Books,
    eventId: EventId,
    whoRequested: Client,
    whenHappened: Instant,
    primary: Boolean
): Transaction<BookId, Books> {
    val toBeRemoved = books.findBookEntries(Predicate { p -> p.whoRequested == whoRequested && p.isQuote})

    if (toBeRemoved.isEmpty) {
        return Transaction(books)
    }

    val massQuoteCancelledEvent = MassQuoteCancelledEvent(
        eventId = eventId.next(),
        entries = toBeRemoved.map(BookEntry::cancelled),
        bookId = books.bookId,
        primary = primary,
        whoRequested = whoRequested,
        whenHappened = whenHappened
    )

    return massQuoteCancelledEvent.playAndAppend(books)
}