package jasition.matching.domain.quote.command

import arrow.core.Either
import io.vavr.collection.Seq
import jasition.cqrs.Command
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.TimeInForce
import jasition.matching.domain.client.Client
import jasition.matching.domain.quote.event.MassQuotePlacedEvent
import jasition.matching.domain.quote.event.MassQuoteRejectedEvent
import java.time.Instant

data class PlaceMassQuoteCommand(
    val quoteId: String,
    val whoRequested: Client,
    val bookId: BookId,
    val quoteModelType: QuoteModelType = QuoteModelType.QUOTE_ENTRY,
    val timeInForce: TimeInForce = TimeInForce.GOOD_TILL_CANCEL,
    val entries: Seq<QuoteEntry>,
    val whenRequested: Instant
) : Command {

    fun validate(
        books: Books
    ): Either<MassQuoteRejectedEvent, MassQuotePlacedEvent> {

        return Either.right(
            MassQuotePlacedEvent(
                eventId = books.lastEventId.next(),
                bookId = books.bookId,
                quoteId = quoteId,
                whoRequested = whoRequested,
                quoteModelType = quoteModelType,
                timeInForce = timeInForce,
                entries = entries,
                whenHappened = whenRequested
            )
        )
    }
}
