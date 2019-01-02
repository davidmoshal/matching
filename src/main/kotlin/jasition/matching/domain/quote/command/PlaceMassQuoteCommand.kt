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
import jasition.matching.domain.quote.event.QuoteRejectReason
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
        val invalidSizeEntries = entries.filter { hasNonPositiveSize(it) }
        if (invalidSizeEntries.size() > 0) {
            return Either.left(
                toRejectedEvent(
                    books = books,
                    quoteRejectReason = QuoteRejectReason.EXCHANGE_CLOSED,
                    quoteRejectText = "Quote sizes must be positive : ${invalidSizeEntries.toJavaArray()}"
                )
            )
        }

        val lowestSellPrice = lowestSellPrice()
        val highestBuyPrice = highestBuyPrice()
        if (lowestSellPrice ?: Long.MAX_VALUE  <= highestBuyPrice ?: Long.MIN_VALUE) {
            return Either.left(
                toRejectedEvent(
                    books = books,
                    quoteRejectReason = QuoteRejectReason.INVALID_BID_ASK_SPREAD,
                    quoteRejectText = "Quote prices must not cross within a mass quote: lowestSellPrice=$lowestSellPrice, highestBuyPrice=$highestBuyPrice"
                )
            )
        }

        if (!books.tradingStatuses.effectiveStatus().allows(this)) {
            return Either.left(
                toRejectedEvent(
                    books = books,
                    quoteRejectReason = QuoteRejectReason.EXCHANGE_CLOSED,
                    quoteRejectText = "Placing mass quote is currently not allowed : ${books.tradingStatuses.effectiveStatus()}"
                )
            )
        }

        return Either.right(toPlacedEvent(books))
    }

    private fun lowestSellPrice(): Long? =
        entries.filter { it.offer?.price != null }
            .map { it.offer?.price?.value }
            .min()
            .orNull

    private fun highestBuyPrice(): Long? =
        entries.filter { it.bid?.price != null }
            .map { it.bid?.price?.value }
            .max()
            .orNull


    private fun hasNonPositiveSize(it: QuoteEntry): Boolean =
        (it.bid?.let { b -> b.size <= 0 } ?: false)
                || it.offer?.let { o -> o.size <= 0 } ?: false


    private fun toRejectedEvent(
        books: Books,
        quoteRejectReason: QuoteRejectReason,
        quoteRejectText: String
    ): MassQuoteRejectedEvent {
        return MassQuoteRejectedEvent(
            eventId = books.lastEventId.next(),
            bookId = books.bookId,
            quoteId = quoteId,
            whoRequested = whoRequested,
            quoteModelType = quoteModelType,
            timeInForce = timeInForce,
            entries = entries,
            whenHappened = whenRequested,
            quoteRejectReason = quoteRejectReason,
            quoteRejectText = quoteRejectText
        )
    }

    private fun toPlacedEvent(books: Books): MassQuotePlacedEvent {
        return MassQuotePlacedEvent(
            eventId = books.lastEventId.next(),
            bookId = books.bookId,
            quoteId = quoteId,
            whoRequested = whoRequested,
            quoteModelType = quoteModelType,
            timeInForce = timeInForce,
            entries = entries,
            whenHappened = whenRequested
        )
    }
}
