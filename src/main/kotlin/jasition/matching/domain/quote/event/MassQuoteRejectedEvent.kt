package jasition.matching.domain.quote.event

import io.vavr.collection.Seq
import jasition.cqrs.Event
import jasition.cqrs.EventId
import jasition.cqrs.Transaction
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.client.Client
import jasition.matching.domain.quote.command.QuoteEntry
import java.time.Instant

data class MassQuoteRejectedEvent(
    val eventId: EventId,
    val quoteId: String,
    val whoRequested: Client,
    val bookId: BookId,
    val entries : Seq<QuoteEntry>,
    val whenHappened: Instant,
    val quoteRejectReason : QuoteRejectReason
) : Event<BookId, Books> {
    override fun aggregateId(): BookId = bookId
    override fun eventId(): EventId = eventId
    override fun isPrimary(): Boolean = true

    override fun play(aggregate: Books): Transaction<BookId, Books> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}

enum class QuoteRejectReason {
    UNKNOWN_SYMBOL,
    EXCHANGE_CLOSED,
    DUPLICATE_QUOTE,
    INVALID_BID_ASK_SPREAD,
    INVALID_PRICE,
    NOT_AUTHORISED,
    OTHER
}