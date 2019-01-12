package jasition.matching.domain.scenario.trading

import io.kotlintest.shouldBe
import io.kotlintest.specs.FeatureSpec
import io.vavr.collection.List
import jasition.cqrs.EventId
import jasition.matching.domain.*
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.quote.QuoteModelType
import jasition.matching.domain.quote.event.MassQuoteCancelledEvent
import jasition.matching.domain.quote.event.MassQuotePlacedEvent
import jasition.matching.domain.trade.event.TradeEvent
import java.time.Instant

internal class `Given the book has quotes of BUY 4 at 9 SELL 4 at 10 and BUY 5 at 8 SELL 5 at 11` : FeatureSpec({
    val quoteEntryModel = "[1 - Quote entry model] "
    val cannotMatchOtherMarketMaker = "[2 -  Cannot match other market maker] "

    val aggressorPartialFilledByQuotes = "[3 -  Aggressor order partial-filled by quotes] "
    val aggressorFilledByQuotes = "[4 -  Aggressor order filled by quotes] "

    val now = Instant.now()
    val bookId = aBookId()
    val originalMassQuotePlacedEvent = MassQuotePlacedEvent(
        bookId = bookId,
        eventId = EventId(1),
        whenHappened = now,
        quoteId = randomId(),
        whoRequested = anotherFirmWithoutClient(),
        quoteModelType = QuoteModelType.QUOTE_ENTRY,
        timeInForce = TimeInForce.GOOD_TILL_CANCEL,
        entries = List.of(
            aQuoteEntry(
                bid = PriceWithSize(size = 4, price = Price(9)),
                offer = PriceWithSize(size = 4, price = Price(10))
            ), aQuoteEntry(
                bid = PriceWithSize(size = 5, price = Price(8)),
                offer = PriceWithSize(size = 5, price = Price(11))
            )
        )
    )
    val books = originalMassQuotePlacedEvent.play(aBooks(bookId)).aggregate

    feature(quoteEntryModel) {
        scenario(quoteEntryModel + "When a Mass Quote with Quote Entry mode of the same firm is placed, then all existing quotes of the same firm are cancelled and all new quote entries are added") {
            val massQuotePlacedEvent = MassQuotePlacedEvent(
                bookId = bookId,
                eventId = EventId(2),
                whenHappened = now,
                quoteId = randomId(),
                whoRequested = anotherFirmWithoutClient(),
                quoteModelType = QuoteModelType.QUOTE_ENTRY,
                timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                entries = List.of(
                    aQuoteEntry(
                        bid = PriceWithSize(size = 5, price = Price(8)),
                        offer = PriceWithSize(size = 5, price = Price(9))
                    ), aQuoteEntry(
                        bid = PriceWithSize(size = 6, price = Price(7)),
                        offer = PriceWithSize(size = 6, price = Price(10))
                    )
                )
            )
            val result = massQuotePlacedEvent.play(books)

            result.events shouldBe List.of(
                MassQuoteCancelledEvent(
                    eventId = EventId(3),
                    bookId = bookId,
                    whoRequested = originalMassQuotePlacedEvent.whoRequested,
                    whenHappened = now,
                    primary = false,
                    entries = List.of(
                        expectedBookEntry(
                            event = originalMassQuotePlacedEvent,
                            quoteEntry = originalMassQuotePlacedEvent.entries.get(0),
                            side = Side.BUY,
                            sizes = EntrySizes(available = 0, traded = 0, cancelled = 4),
                            status = EntryStatus.CANCELLED
                        )
                        ,
                        expectedBookEntry(
                            event = originalMassQuotePlacedEvent,
                            quoteEntry = originalMassQuotePlacedEvent.entries.get(1),
                            side = Side.BUY,
                            sizes = EntrySizes(available = 0, traded = 0, cancelled = 5),
                            status = EntryStatus.CANCELLED
                        )
                        ,
                        expectedBookEntry(
                            event = originalMassQuotePlacedEvent,
                            quoteEntry = originalMassQuotePlacedEvent.entries.get(0),
                            side = Side.SELL,
                            sizes = EntrySizes(available = 0, traded = 0, cancelled = 4),
                            status = EntryStatus.CANCELLED
                        )
                        ,
                        expectedBookEntry(
                            event = originalMassQuotePlacedEvent,
                            quoteEntry = originalMassQuotePlacedEvent.entries.get(1),
                            side = Side.SELL,
                            sizes = EntrySizes(available = 0, traded = 0, cancelled = 5),
                            status = EntryStatus.CANCELLED
                        )
                    )
                )
            )
            result.aggregate.buyLimitBook.entries.values() shouldBe List.of(
                expectedBookEntry(
                    event = massQuotePlacedEvent,
                    quoteEntry = massQuotePlacedEvent.entries.get(0),
                    side = Side.BUY,
                    sizes = EntrySizes(5),
                    status = EntryStatus.NEW
                ), expectedBookEntry(
                    event = massQuotePlacedEvent,
                    quoteEntry = massQuotePlacedEvent.entries.get(1),
                    side = Side.BUY,
                    sizes = EntrySizes(6),
                    status = EntryStatus.NEW
                )
            )
            result.aggregate.sellLimitBook.entries.values() shouldBe List.of(
                expectedBookEntry(
                    event = massQuotePlacedEvent,
                    quoteEntry = massQuotePlacedEvent.entries.get(0),
                    side = Side.SELL,
                    sizes = EntrySizes(5),
                    status = EntryStatus.NEW
                ), expectedBookEntry(
                    event = massQuotePlacedEvent,
                    quoteEntry = massQuotePlacedEvent.entries.get(1),
                    side = Side.SELL,
                    sizes = EntrySizes(6),
                    status = EntryStatus.NEW
                )
            )
        }
    }

    feature(cannotMatchOtherMarketMaker) {
        scenario(cannotMatchOtherMarketMaker + "When a Mass Quote from another firm is placed, then all existing quotes and new quotes are on the book even prices have crossed") {
            val massQuotePlacedEvent = MassQuotePlacedEvent(
                bookId = bookId,
                eventId = EventId(2),
                whenHappened = now,
                quoteId = randomId(),
                whoRequested = aFirmWithoutClient(),
                quoteModelType = QuoteModelType.QUOTE_ENTRY,
                timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                entries = List.of(
                    aQuoteEntry(
                        bid = PriceWithSize(size = 5, price = Price(8)),
                        offer = PriceWithSize(size = 5, price = Price(9))
                    ), aQuoteEntry(
                        bid = PriceWithSize(size = 6, price = Price(7)),
                        offer = PriceWithSize(size = 6, price = Price(10))
                    )
                )
            )
            val result = massQuotePlacedEvent.play(books)

            val existingEntries = originalMassQuotePlacedEvent.toBookEntries()

            result.events.size() shouldBe 0
            result.aggregate.buyLimitBook.entries.values() shouldBe List.of(
                existingEntries.get(0),
                existingEntries.get(2),
                expectedBookEntry(
                    event = massQuotePlacedEvent,
                    quoteEntry = massQuotePlacedEvent.entries.get(0),
                    side = Side.BUY,
                    sizes = EntrySizes(5),
                    status = EntryStatus.NEW
                ),
                expectedBookEntry(
                    event = massQuotePlacedEvent,
                    quoteEntry = massQuotePlacedEvent.entries.get(1),
                    side = Side.BUY,
                    sizes = EntrySizes(6),
                    status = EntryStatus.NEW
                )
            )
            result.aggregate.sellLimitBook.entries.values() shouldBe List.of(
                expectedBookEntry(
                    event = massQuotePlacedEvent,
                    quoteEntry = massQuotePlacedEvent.entries.get(0),
                    side = Side.SELL,
                    sizes = EntrySizes(5),
                    status = EntryStatus.NEW
                ),
                existingEntries.get(1),
                expectedBookEntry(
                    event = massQuotePlacedEvent,
                    quoteEntry = massQuotePlacedEvent.entries.get(1),
                    side = Side.SELL,
                    sizes = EntrySizes(6),
                    status = EntryStatus.NEW
                ),
                existingEntries.get(3)
            )
        }
    }
    feature(aggressorPartialFilledByQuotes) {
        scenario(aggressorPartialFilledByQuotes + "When a BUY Limit GTC Order 10 at 11 is placed, then 4 at 10 traded and 5 at 11 traded and a new BUY entry 1 at 11 added and all the SELL entries removed") {
            val orderPlacedEvent = anOrderPlacedEvent(
                bookId = bookId,
                entryType = EntryType.LIMIT,
                side = Side.BUY,
                price = Price(11),
                timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                whenRequested = now,
                eventId = EventId(2),
                sizes = EntrySizes(10)
            )
            val result = orderPlacedEvent.play(books)

            result.aggregate.buyLimitBook.entries.values() shouldBe List.of(
                expectedBookEntry(
                    event = orderPlacedEvent,
                    status = EntryStatus.PARTIAL_FILL,
                    sizes = EntrySizes(available = 1, traded = 9, cancelled = 0)
                ),
                expectedBookEntry(
                    event = originalMassQuotePlacedEvent,
                    quoteEntry = originalMassQuotePlacedEvent.entries.get(0),
                    side = Side.BUY,
                    sizes = EntrySizes(4),
                    status = EntryStatus.NEW
                ),
                expectedBookEntry(
                    event = originalMassQuotePlacedEvent,
                    quoteEntry = originalMassQuotePlacedEvent.entries.get(1),
                    side = Side.BUY,
                    sizes = EntrySizes(5),
                    status = EntryStatus.NEW
                )
            )

            result.aggregate.sellLimitBook.entries.size() shouldBe 0

            result.events shouldBe List.of(
                TradeEvent(
                    eventId = EventId(3),
                    bookId = bookId,
                    size = 4,
                    price = Price(10),
                    whenHappened = now,
                    aggressor = expectedTradeSideEntry(
                        event = orderPlacedEvent,
                        sizes = EntrySizes(available = 6, traded = 4, cancelled = 0),
                        status = EntryStatus.PARTIAL_FILL
                    ),
                    passive = expectedTradeSideEntry(
                        event = originalMassQuotePlacedEvent,
                        quoteEntry = originalMassQuotePlacedEvent.entries.get(0),
                        side = Side.SELL,
                        sizes = EntrySizes(available = 0, traded = 4, cancelled = 0),
                        status = EntryStatus.FILLED
                    )
                ),
                TradeEvent(
                    eventId = EventId(4),
                    bookId = bookId,
                    size = 5,
                    price = Price(11),
                    whenHappened = now,
                    aggressor = expectedTradeSideEntry(
                        event = orderPlacedEvent,
                        sizes = EntrySizes(available = 1, traded = 9, cancelled = 0),
                        status = EntryStatus.PARTIAL_FILL
                    ),
                    passive = expectedTradeSideEntry(
                        event = originalMassQuotePlacedEvent,
                        quoteEntry = originalMassQuotePlacedEvent.entries.get(1),
                        side = Side.SELL,
                        sizes = EntrySizes(available = 0, traded = 5, cancelled = 0),
                        status = EntryStatus.FILLED
                    )
                )
            )
        }
    }
    feature(aggressorFilledByQuotes) {
        scenario(aggressorFilledByQuotes + "When a BUY Limit GTC Order 9 at 11 is placed, then 4 at 10 traded and 5 at 11 traded and the SELL entry 1 at 11 remains") {
            val orderPlacedEvent = anOrderPlacedEvent(
                bookId = bookId,
                entryType = EntryType.LIMIT,
                side = Side.BUY,
                price = Price(11),
                timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                whenRequested = now,
                eventId = EventId(2),
                sizes = EntrySizes(9)
            )
            val result = orderPlacedEvent.play(books)

            result.aggregate.buyLimitBook.entries.values() shouldBe List.of(
                expectedBookEntry(
                    event = originalMassQuotePlacedEvent,
                    quoteEntry = originalMassQuotePlacedEvent.entries.get(0),
                    side = Side.BUY,
                    sizes = EntrySizes(4),
                    status = EntryStatus.NEW
                ),
                expectedBookEntry(
                    event = originalMassQuotePlacedEvent,
                    quoteEntry = originalMassQuotePlacedEvent.entries.get(1),
                    side = Side.BUY,
                    sizes = EntrySizes(5),
                    status = EntryStatus.NEW
                )
            )

            result.aggregate.sellLimitBook.entries.size() shouldBe 0

            result.events shouldBe List.of(
                TradeEvent(
                    eventId = EventId(3),
                    bookId = bookId,
                    size = 4,
                    price = Price(10),
                    whenHappened = now,
                    aggressor = expectedTradeSideEntry(
                        event = orderPlacedEvent,
                        sizes = EntrySizes(available = 5, traded = 4, cancelled = 0),
                        status = EntryStatus.PARTIAL_FILL
                    ),
                    passive = expectedTradeSideEntry(
                        event = originalMassQuotePlacedEvent,
                        quoteEntry = originalMassQuotePlacedEvent.entries.get(0),
                        side = Side.SELL,
                        sizes = EntrySizes(available = 0, traded = 4, cancelled = 0),
                        status = EntryStatus.FILLED
                    )
                ),
                TradeEvent(
                    eventId = EventId(4),
                    bookId = bookId,
                    size = 5,
                    price = Price(11),
                    whenHappened = now,
                    aggressor = expectedTradeSideEntry(
                        event = orderPlacedEvent,
                        sizes = EntrySizes(available = 0, traded = 9, cancelled = 0),
                        status = EntryStatus.FILLED
                    ),
                    passive = expectedTradeSideEntry(
                        event = originalMassQuotePlacedEvent,
                        quoteEntry = originalMassQuotePlacedEvent.entries.get(1),
                        side = Side.SELL,
                        sizes = EntrySizes(available = 0, traded = 5, cancelled = 0),
                        status = EntryStatus.FILLED
                    )
                )
            )
        }
    }
})

