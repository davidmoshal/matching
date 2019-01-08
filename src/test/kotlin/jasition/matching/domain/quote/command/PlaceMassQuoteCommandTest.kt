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
import jasition.matching.domain.book.entry.PriceWithSize
import jasition.matching.domain.book.entry.TimeInForce
import jasition.matching.domain.quote.QuoteModelType
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
            ),
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
    "Single-sided quoting on the BID side is allowed" {
        val newEntries = List.of(aQuoteEntry(offerSize = null, bidPrice = 10))
        command.copy(
            entries = newEntries
        ).validate(books) shouldBe Either.right(
            MassQuotePlacedEvent(
                eventId = books.lastEventId.next(),
                quoteId = command.quoteId,
                whoRequested = command.whoRequested,
                bookId = bookId,
                quoteModelType = command.quoteModelType,
                timeInForce = command.timeInForce,
                entries = newEntries,
                whenHappened = command.whenRequested
            )
        )
    }
    "Single-sided quoting on the OFFER side is allowed" {
        val newEntries = List.of(aQuoteEntry(bidSize = null, offerPrice = 10))
        command.copy(
            entries = newEntries
        ).validate(books) shouldBe Either.right(
            MassQuotePlacedEvent(
                eventId = books.lastEventId.next(),
                quoteId = command.quoteId,
                whoRequested = command.whoRequested,
                bookId = bookId,
                quoteModelType = command.quoteModelType,
                timeInForce = command.timeInForce,
                entries = newEntries,
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
        row(0, 2),
        row(2, 0),
        row(randomSize(from = -10, until = -1), 4),
        row(7, randomSize(from = -20, until = -11)),
        row(-15, -15)
    ) { bidSize, offerSize ->


        val min = minOf(bidSize, offerSize)
        "When the request has non-positive sizes (bid=$bidSize, offer=$offerSize), the mass quote is rejected" {
            val newEntries = List.of(
                aQuoteEntry(
                    bid = PriceWithSize(randomPrice(from = 10, until = 12), bidSize),
                    offer = PriceWithSize(randomPrice(from = 13, until = 15), offerSize)
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
                    quoteRejectText = "Quote sizes must be positive : $min"
                )
            )
        }
    }
    forall(
        row(List.of(aQuoteEntry(bidPrice = 14, offerPrice = 14)), 14, 14),
        row(
            List.of(aQuoteEntry(bidSize = null, offerPrice = 12), aQuoteEntry(bidPrice = 13, offerSize = null)),
            12,
            13
        ),
        row(List.of(aQuoteEntry(bidPrice = 14, offerPrice = 12), aQuoteEntry(bidPrice = 13, offerPrice = 13)), 12, 14)
    ) { newEntries, minSell, maxBuy ->
        "When the request has crossed prices (minSell=$minSell, maxBuy=$maxBuy), the mass quote is rejected" {
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
                    quoteRejectText = "Quote prices must not cross within a mass quote: lowestSellPrice=$minSell, highestBuyPrice=$maxBuy"
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