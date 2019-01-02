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
        val rejection =
            rejectDueToUnknownSymbol(books)
                ?: rejectDueToIncorrectSizes(books)
                ?: rejectDueToCrossedPrices(books)
                ?: rejectDueToExchangeClosed(books)

        if (rejection != null) {
            return Either.left(rejection)
        }

        return Either.right(toPlacedEvent(books))
    }

    private fun rejectDueToUnknownSymbol(books: Books): MassQuoteRejectedEvent? =
        if (bookId != books.bookId)
            toRejectedEvent(
                books = books.copy(bookId = bookId),
                quoteRejectReason = QuoteRejectReason.UNKNOWN_SYMBOL,
                quoteRejectText = "Unknown book ID : ${bookId.bookId}"
            )
        else null

    private fun rejectDueToExchangeClosed(books: Books): MassQuoteRejectedEvent? =
        if (!books.tradingStatuses.effectiveStatus().allows(this))
            toRejectedEvent(
                books = books,
                quoteRejectReason = QuoteRejectReason.EXCHANGE_CLOSED,
                quoteRejectText = "Placing mass quote is currently not allowed : ${books.tradingStatuses.effectiveStatus()}"
            )
        else null

    private fun rejectDueToCrossedPrices(books: Books): MassQuoteRejectedEvent? {
        val lowestSellPrice = lowestSellPrice()
        val highestBuyPrice = highestBuyPrice()
        return if (lowestSellPrice ?: Long.MAX_VALUE <= highestBuyPrice ?: Long.MIN_VALUE)
            toRejectedEvent(
                books = books,
                quoteRejectReason = QuoteRejectReason.INVALID_BID_ASK_SPREAD,
                quoteRejectText = "Quote prices must not cross within a mass quote: lowestSellPrice=$lowestSellPrice, highestBuyPrice=$highestBuyPrice"
            )
        else null
    }

    private fun rejectDueToIncorrectSizes(books: Books): MassQuoteRejectedEvent? =
        entries.map { findNonPositiveSize(it) }
            .filterNotNull()
            .min()?.let {
                toRejectedEvent(
                    books = books,
                    quoteRejectReason = QuoteRejectReason.INVALID_QUANTITY,
                    quoteRejectText = "Quote sizes must be positive : $it"
                )
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


    private fun findNonPositiveSize(it: QuoteEntry): Int? =
        it.bid?.let { b -> if (b.size <= 0) b.size else null }
            ?: it.offer?.let { o -> if (o.size <= 0) o.size else null }


    private fun toRejectedEvent(
        books: Books,
        quoteRejectReason: QuoteRejectReason,
        quoteRejectText: String
    ): MassQuoteRejectedEvent = MassQuoteRejectedEvent(
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

    private fun toPlacedEvent(books: Books): MassQuotePlacedEvent = MassQuotePlacedEvent(
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
