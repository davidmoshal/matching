package jasition.matching.domain.quote.event

import io.vavr.collection.Seq
import jasition.cqrs.Event
import jasition.cqrs.EventId
import jasition.cqrs.Transaction
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.TimeInForce
import jasition.matching.domain.client.Client
import jasition.matching.domain.quote.QuoteEntry
import jasition.matching.domain.quote.QuoteModelType
import jasition.matching.domain.quote.cancelExistingQuotes
import java.time.Instant

data class MassQuoteRejectedEvent(
    val eventId: EventId,
    val quoteId: String,
    val whoRequested: Client,
    val bookId: BookId,
    val quoteModelType: QuoteModelType,
    val timeInForce: TimeInForce,
    val entries: Seq<QuoteEntry>,
    val whenHappened: Instant,
    val quoteRejectReason: QuoteRejectReason,
    val quoteRejectText: String
) : Event<BookId, Books> {
    override fun aggregateId(): BookId = bookId
    override fun eventId(): EventId = eventId
    override fun isPrimary(): Boolean = true

    override fun play(aggregate: Books): Transaction<BookId, Books> {
        return cancelExistingQuotes(
            books = aggregate.copy(lastEventId = aggregate.verifyEventId(eventId)),
            eventId = eventId,
            whoRequested = whoRequested,
            whenHappened = whenHappened,
            primary = false
        )
    }
}

enum class QuoteRejectReason {
    UNKNOWN_SYMBOL,
    EXCHANGE_CLOSED,
    DUPLICATE_QUOTE,
    INVALID_BID_ASK_SPREAD,
    INVALID_PRICE,
    INVALID_QUANTITY,
    NOT_AUTHORISED,
    OTHER
}