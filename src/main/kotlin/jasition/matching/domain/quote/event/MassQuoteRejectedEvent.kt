package jasition.matching.domain.quote.event

import io.vavr.collection.Seq
import jasition.cqrs.Event
import jasition.cqrs.EventId
import jasition.cqrs.Transaction
import jasition.cqrs.playAndAppend
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

    //TODO unit test
    override fun play_2_(aggregate: Books): Books {
        val books = aggregate.copy(lastEventId = aggregate.verifyEventId(eventId))

        return cancelExistingQuotes(
            books = books,
            eventId = eventId,
            whoRequested = whoRequested,
            whenHappened = whenHappened,
            primary = false
        )?.play_2_(books) ?: books
    }

    override fun play(aggregate: Books): Transaction<BookId, Books> {
        val books = aggregate.copy(lastEventId = aggregate.verifyEventId(eventId))
        val event = cancelExistingQuotes(
            books = books,
            eventId = eventId,
            whoRequested = whoRequested,
            whenHappened = whenHappened,
            primary = false
        )

        return if (event != null) event playAndAppend books else Transaction(books)

// TODO: revise for newer Jacoco version - Below is equivalence to above but Jacoco cannot reach 100% coverage with the let function
//        return event?.playAndAppend(books) ?: Transaction(books)
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