package jasition.matching.domain.quote.command

import io.vavr.collection.List
import io.vavr.collection.Seq
import jasition.cqrs.EventId
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.client.Client
import jasition.matching.domain.client.ClientRequestId
import java.time.Instant

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