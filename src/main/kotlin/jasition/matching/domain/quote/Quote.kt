package jasition.matching.domain.quote

import io.vavr.collection.List
import io.vavr.collection.Seq
import jasition.cqrs.EventId
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

    fun toBookEntry(
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
        entryType = EntryType.LIMIT,
        side = side,
        sizes = EntrySizes(size),
        price = price,
        timeInForce = timeInForce,
        status = EntryStatus.NEW
    )
}

enum class QuoteModelType {
    QUOTE_ENTRY
}

fun cancelExistingQuotes(
    books: Books,
    eventId: EventId,
    whoRequested: Client,
    whenHappened: Instant
): MassQuoteCancelledEvent? {
    val toBeRemoved = books
        .findBookEntries(Predicate { p -> p.whoRequested == whoRequested && p.isQuote })

    if (toBeRemoved.isEmpty) return null

    return MassQuoteCancelledEvent(
        eventId = eventId.inc(),
        entries = toBeRemoved.map { it.cancelled() },
        // TODO: revise for newer Jacoco version - Below is equivalence to above but Jacoco cannot reach 100% coverage with the function reference
//        entries = toBeRemoved.map(BookEntry::cancelled),
        bookId = books.bookId,
        whoRequested = whoRequested,
        whenHappened = whenHappened
    )
}
