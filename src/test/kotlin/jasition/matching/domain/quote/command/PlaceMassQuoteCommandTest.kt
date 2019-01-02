package jasition.matching.domain.quote.command

import arrow.core.Either
import io.kotlintest.data.forall
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.kotlintest.tables.row
import io.vavr.collection.List
import jasition.matching.domain.*
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.TradingStatus
import jasition.matching.domain.book.TradingStatuses
import jasition.matching.domain.book.entry.Price
import jasition.matching.domain.book.entry.PriceWithSize
import jasition.matching.domain.book.entry.TimeInForce
import jasition.matching.domain.quote.event.MassQuotePlacedEvent
import jasition.matching.domain.quote.event.MassQuoteRejectedEvent
import jasition.matching.domain.quote.event.QuoteRejectReason
import java.time.Instant

internal class `Given there is a request to place a mass quote` : StringSpec({
    val bookId = aBookId()
    val books = aBooks(bookId)
    val command = PlaceMassQuoteCommand(
        quoteId = "quote1",
        whoRequested = aFirmWithoutClient(),
        bookId = bookId,
        quoteModelType = QuoteModelType.QUOTE_ENTRY,
        timeInForce = TimeInForce.GOOD_TILL_CANCEL,
        entries = List.of(
            aQuoteEntry(
                bid = PriceWithSize(randomPrice(from = 10, until = 12), randomSize()),
                offer = PriceWithSize(randomPrice(from = 13, until = 15), randomSize())
            )
        ), whenRequested = Instant.now()
    )
    "When the request is valid, the mass quote is placed" {
        command.validate(books) shouldBe Either.right(
            MassQuotePlacedEvent(
                eventId = books.lastEventId.next(),
                quoteId = command.quoteId,
                whoRequested = command.whoRequested,
                bookId = bookId,
                quoteModelType = command.quoteModelType,
                timeInForce = command.timeInForce,
                entries = command.entries,
                whenHappened = command.whenRequested
            )
        )
    }
    "When the wrong book ID is used, then the mass quote is rejected" {
        val wrongBookId = "Wrong ID"
        command.copy(bookId = BookId(wrongBookId)).validate(books) shouldBe Either.left(
            MassQuoteRejectedEvent(
                eventId = books.lastEventId.next(),
                quoteId = command.quoteId,
                whoRequested = command.whoRequested,
                bookId = BookId(wrongBookId),
                quoteModelType = command.quoteModelType,
                timeInForce = command.timeInForce,
                entries = command.entries,
                whenHappened = command.whenRequested,
                quoteRejectReason = QuoteRejectReason.UNKNOWN_SYMBOL,
                quoteRejectText = "Unknown book ID : $wrongBookId"
            )
        )
    }
    forall(
        row(0),
        row(randomSize(from = -20, until = -1)),
        row(randomSize(from = -20, until = -1))
    ) { negativeSize ->

        "When the request has non-positive sizes $negativeSize, the mass quote is rejected" {
            val newEntries = List.of(
                aQuoteEntry(
                    bid = PriceWithSize(randomPrice(from = 10, until = 12), negativeSize),
                    offer = PriceWithSize(randomPrice(from = 13, until = 15), randomSize())
                )
            )
            command.copy(
                entries = newEntries
            ).validate(books) shouldBe Either.left(
                MassQuoteRejectedEvent(
                    eventId = books.lastEventId.next(),
                    quoteId = command.quoteId,
                    whoRequested = command.whoRequested,
                    bookId = bookId,
                    quoteModelType = command.quoteModelType,
                    timeInForce = command.timeInForce,
                    entries = newEntries,
                    whenHappened = command.whenRequested,
                    quoteRejectReason = QuoteRejectReason.INVALID_QUANTITY,
                    quoteRejectText = "Quote sizes must be positive : $negativeSize"
                )
            )
        }
    }
    forall(
        row(Price(14), Price(14)),
        row(randomPrice(from = 10, until = 20), randomPrice(from = 1, until = 10))
    ) { maxBuy, minSell ->
        "When the request has crossed prices maxBuy=$maxBuy and minSell=$minSell, the mass quote is rejected" {
            val newEntries = List.of(
                aQuoteEntry(
                    bid = PriceWithSize(maxBuy, randomSize()),
                    offer = PriceWithSize(minSell, randomSize())
                )
            )
            command.copy(
                entries = newEntries
            ).validate(books) shouldBe Either.left(
                MassQuoteRejectedEvent(
                    eventId = books.lastEventId.next(),
                    quoteId = command.quoteId,
                    whoRequested = command.whoRequested,
                    bookId = bookId,
                    quoteModelType = command.quoteModelType,
                    timeInForce = command.timeInForce,
                    entries = newEntries,
                    whenHappened = command.whenRequested,
                    quoteRejectReason = QuoteRejectReason.INVALID_BID_ASK_SPREAD,
                    quoteRejectText = "Quote prices must not cross within a mass quote: lowestSellPrice=${minSell.value}, highestBuyPrice=${maxBuy.value}"
                )
            )
        }
    }

    "When the effective trading status disallows placing order, then the order is rejected" {
        command.validate(books.copy(tradingStatuses = TradingStatuses(TradingStatus.NOT_AVAILABLE_FOR_TRADING))) shouldBe Either.left(
            MassQuoteRejectedEvent(
                eventId = books.lastEventId.next(),
                quoteId = command.quoteId,
                whoRequested = command.whoRequested,
                bookId = bookId,
                quoteModelType = command.quoteModelType,
                timeInForce = command.timeInForce,
                entries = command.entries,
                whenHappened = command.whenRequested,
                quoteRejectReason = QuoteRejectReason.EXCHANGE_CLOSED,
                quoteRejectText = "Placing mass quote is currently not allowed : ${TradingStatus.NOT_AVAILABLE_FOR_TRADING.name}"
            )
        )

    }
})