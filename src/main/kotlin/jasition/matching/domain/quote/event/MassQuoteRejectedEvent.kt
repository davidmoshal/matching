package jasition.matching.domain.quote.event

import io.vavr.collection.Seq
import jasition.cqrs.Event
import jasition.cqrs.EventId
import jasition.cqrs.Transaction
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.TimeInForce
import jasition.matching.domain.client.Client
import jasition.matching.domain.quote.command.QuoteEntry
import jasition.matching.domain.quote.command.QuoteModelType
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

        return Transaction(aggregate.copy(lastEventId = aggregate.verifyEventId(eventId)))
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