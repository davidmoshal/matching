package jasition.matching.domain.scenario.trading

import io.kotlintest.data.forall
import io.kotlintest.shouldBe
import io.kotlintest.specs.FeatureSpec
import io.kotlintest.tables.row
import io.vavr.collection.List
import jasition.cqrs.EventId
import jasition.matching.domain.*
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.quote.QuoteModelType
import jasition.matching.domain.quote.event.MassQuotePlacedEvent
import jasition.matching.domain.trade.event.TradeEvent
import java.time.Instant

@Deprecated("Old CQRS Semantics")
internal class `Given the book has a BUY Limit GTC Order 4 at 10` : FeatureSpec({
    val higherBuyOverLowerFeature = "[1 - Higher BUY over lower] "
    val earlierOverLaterFeature = "[2 - Earlier over later] "
    val smallerEventIdOverBiggerFeature = "[3 - Smaller Event ID over bigger] "
    val noTradeIfPricesDoNotCrossFeature = "[4 - No trade if prices do not cross] "
    val noWashTradeFeature = "[5 - No wash trade] "
    val aggressorTakesBetterExecutionPriceFeature = "[6 - Aggressor takes better execution price] "
    val aggressorFilledPassivePartialFilledFeature = "[7 - Aggressor filled passive partial-filled] "
    val aggressorFilledPassiveFilledFeature = "[8 - Aggressor filled passive filled] "
    val aggressorPartialFilledPassiveFilledFeature = "[9 - Aggressor partial-filled passive filled] "

    val now = Instant.now()
    val existingEntry = aBookEntry(
        requestId = anotherClientRequestId(),
        whoRequested = anotherFirmWithClient(),
        price = Price(10),
        whenSubmitted = now,
        eventId = EventId(1),
        entryType = EntryType.LIMIT,
        side = Side.BUY,
        timeInForce = TimeInForce.GOOD_TILL_CANCEL,
        sizes = EntrySizes(4),
        status = EntryStatus.NEW
    )
    val bookId = aBookId()
    val books = aBooks(bookId, List.of(existingEntry))

    feature(higherBuyOverLowerFeature) {
        scenario(higherBuyOverLowerFeature + "When a BUY Limit GTC Order 5 at 11 is placed, then the new entry is added above the existing") {
            val orderPlacedEvent = anOrderPlacedEvent(
                bookId = bookId,
                entryType = EntryType.LIMIT,
                side = Side.BUY,
                price = Price(11),
                timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                whenRequested = now,
                eventId = EventId(2),
                sizes = EntrySizes(5)
            )
            val result = orderPlacedEvent.play(books)

            val expectedBookEntry = expectedBookEntry(orderPlacedEvent)
            result.events.size() shouldBe 0
            result.aggregate.buyLimitBook.entries.values() shouldBe List.of(expectedBookEntry, existingEntry)
            result.aggregate.sellLimitBook.entries.size() shouldBe 0
        }
        scenario(higherBuyOverLowerFeature + "When a BUY Limit GTC Order 5 at 9 is placed, then the new entry is added below the existing") {
            val orderPlacedEvent = anOrderPlacedEvent(
                bookId = bookId,
                entryType = EntryType.LIMIT,
                side = Side.BUY,
                price = Price(9),
                timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                whenRequested = now,
                eventId = EventId(2),
                sizes = EntrySizes(5)
            )
            val result = orderPlacedEvent.play(books)

            result.events.size() shouldBe 0
            result.aggregate.buyLimitBook.entries.values() shouldBe List.of(
                existingEntry,
                expectedBookEntry(orderPlacedEvent)
            )
            result.aggregate.sellLimitBook.entries.size() shouldBe 0
        }
    }

    feature(earlierOverLaterFeature) {
        scenario(earlierOverLaterFeature + "When a BUY Limit GTC Order 5 at 10 is placed at a later time, then the new entry is added below the existing") {
            val orderPlacedEvent = anOrderPlacedEvent(
                bookId = bookId,
                entryType = EntryType.LIMIT,
                side = Side.BUY,
                price = Price(10),
                timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                whenRequested = now.plusMillis(1),
                eventId = EventId(2),
                sizes = EntrySizes(5)
            )
            val result = orderPlacedEvent.play(books)

            result.events.size() shouldBe 0
            result.aggregate.buyLimitBook.entries.values() shouldBe List.of(
                existingEntry,
                expectedBookEntry(orderPlacedEvent)
            )
            result.aggregate.sellLimitBook.entries.size() shouldBe 0
        }
    }

    feature(smallerEventIdOverBiggerFeature) {
        scenario(smallerEventIdOverBiggerFeature + "When a BUY Limit GTC Order 5 at 10 is placed at the same instant, then the new entry is added below the existing") {
            val orderPlacedEvent = anOrderPlacedEvent(
                bookId = bookId,
                entryType = EntryType.LIMIT,
                side = Side.BUY,
                price = Price(10),
                timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                whenRequested = existingEntry.key.whenSubmitted,
                eventId = EventId(2),
                sizes = EntrySizes(5)
            )
            val result = orderPlacedEvent.play(books)

            result.events.size() shouldBe 0
            result.aggregate.buyLimitBook.entries.values() shouldBe List.of(
                existingEntry,
                expectedBookEntry(orderPlacedEvent)
            )
            result.aggregate.sellLimitBook.entries.size() shouldBe 0
        }
    }

    feature(noTradeIfPricesDoNotCrossFeature) {
        scenario(noTradeIfPricesDoNotCrossFeature + "When a SELL Limit GTC Order 2 at 11 is placed, then the SELL entry is added") {
            val orderPlacedEvent = anOrderPlacedEvent(
                bookId = bookId,
                entryType = EntryType.LIMIT,
                side = Side.SELL,
                price = Price(11),
                timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                whenRequested = now,
                eventId = EventId(2),
                sizes = EntrySizes(2)
            )
            val result = orderPlacedEvent.play(books)

            result.events.size() shouldBe 0
            result.aggregate.buyLimitBook.entries.values() shouldBe List.of(existingEntry)
            result.aggregate.sellLimitBook.entries.values() shouldBe List.of(expectedBookEntry(orderPlacedEvent))
        }
    }

    feature(noWashTradeFeature) {
        forall(
            row(anotherFirmWithClient(), "the same firm and same firm client"),
            row(anotherFirmWithoutClient(), "the same firm, one with but another without firm client")
        ) { client, details ->

            scenario(noWashTradeFeature + "When a SELL Limit GTC Order 4 at 10 is placed by $details, then the SELL entry is added") {
                val orderPlacedEvent = anOrderPlacedEvent(
                    requestId = anotherClientRequestId(),
                    whoRequested = client,
                    bookId = bookId,
                    entryType = EntryType.LIMIT,
                    side = Side.SELL,
                    price = Price(10),
                    timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                    whenRequested = now,
                    eventId = EventId(2),
                    sizes = EntrySizes(4)
                )
                val result = orderPlacedEvent.play(books)

                result.events.size() shouldBe 0
                result.aggregate.buyLimitBook.entries.values() shouldBe List.of(existingEntry)
                result.aggregate.sellLimitBook.entries.values() shouldBe List.of(expectedBookEntry(orderPlacedEvent))
            }
        }
        scenario(noWashTradeFeature + "When a SELL Limit GTC Order 4 at 10 is placed by the same firm, both without firm client, then the SELL entry is added") {
            val orderPlacedEvent = anOrderPlacedEvent(
                requestId = anotherClientRequestId(),
                whoRequested = anotherFirmWithoutClient(),
                bookId = bookId,
                entryType = EntryType.LIMIT,
                side = Side.SELL,
                price = Price(10),
                timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                whenRequested = now,
                eventId = EventId(2),
                sizes = EntrySizes(4)
            )
            val existingEntryWithoutFirmClient = existingEntry.copy(whoRequested = anotherFirmWithoutClient())
            val result = orderPlacedEvent.play(
                aBooks(bookId, List.of(existingEntryWithoutFirmClient))
            )

            val expectedBookEntry = expectedBookEntry(orderPlacedEvent)
            result.events.size() shouldBe 0
            result.aggregate.buyLimitBook.entries.values() shouldBe List.of(existingEntryWithoutFirmClient)
            result.aggregate.sellLimitBook.entries.values() shouldBe List.of(expectedBookEntry)
        }
    }

    feature(aggressorTakesBetterExecutionPriceFeature) {
        scenario(aggressorTakesBetterExecutionPriceFeature + "When a SELL Limit GTC Order 4 at 9 is placed, then 4 at 10 traded and the BUY entry removed") {
            val orderPlacedEvent = anOrderPlacedEvent(
                bookId = bookId,
                entryType = EntryType.LIMIT,
                side = Side.SELL,
                price = Price(9),
                timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                whenRequested = now,
                eventId = EventId(2),
                sizes = EntrySizes(4)
            )
            val result = orderPlacedEvent.play(books)

            result.events shouldBe List.of(
                TradeEvent(
                    eventId = EventId(3),
                    bookId = bookId,
                    size = 4,
                    price = Price(10),
                    whenHappened = now,
                    aggressor = expectedTradeSideEntry(
                        event = orderPlacedEvent,
                        sizes = EntrySizes(available = 0, traded = 4, cancelled = 0),
                        status = EntryStatus.FILLED
                    ),
                    passive = expectedTradeSideEntry(
                        bookEntry = existingEntry,
                        sizes = EntrySizes(available = 0, traded = 4, cancelled = 0),
                        status = EntryStatus.FILLED
                    )
                )
            )
            result.aggregate.buyLimitBook.entries.size() shouldBe 0
            result.aggregate.sellLimitBook.entries.size() shouldBe 0
        }
    }

    feature(aggressorFilledPassivePartialFilledFeature) {
        forall(
            row(TimeInForce.GOOD_TILL_CANCEL),
            row(TimeInForce.IMMEDIATE_OR_CANCEL)
        ) { timeInForce ->
            scenario(aggressorFilledPassivePartialFilledFeature + "When a SELL Limit ${timeInForce.code} Order 3 at 10 is placed, then 3 at 10 traded and the BUY entry remains 1 at 10") {
                val orderPlacedEvent = anOrderPlacedEvent(
                    bookId = bookId,
                    entryType = EntryType.LIMIT,
                    side = Side.SELL,
                    price = Price(10),
                    timeInForce = timeInForce,
                    whenRequested = now,
                    eventId = EventId(2),
                    sizes = EntrySizes(3)
                )
                val result = orderPlacedEvent.play(books)

                result.events shouldBe List.of(
                    TradeEvent(
                        eventId = EventId(3),
                        bookId = bookId,
                        size = 3,
                        price = Price(10),
                        whenHappened = now,
                        aggressor = expectedTradeSideEntry(
                            event = orderPlacedEvent,
                            sizes = EntrySizes(available = 0, traded = 3, cancelled = 0),
                            status = EntryStatus.FILLED
                        ),
                        passive = expectedTradeSideEntry(
                            bookEntry = existingEntry,
                            sizes = EntrySizes(available = 1, traded = 3, cancelled = 0),
                            status = EntryStatus.PARTIAL_FILL
                        )
                    )
                )
                result.aggregate.buyLimitBook.entries.values() shouldBe List.of(
                    existingEntry.copy(
                        status = EntryStatus.PARTIAL_FILL,
                        sizes = EntrySizes(available = 1, traded = 3, cancelled = 0)
                    )
                )
                result.aggregate.sellLimitBook.entries.size() shouldBe 0
            }
        }
        scenario(aggressorFilledPassivePartialFilledFeature + "When a Mass Quote of ((BUY 4 at 9 SELL 3 at 10), (BUY 5 at 8 SELL 5 at 11)) is placed, then 3 at 10 traded and the BUY entry remains 1 at 10 and the rest of quote entries are added") {
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
                        bid = PriceWithSize(size = 4, price = Price(9)),
                        offer = PriceWithSize(size = 3, price = Price(10))
                    ), aQuoteEntry(
                        bid = PriceWithSize(size = 5, price = Price(8)),
                        offer = PriceWithSize(size = 5, price = Price(11))
                    )
                )
            )
            val result = massQuotePlacedEvent.play(books)

            result.events shouldBe List.of(
                TradeEvent(
                    eventId = EventId(3),
                    bookId = bookId,
                    size = 3,
                    price = Price(10),
                    whenHappened = now,
                    aggressor = expectedTradeSideEntry(
                        event = massQuotePlacedEvent,
                        quoteEntry = massQuotePlacedEvent.entries.get(0),
                        side = Side.SELL,
                        sizes = EntrySizes(available = 0, traded = 3, cancelled = 0),
                        status = EntryStatus.FILLED
                    ),
                    passive = expectedTradeSideEntry(
                        bookEntry = existingEntry,
                        sizes = EntrySizes(available = 1, traded = 3, cancelled = 0),
                        status = EntryStatus.PARTIAL_FILL
                    )
                )
            )
            result.aggregate.buyLimitBook.entries.values() shouldBe List.of(
                existingEntry.copy(
                    status = EntryStatus.PARTIAL_FILL,
                    sizes = EntrySizes(available = 1, traded = 3, cancelled = 0)
                ), expectedBookEntry(
                    event = massQuotePlacedEvent,
                    quoteEntry = massQuotePlacedEvent.entries.get(0),
                    side = Side.BUY,
                    sizes = EntrySizes(4),
                    status = EntryStatus.NEW
                ), expectedBookEntry(
                    event = massQuotePlacedEvent,
                    quoteEntry = massQuotePlacedEvent.entries.get(1),
                    side = Side.BUY,
                    sizes = EntrySizes(5),
                    status = EntryStatus.NEW
                )
            )
            result.aggregate.sellLimitBook.entries.values() shouldBe List.of(
                expectedBookEntry(
                    event = massQuotePlacedEvent,
                    quoteEntry = massQuotePlacedEvent.entries.get(1),
                    side = Side.SELL,
                    sizes = EntrySizes(5),
                    status = EntryStatus.NEW
                )
            )
        }
    }

    feature(aggressorFilledPassiveFilledFeature) {
        forall(
            row(TimeInForce.GOOD_TILL_CANCEL),
            row(TimeInForce.IMMEDIATE_OR_CANCEL)
        ) { timeInForce ->

            scenario(aggressorFilledPassiveFilledFeature + "When a SELL Limit ${timeInForce.code} Order 4 at 10 is placed, then 4 at 10 traded and the BUY entry removed") {
                val orderPlacedEvent = anOrderPlacedEvent(
                    bookId = bookId,
                    entryType = EntryType.LIMIT,
                    side = Side.SELL,
                    price = Price(10),
                    timeInForce = timeInForce,
                    whenRequested = now,
                    eventId = EventId(2),
                    sizes = EntrySizes(4)
                )
                val result = orderPlacedEvent.play(books)

                result.events shouldBe List.of(
                    TradeEvent(
                        eventId = EventId(3),
                        bookId = bookId,
                        size = 4,
                        price = Price(10),
                        whenHappened = now,
                        aggressor = expectedTradeSideEntry(
                            event = orderPlacedEvent,
                            sizes = EntrySizes(available = 0, traded = 4, cancelled = 0),
                            status = EntryStatus.FILLED
                        ),
                        passive = expectedTradeSideEntry(
                            bookEntry = existingEntry,
                            sizes = EntrySizes(available = 0, traded = 4, cancelled = 0),
                            status = EntryStatus.FILLED
                        )
                    )
                )
                result.aggregate.buyLimitBook.entries.size() shouldBe 0
                result.aggregate.sellLimitBook.entries.size() shouldBe 0
            }
        }
        scenario(aggressorFilledPassiveFilledFeature + "When a Mass Quote of ((BUY 4 at 9 SELL 4 at 10), (BUY 5 at 8 SELL 5 at 11)) is placed, then 4 at 10 traded and the BUY entry removed and the rest of quote entries are added") {
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
                        bid = PriceWithSize(size = 4, price = Price(9)),
                        offer = PriceWithSize(size = 4, price = Price(10))
                    ), aQuoteEntry(
                        bid = PriceWithSize(size = 5, price = Price(8)),
                        offer = PriceWithSize(size = 5, price = Price(11))
                    )
                )
            )
            val result = massQuotePlacedEvent.play(books)

            result.events shouldBe List.of(
                TradeEvent(
                    eventId = EventId(3),
                    bookId = bookId,
                    size = 4,
                    price = Price(10),
                    whenHappened = now,
                    aggressor = expectedTradeSideEntry(
                        event = massQuotePlacedEvent,
                        quoteEntry = massQuotePlacedEvent.entries.get(0),
                        side = Side.SELL,
                        sizes = EntrySizes(available = 0, traded = 4, cancelled = 0),
                        status = EntryStatus.FILLED
                    ),
                    passive = expectedTradeSideEntry(
                        bookEntry = existingEntry,
                        sizes = EntrySizes(available = 0, traded = 4, cancelled = 0),
                        status = EntryStatus.FILLED
                    )
                )
            )
            result.aggregate.buyLimitBook.entries.values() shouldBe List.of(
                expectedBookEntry(
                    event = massQuotePlacedEvent,
                    quoteEntry = massQuotePlacedEvent.entries.get(0),
                    side = Side.BUY,
                    sizes = EntrySizes(4),
                    status = EntryStatus.NEW
                ), expectedBookEntry(
                    event = massQuotePlacedEvent,
                    quoteEntry = massQuotePlacedEvent.entries.get(1),
                    side = Side.BUY,
                    sizes = EntrySizes(5),
                    status = EntryStatus.NEW
                )
            )
            result.aggregate.sellLimitBook.entries.values() shouldBe List.of(
                expectedBookEntry(
                    event = massQuotePlacedEvent,
                    quoteEntry = massQuotePlacedEvent.entries.get(1),
                    side = Side.SELL,
                    sizes = EntrySizes(5),
                    status = EntryStatus.NEW
                )
            )
        }
    }

    feature(aggressorPartialFilledPassiveFilledFeature) {
        scenario(aggressorPartialFilledPassiveFilledFeature + "When a SELL Limit GTC Order 5 at 10 is placed, then 4 at 10 traded and the BUY entry removed and a SELL entry 1 at 10 added") {
            val orderPlacedEvent = anOrderPlacedEvent(
                bookId = bookId,
                entryType = EntryType.LIMIT,
                side = Side.SELL,
                price = Price(10),
                timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                whenRequested = now,
                eventId = EventId(2),
                sizes = EntrySizes(5)
            )
            val result = orderPlacedEvent.play(books)

            result.events shouldBe List.of(
                TradeEvent(
                    eventId = EventId(3),
                    bookId = bookId,
                    size = 4,
                    price = Price(10),
                    whenHappened = now,
                    aggressor = expectedTradeSideEntry(
                        event = orderPlacedEvent,
                        sizes = EntrySizes(available = 1, traded = 4, cancelled = 0),
                        status = EntryStatus.PARTIAL_FILL
                    ),
                    passive = expectedTradeSideEntry(
                        bookEntry = existingEntry,
                        sizes = EntrySizes(available = 0, traded = 4, cancelled = 0),
                        status = EntryStatus.FILLED
                    )
                )
            )
            result.aggregate.buyLimitBook.entries.size() shouldBe 0
            result.aggregate.sellLimitBook.entries.values() shouldBe List.of(
                expectedBookEntry(
                    event = orderPlacedEvent,
                    status = EntryStatus.PARTIAL_FILL,
                    sizes = EntrySizes(available = 1, traded = 4, cancelled = 0)
                )
            )
        }
        scenario(aggressorPartialFilledPassiveFilledFeature + "When a SELL Limit IOC Order 5 at 10 is placed, then 4 at 10 traded and the BUY entry removed and the remaining 1 at 10 cancelled") {
            val orderPlacedEvent = anOrderPlacedEvent(
                bookId = bookId,
                entryType = EntryType.LIMIT,
                side = Side.SELL,
                price = Price(10),
                timeInForce = TimeInForce.IMMEDIATE_OR_CANCEL,
                whenRequested = now,
                eventId = EventId(2),
                sizes = EntrySizes(5)
            )
            val result = orderPlacedEvent.play(books)

            result.events shouldBe List.of(
                TradeEvent(
                    eventId = EventId(3),
                    bookId = bookId,
                    size = 4,
                    price = Price(10),
                    whenHappened = now,
                    aggressor = expectedTradeSideEntry(
                        event = orderPlacedEvent,
                        sizes = EntrySizes(available = 1, traded = 4, cancelled = 0),
                        status = EntryStatus.PARTIAL_FILL
                    ),
                    passive = expectedTradeSideEntry(
                        bookEntry = existingEntry,
                        sizes = EntrySizes(available = 0, traded = 4, cancelled = 0),
                        status = EntryStatus.FILLED
                    )
                ), expectedOrderCancelledByExchangeEvent(
                    event = orderPlacedEvent,
                    eventId = EventId(4),
                    tradedSize = 4,
                    cancelledSize = 1
                )

            )
            result.aggregate.buyLimitBook.entries.size() shouldBe 0
            result.aggregate.sellLimitBook.entries.size() shouldBe 0
        }
        scenario(aggressorPartialFilledPassiveFilledFeature + "When a Mass Quote of ((BUY 5 at 9 SELL 5 at 10), (BUY 5 at 8 SELL 5 at 11)) is placed, then 4 at 10 traded and the BUY entry removed and the rest of quote entries are added") {
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
                        bid = PriceWithSize(size = 5, price = Price(9)),
                        offer = PriceWithSize(size = 5, price = Price(10))
                    ), aQuoteEntry(
                        bid = PriceWithSize(size = 5, price = Price(8)),
                        offer = PriceWithSize(size = 5, price = Price(11))
                    )
                )
            )
            val result = massQuotePlacedEvent.play(books)

            result.events shouldBe List.of(
                TradeEvent(
                    eventId = EventId(3),
                    bookId = bookId,
                    size = 4,
                    price = Price(10),
                    whenHappened = now,
                    aggressor = expectedTradeSideEntry(
                        event = massQuotePlacedEvent,
                        quoteEntry = massQuotePlacedEvent.entries.get(0),
                        side = Side.SELL,
                        sizes = EntrySizes(available = 1, traded = 4, cancelled = 0),
                        status = EntryStatus.PARTIAL_FILL
                    ),
                    passive = expectedTradeSideEntry(
                        bookEntry = existingEntry,
                        sizes = EntrySizes(available = 0, traded = 4, cancelled = 0),
                        status = EntryStatus.FILLED
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
                    sizes = EntrySizes(5),
                    status = EntryStatus.NEW
                )
            )
            result.aggregate.sellLimitBook.entries.values() shouldBe List.of(
                expectedBookEntry(
                    event = massQuotePlacedEvent,
                    quoteEntry = massQuotePlacedEvent.entries.get(0),
                    side = Side.SELL,
                    sizes = EntrySizes(available = 1, traded = 4, cancelled = 0),
                    status = EntryStatus.PARTIAL_FILL
                ), expectedBookEntry(
                    event = massQuotePlacedEvent,
                    quoteEntry = massQuotePlacedEvent.entries.get(1),
                    side = Side.SELL,
                    sizes = EntrySizes(5),
                    status = EntryStatus.NEW
                )
            )
        }
    }

})

