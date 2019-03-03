package jasition.matching.domain.quote.command

import arrow.core.Either
import io.vavr.collection.List
import io.vavr.collection.Seq
import jasition.cqrs.Command
import jasition.cqrs.Event
import jasition.cqrs.Transaction
import jasition.cqrs.append
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.BooksNotFoundException
import jasition.matching.domain.book.entry.PriceWithSize
import jasition.matching.domain.book.entry.TimeInForce
import jasition.matching.domain.client.Client
import jasition.matching.domain.quote.QuoteEntry
import jasition.matching.domain.quote.QuoteModelType
import jasition.matching.domain.quote.cancelExistingQuotes
import jasition.matching.domain.quote.event.MassQuotePlacedEvent
import jasition.matching.domain.quote.event.MassQuoteRejectedEvent
import jasition.matching.domain.quote.event.QuoteRejectReason
import jasition.matching.domain.trade.matchAndFinalise
import java.time.Instant

data class PlaceMassQuoteCommand(
    val quoteId: String,
    val whoRequested: Client,
    val bookId: BookId,
    val quoteModelType: QuoteModelType = QuoteModelType.QUOTE_ENTRY,
    val timeInForce: TimeInForce = TimeInForce.GOOD_TILL_CANCEL,
    val entries: Seq<QuoteEntry>,
    val whenRequested: Instant
) : Command<BookId, Books> {

    override fun execute(aggregate: Books?): Either<Exception, Transaction<BookId, Books>> {
        if (aggregate == null) return Either.left(BooksNotFoundException("Books $bookId not found"))

        val cancelledEvent = cancelExistingQuotes(
            books = aggregate,
            eventId = aggregate.lastEventId,
            whoRequested = whoRequested,
            whenHappened = whenRequested
        )

        val events = cancelledEvent?.let {
            List.of<Event<BookId, Books>>(it)
        } ?: List.empty()

        val cancelledBooks = cancelledEvent?.play(aggregate) ?: aggregate

        val rejection = rejectDueToUnknownSymbol(cancelledBooks)
            ?: rejectDueToIncorrectSizes(cancelledBooks)
            ?: rejectDueToCrossedPrices(cancelledBooks)
            ?: rejectDueToExchangeClosed(cancelledBooks)

        rejection?.run {
            return Either.right(Transaction(play(cancelledBooks), events.append(this)))
        }

        val placedEvent = toPlacedEvent(cancelledBooks)
        val placedBooks = placedEvent.play(cancelledBooks)

        val initial = Transaction(placedBooks, events.append(placedEvent))

        return Either.right(placedEvent.toBookEntries().fold(initial) { txn, entry ->
            txn append matchAndFinalise(entry, txn.aggregate)
        })
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
        entries.map { it.offer }
            .filterNotNull()
            .map { it.price.value }
            .min()

    private fun highestBuyPrice(): Long? =
        entries.map { it.bid }
            .filterNotNull()
            .map { it.price.value }
            .max()

    private fun findNonPositiveSize(quoteEntry: QuoteEntry): Int? =
        findNonPositiveSize(quoteEntry.bid) ?: findNonPositiveSize(quoteEntry.offer)

    private fun findNonPositiveSize(priceWithSize: PriceWithSize?): Int? =
        priceWithSize?.let { if (it.size <= 0) it.size else null }

    private fun toRejectedEvent(
        books: Books,
        quoteRejectReason: QuoteRejectReason,
        quoteRejectText: String
    ): MassQuoteRejectedEvent = MassQuoteRejectedEvent(
        eventId = books.lastEventId.inc(),
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
        eventId = books.lastEventId.inc(),
        bookId = books.bookId,
        quoteId = quoteId,
        whoRequested = whoRequested,
        quoteModelType = quoteModelType,
        timeInForce = timeInForce,
        entries = entries,
        whenHappened = whenRequested
    )
}
