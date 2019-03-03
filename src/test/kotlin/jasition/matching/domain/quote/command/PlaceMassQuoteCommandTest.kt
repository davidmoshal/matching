package jasition.matching.domain.quote.command

import arrow.core.Either
import io.kotlintest.data.forall
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.kotlintest.tables.row
import io.vavr.collection.List
import jasition.cqrs.EventId
import jasition.cqrs.Transaction
import jasition.matching.domain.*
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.TradingStatus
import jasition.matching.domain.book.TradingStatuses
import jasition.matching.domain.book.entry.PriceWithSize
import jasition.matching.domain.book.entry.Side.BUY
import jasition.matching.domain.book.entry.Side.SELL
import jasition.matching.domain.book.entry.TimeInForce
import jasition.matching.domain.book.event.EntryAddedToBookEvent
import jasition.matching.domain.quote.QuoteModelType
import jasition.matching.domain.quote.event.MassQuotePlacedEvent
import jasition.matching.domain.quote.event.MassQuoteRejectedEvent
import jasition.matching.domain.quote.event.QuoteRejectReason
import java.time.Instant

internal class PlaceMassQuoteCommandTest : StringSpec({
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

    val expectedBuyEntries = List.of(
        expectedBookEntry(command = command, eventId = EventId(1), side = BUY, entry = command.entries[0]),
        expectedBookEntry(command = command, eventId = EventId(1), side = BUY, entry = command.entries[1])
    )

    val expectedSellEntries = List.of(
        expectedBookEntry(command = command, eventId = EventId(1), side = SELL, entry = command.entries[0]),
        expectedBookEntry(command = command, eventId = EventId(1), side = SELL, entry = command.entries[1])
    )

    "When the request is valid, the mass quote is placed" {
        command.execute(books) shouldBe Either.right(
            Transaction<BookId, Books>(
                aggregate = books.copy(
                    buyLimitBook = books.buyLimitBook.add(expectedBuyEntries[0]).add(expectedBuyEntries[1]),
                    sellLimitBook = books.sellLimitBook.add(expectedSellEntries[0]).add(expectedSellEntries[1]),
                    lastEventId = EventId(5)
                ),
                events = List.of(
                    MassQuotePlacedEvent(
                        eventId = books.lastEventId.inc(),
                        quoteId = command.quoteId,
                        whoRequested = command.whoRequested,
                        bookId = bookId,
                        quoteModelType = command.quoteModelType,
                        timeInForce = command.timeInForce,
                        entries = command.entries,
                        whenHappened = command.whenRequested
                    ),
                    EntryAddedToBookEvent(bookId = bookId, eventId = EventId(2), entry = expectedBuyEntries[0]),
                    EntryAddedToBookEvent(bookId = bookId, eventId = EventId(3), entry = expectedSellEntries[0]),
                    EntryAddedToBookEvent(bookId = bookId, eventId = EventId(4), entry = expectedBuyEntries[1]),
                    EntryAddedToBookEvent(bookId = bookId, eventId = EventId(5), entry = expectedSellEntries[1])
                )
            )
        )
    }
    "Single-sided quoting on the BID side is allowed" {
        val quoteEntry = aQuoteEntry(offerSize = null, bidPrice = 10)
        val newCommand = command.copy(entries = List.of(quoteEntry))
        val expectedEntry =
            expectedBookEntry(command = newCommand, eventId = EventId(1), side = BUY, entry = quoteEntry)

        newCommand.execute(books) shouldBe Either.right(
            Transaction<BookId, Books>(
                aggregate = books.copy(
                    buyLimitBook = books.buyLimitBook.add(expectedEntry),
                    sellLimitBook = books.sellLimitBook,
                    lastEventId = EventId(2)
                ),
                events = List.of(
                    MassQuotePlacedEvent(
                        eventId = books.lastEventId.inc(),
                        quoteId = command.quoteId,
                        whoRequested = command.whoRequested,
                        bookId = bookId,
                        quoteModelType = command.quoteModelType,
                        timeInForce = command.timeInForce,
                        entries = List.of(quoteEntry),
                        whenHappened = command.whenRequested
                    ),
                    EntryAddedToBookEvent(
                        bookId = bookId,
                        eventId = EventId(2),
                        entry = expectedEntry
                    )
                )
            )
        )
    }
    "Single-sided quoting on the OFFER side is allowed" {
        val quoteEntry = aQuoteEntry(bidSize = null, offerPrice = 10)
        val newCommand = command.copy(
            entries = List.of(quoteEntry)
        )
        val expectedEntry =
            expectedBookEntry(command = newCommand, eventId = EventId(1), side = SELL, entry = quoteEntry)

        newCommand.execute(books) shouldBe Either.right(
            Transaction<BookId, Books>(
                aggregate = books.copy(
                    buyLimitBook = books.buyLimitBook,
                    sellLimitBook = books.sellLimitBook.add(expectedEntry),
                    lastEventId = EventId(2)
                ),
                events = List.of(
                    MassQuotePlacedEvent(
                        eventId = books.lastEventId.inc(),
                        quoteId = command.quoteId,
                        whoRequested = command.whoRequested,
                        bookId = bookId,
                        quoteModelType = command.quoteModelType,
                        timeInForce = command.timeInForce,
                        entries = List.of(quoteEntry),
                        whenHappened = command.whenRequested
                    ),
                    EntryAddedToBookEvent(
                        bookId = bookId,
                        eventId = EventId(2),
                        entry = expectedEntry
                    )
                )
            )
        )
    }
    "When the wrong book ID is used, then the mass quote is rejected" {
        val wrongBookId = "Wrong ID"
        command.copy(bookId = BookId(wrongBookId)).execute(books) shouldBe Either.right(
            Transaction<BookId, Books>(
                aggregate = books.copy(
                    buyLimitBook = books.buyLimitBook,
                    sellLimitBook = books.sellLimitBook,
                    lastEventId = EventId(1)
                ),
                events = List.of(
                    MassQuoteRejectedEvent(
                        eventId = books.lastEventId.inc(),
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
            ).execute(books) shouldBe Either.right(
                Transaction<BookId, Books>(
                    aggregate = books.copy(
                        buyLimitBook = books.buyLimitBook,
                        sellLimitBook = books.sellLimitBook,
                        lastEventId = EventId(1)
                    ),
                    events = List.of(
                        MassQuoteRejectedEvent(
                            eventId = books.lastEventId.inc(),
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
            ).execute(books) shouldBe Either.right(
                Transaction<BookId, Books>(
                    aggregate = books.copy(
                        buyLimitBook = books.buyLimitBook,
                        sellLimitBook = books.sellLimitBook,
                        lastEventId = EventId(1)
                    ),
                    events = List.of(
                        MassQuoteRejectedEvent(
                            eventId = books.lastEventId.inc(),
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
                )
            )
        }
    }

    "When the effective trading status disallows placing order, then the order is rejected" {
        val tradingStatuses = TradingStatuses(TradingStatus.NOT_AVAILABLE_FOR_TRADING)
        command.execute(books.copy(tradingStatuses = tradingStatuses)) shouldBe Either.right(
            Transaction<BookId, Books>(
                aggregate = books.copy(
                    buyLimitBook = books.buyLimitBook,
                    sellLimitBook = books.sellLimitBook,
                    lastEventId = EventId(1),
                    tradingStatuses = tradingStatuses
                ),
                events = List.of(
                    MassQuoteRejectedEvent(
                        eventId = books.lastEventId.inc(),
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
            )
        )

    }
})